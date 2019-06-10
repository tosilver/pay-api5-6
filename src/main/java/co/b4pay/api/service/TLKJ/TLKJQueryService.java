package co.b4pay.api.service.TLKJ;

import co.b4pay.api.common.TLKJ.HttpConnectionUtil;
import co.b4pay.api.common.TLKJ.QpayConstants;
import co.b4pay.api.common.TLKJ.QpayUtil;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.model.*;
import co.b4pay.api.service.BasePayService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * 通联快捷签约申请service
 *
 * @author zgp
 * @version
 */
@Service
//@Transactional
public class TLKJQueryService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(TLKJQueryService.class);

    private HmacSHA1Signature signature = new HmacSHA1Signature();



    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {

        logger.info("TLKJPayService-->executeReturn:" + params);
        Map m = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            m.put(parameterName, request.getParameter(parameterName));
        }
        m.remove("signature");
        try {
            String content = SignatureUtil.getSignatureContent(m, true);
            String sign = signature.sign(content, merchantDao.getOne(merchantId).getSecretKey(), Constants.CHARSET_UTF8);
            m.put("signature", sign);
            String result = HttpsUtils.post( "http://127.0.0.1:9988/pay/TLKJQueryExecute.do", null, m);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);
        logger.info("TLKJPayService-->execute:" + params);
        //按要求组装参数
        //商户订单号
        String orderid = params.getString("orderid");
        //协议编号
        String trxid = params.getString("trxid");
        //封装请求参数
        Map<String, String> map = buildBasicMap();
        Trade trade=null;
        if (orderid != null){
            map.put("orderid",orderid);
            trade = tradeDao.findByMerchantOrderNo(orderid);
        }
        if (trxid != null){
            map.put("trxid",trxid);
            trade = tradeDao.findByPayOrderNo(trxid);
        }
        Map<String, String> dorequest = QpayUtil.dorequest(QpayConstants.SYB_APIURL_QPAY + "/query", map, QpayConstants.SYB_APPKEY);
        logger.info("返回的参数如下:");
        print(dorequest);
        String dorequestJson = JSON.toJSONString(dorequest);
        JSONObject resultJson = JSONObject.parseObject(dorequestJson);
        String retcode = resultJson.getString("retcode");
        String orderid1 = resultJson.getString("orderid");
        String trxcode = resultJson.getString("trxcode");
        String trxamt = resultJson.getString("trxamt");
        String trxstatus = resultJson.getString("trxstatus");
        String acct = resultJson.getString("acct");
        String fintime = resultJson.getString("fintime");
        String errmsg = resultJson.getString("errmsg");
        if ("SUCCESS".equals(retcode)){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("retmsg", "接口调用成功");
            jsonObject.put("retcode", retcode);
            jsonObject.put("orderid", orderid1);
            jsonObject.put("trxamt", trxamt);
            jsonObject.put("trxstatus", trxstatus);
            jsonObject.put("acct", acct);
            jsonObject.put("fintime", fintime);
            jsonObject.put("errmsg", errmsg);
            if ("0000".equals(trxstatus) && trade != null && trade.getTradeState()==0){
                trade.setTradeState(1);
                tradeDao.save(trade);
            }
            return jsonObject;
        } else if ("FAIL".equals(retcode)){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("retmsg", "接口调用失败");
            jsonObject.put("retcode", retcode);
            jsonObject.put("errmsg", errmsg);
            return jsonObject;
        }else {
            throw new BizException("服务器异常!!!");
        }
    }

    /**
     * 公共请求参数
     * @return
     */
    public static Map<String, String> buildBasicMap(){
        TreeMap<String,String> params = new TreeMap<String,String>();
        params.put("appid", QpayConstants.SYB_APPID);
        params.put("cusid", QpayConstants.SYB_CUSID);
        params.put("version", QpayConstants.version);
        params.put("randomstr", System.currentTimeMillis()+"");
        return params;
    }


    /**
     * 对返回的数据进行轮询
     * @param map
     */
    public static void print(Map<String, String> map){
        if(map!=null){
            for(String key:map.keySet()){
                logger.info(key+":"+map.get(key));
            }
        }
    }
}







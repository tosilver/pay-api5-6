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
public class PayAgreeConfirmService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(PayAgreeConfirmService.class);
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
            String result = HttpsUtils.post( "http://127.0.0.1:9988/pay/payagreeconfirmExecute.do", null, m);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        logger.info("TLKJPayService-->execute:" + params);
        //商户订单号
        String orderid = params.getString("orderid");
        Trade trade = tradeDao.findByMerchantOrderNo(orderid);
        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);
        //按要求组装参数
        //协议编号
        String agreeid = params.getString("agreeid");
        //
        String smscode = params.getString("smscode");
        //
        String thpinfo = params.getString("thpinfo");

        //封装请求参数
        Map<String, String> map = buildBasicMap();
        map.put("orderid",trade.getId());
        map.put("agreeid",agreeid);
        map.put("smscode",smscode);
        if ( thpinfo != null){
            map.put("thpinfo",thpinfo);
        }

        Map<String, String> dorequest = QpayUtil.dorequest(QpayConstants.SYB_APIURL_QPAY + "/payagreeconfirm", map, QpayConstants.SYB_APPKEY);
        logger.info("返回的参数如下:");
        print(dorequest);
        String dorequestJson = JSON.toJSONString(dorequest);
        JSONObject resultJson = JSONObject.parseObject(dorequestJson);
        String retcode = resultJson.getString("retcode");
        String errmsg = resultJson.getString("errmsg");
        String trxstatus = resultJson.getString("trxstatus");
        String thpinfo1 = resultJson.getString("thpinfo");
        //交易单号
        String trxid = resultJson.getString("trxid");
        //渠道平台交易单号
        String chnltrxid = resultJson.getString("chnltrxid");
        if (thpinfo1 == null){
            thpinfo1="";
        }
        if ("SUCCESS".equals(retcode)){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("msg", "接口调用成功");
            jsonObject.put("retcode", retcode);
            jsonObject.put("errmsg", errmsg);
            jsonObject.put("trxstatus", trxstatus);
            jsonObject.put("orderid",orderid);
            if ("0000".equals(trxstatus)){
                trade.setTradeState(0);
                trade.setPayOrderNo(trxid);
                tradeDao.save(trade);
                jsonObject.put("trxid",trxid);
            }else if ("1999".equals(trxstatus)){
                jsonObject.put("thpinfo",thpinfo1);
            }
            return jsonObject;
        } else if ("FAIL".equals(retcode)){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("msg", "接口调用失败");
            jsonObject.put("retcode", retcode);
            return jsonObject;
        }else {
            throw new BizException("服务器异常!!!");
        }
    }

    /***
     * 生成uid 8位数字
     */
    public static String generateUID(){
        Random random = new Random();
        String result="";
        for(int i=0;i<8;i++){
            //首字母不能为0
            result += (random.nextInt(9)+1);
        }
        return result;
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







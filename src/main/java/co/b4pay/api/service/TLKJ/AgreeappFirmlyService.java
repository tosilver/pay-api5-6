package co.b4pay.api.service.TLKJ;

import co.b4pay.api.common.TLKJ.HttpConnectionUtil;
import co.b4pay.api.common.TLKJ.QpayConstants;
import co.b4pay.api.common.TLKJ.QpayUtil;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.model.KJAgreeapply;
import co.b4pay.api.model.Router;
import co.b4pay.api.service.BasePayService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static co.b4pay.api.common.utils.DateUtil.now;

/**
 * 通联快捷签约申请service
 *
 * @author zgp
 * @version
 */
@Service
//@Transactional
public class AgreeappFirmlyService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(AgreeappFirmlyService.class);

    //private static final String TLkJPAY_API_DOMAIN = MainConfig.getConfig("TLkJPAY_API_DOMAIN");

    private HmacSHA1Signature signature = new HmacSHA1Signature();



    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {

        logger.info("签约申请确认-->executeReturn:" + params);
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
            String result = HttpsUtils.post( "http://127.0.0.1:9988/pay/agreeapplyExecute.do", null, m);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        logger.info("签约申请确认-->execute:" + params);

        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);
        long time = System.currentTimeMillis();
        //按要求组装参数
        //商户用户号:要求唯一,身份的识别
        String merUserId = params.getString("meruserid");
        //卡类型
        //00:借记卡
        //02:准贷记卡/贷记卡
        String acctType = params.getString("accttype");
        //银行卡号
        String acctNo = params.getString("acctno");
        //证件号  如末位是X，必须大写
        String idNo = params.getString("idno");
        //户名
        String acctName = params.getString("acctname");
        //手机号码
        String mobile = params.getString("mobile");
        String validDate=null;
        String cvv2=null;
        if ("02".equals(acctType)){
            //有效期 信用卡不能为空
            validDate = params.getString("validdate");
            //cvv2  信用卡不能为空
            cvv2 = params.getString("cvv2");
        }
        //smscode 短信验证码
        String smscode = params.getString("smscode");
        //封装请求参数
        Map<String, String> map = buildBasicMap();
        map.put("meruserid",merUserId);
        map.put("accttype",acctType);
        map.put("acctno",acctNo);
        map.put("idno",idNo);
        map.put("acctname",acctName);
        map.put("mobile",mobile);
        map.put("cvv2", cvv2);
        map.put("validdate", validDate);
        map.put("smscode", smscode);
        //把加密以上参数
        map.put("sign", QpayUtil.sign(map,QpayConstants.SYB_APPKEY));
        HttpConnectionUtil http = new HttpConnectionUtil(QpayConstants.SYB_APIURL_QPAY + "/agreeapply");
        http.init();
        byte[] bys = http.postParams(map, true);
        String result = new String(bys,"UTF-8");
        JSONObject resultJson = JSONObject.parseObject(result);
        String retcode = resultJson.getString("retcode");
        String agreeid = resultJson.getString("agreeid");
        String bankcode = resultJson.getString("bankcode");
        String bankname = resultJson.getString("bankname");
        String errmsg = resultJson.getString("errmsg");
        String trxstatus = resultJson.getString("trxstatus");
        if ("SUCCESS".equals(retcode)){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("msg", "接口调用成功");
            jsonObject.put("retcode", retcode);
            jsonObject.put("agreeid", agreeid);
            jsonObject.put("bankcode", bankcode);
            jsonObject.put("bankname", bankname);
            jsonObject.put("errmsg", errmsg);
            jsonObject.put("trxstatus", trxstatus);
            KJAgreeapply kjAgreeapply = new KJAgreeapply();
            kjAgreeapply.setMeruserId(merUserId);
            kjAgreeapply.setAcctName(acctName);
            kjAgreeapply.setAcctNo(acctNo);
            kjAgreeapply.setAcctType(acctType);
            kjAgreeapply.setCreateTime(now());
            kjAgreeapply.setCvv2(cvv2);
            kjAgreeapply.setResponse(resultJson.toJSONString());
            kjAgreeapply.setValiddate(validDate);
            kjAgreeapplyDao.save(kjAgreeapply);
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
        System.out.println("返回数据如下:");
        if(map!=null){
            for(String key:map.keySet()){
                System.out.println(key+";"+map.get(key));
            }
        }
    }
}







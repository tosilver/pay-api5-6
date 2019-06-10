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

import static co.b4pay.api.common.utils.DateUtil.now;

/**
 * 通联快捷签约申请service
 *
 * @author zgp
 * @version
 */
@Service
//@Transactional
public class PayApplyService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(PayApplyService.class);

    //private static final String TLkJPAY_API_DOMAIN = MainConfig.getConfig("TLkJPAY_API_DOMAIN");

    private HmacSHA1Signature signature = new HmacSHA1Signature();



    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {

        logger.info("TLKJ支付申请-->executeReturn:" + params);
        BigDecimal totalAmount = new BigDecimal(params.getString("amount"));
        BigDecimal totalMOney = totalAmount.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP);
        logger.warn("支付金额为:" + totalMOney);
        Channel channel = getChannel(merchantId, router, totalMOney);// 预校验
        logger.warn("通道:" + channel.getName());
        if (channel.getIp4() == null) {
            channel.setStatus(-1);
            channel.setUpdateTime(now());
            channelDao.save(channel);
            throw new BizException("渠道地址设置异常");
        }
        Map m = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            m.put(parameterName, request.getParameter(parameterName));
        }
        m.put("channelId", channel.getId().toString());
        m.remove("signature");
        try {
            String content = SignatureUtil.getSignatureContent(m, true);
            String sign = signature.sign(content, merchantDao.getOne(merchantId).getSecretKey(), Constants.CHARSET_UTF8);
            m.put("signature", sign);
            String result = HttpsUtils.post( "http://127.0.0.1:9988/pay/payapplyagreeExecute.do", null, m);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        logger.info("TLKJ支付申请-->execute:" + params);
        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);
        long time = System.currentTimeMillis();
        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        BigDecimal totalAmount = new BigDecimal(params.getString("amount"));
        //转换为单位为元的金额
        BigDecimal totalMOney = totalAmount.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP);
        Channel channel = null;
        if (params.containsKey("channelId")) {
            channel = channelDao.getOne(Long.parseLong(params.getString("channelId")));
        }
        if (channel == null || channel.getStatus() < 0) {
            throw new BizException("渠道异常,请稍后再试！");
        }
        if (channel.getUnitPrice().compareTo(totalMOney) < 0) {
            throw new BizException(String.format("单笔交易不能大于%s元", channel.getUnitPrice()));
        }
        if (channel.getMinPrice().compareTo(totalMOney) > 0) {
            throw new BizException(String.format("单笔交易不能低于%s元", channel.getMinPrice()));
        }
        if (StringUtils.isBlank(channel.getGoodsTypeId())) {
            throw new BizException("渠道商品类别为空");
        }
        List<Goods> goodsList = goodsDao.findByTypeId(Integer.valueOf(channel.getGoodsTypeId()));
        if (goodsList.size() == 0) {
            throw new BizException(String.format("渠道商品类别为空,类别序列：%s)", channel.getGoodsTypeId()));
        }
        MerchantRate merchantRate = merchantRateDao.findByMerchantIdAndRouterId(merchantId, router.getId());
        if (merchantRate == null) {
            throw new BizException(String.format("[%s, %s]商户费率设置异常", merchantId, router.getId()));
        }
        if (totalMOney.subtract(merchantRate.getPayCost()).doubleValue() <= 0) {
            throw new BizException(String.format("[%s]支付金额不能少于%s元", router.getId(), merchantRate.getPayCost()));
        }
        //按要求组装参数
        //商户订单号
        String orderid = params.getString("orderid");
        //协议编号
        String agreeid = params.getString("agreeid");
        //证件号  如末位是X，必须大写
        String currency = params.getString("currency");
        //户名
        String subject = params.getString("subject");
        //手机号码
        String notifyurl = params.getString("notifyurl");
        String validtime = params.getString("validtime");
        String trxreserve = params.getString("trxreserve");
        if (validtime==null){
            validtime="0";
        }
        if (trxreserve==null){
            trxreserve="0";
        }
        //封装请求参数
        // B4系统订单号
        String tradeId = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号
        Map<String, String> map = buildBasicMap();
        map.put("orderid",tradeId);
        map.put("agreeid",agreeid);
        map.put("amount",totalAmount.toPlainString());
        map.put("currency",currency);
        map.put("subject",subject);
        map.put("validtime", validtime);
        map.put("trxreserve", trxreserve);
        map.put("notifyurl", String.format("%s/notify/KLKJPayNotify.do", serverUrl));
        Map<String, String> dorequest = QpayUtil.dorequest(QpayConstants.SYB_APIURL_QPAY + "/payapplyagree", map, QpayConstants.SYB_APPKEY);
        logger.info("返回的参数如下:");
        print(dorequest);
        String dorequestJson = JSON.toJSONString(dorequest);
        JSONObject resultJson = JSONObject.parseObject(dorequestJson);
        String retcode = resultJson.getString("retcode");
        String errmsg = resultJson.getString("errmsg");
        String trxstatus = resultJson.getString("trxstatus");
        String thpinfo = resultJson.getString("thpinfo");
        if (thpinfo == null){
            thpinfo="";
        }
        if ("SUCCESS".equals(retcode)){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("msg", "接口调用成功");
            jsonObject.put("retcode", retcode);
            jsonObject.put("errmsg", errmsg);
            jsonObject.put("trxstatus",trxstatus);
            jsonObject.put("thpinfo",thpinfo);
            jsonObject.put("orderid",orderid);
            BigDecimal serviceCharge = totalAmount.multiply(merchantRate.getCostRate(), new MathContext(2, RoundingMode.HALF_UP)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP).add(merchantRate.getPayCost());
            Trade trade = new Trade();
            trade.setId(tradeId);
            trade.setCostRate(merchantRate.getCostRate());
            trade.setPayCost(merchantRate.getPayCost());
            trade.setTotalAmount(totalAmount);
            trade.setMerchantId(merchantId);
            trade.setChannelId(channel.getId());
            trade.setServiceCharge(serviceCharge); // 服务费
            trade.setAccountAmount(totalAmount.subtract(serviceCharge));
            trade.setNotifyUrl(notifyurl);
            trade.setMerchantOrderNo(orderid);
            trade.setRequest(params.toJSONString());
            trade.setResponse(dorequestJson);
            trade.setTime(System.currentTimeMillis() - time);
            trade.setFzStatus(0);
            trade.setTradeState(0);
            trade.setStatus(1);
            trade.setPayOrderNo("");
            tradeDao.save(trade);
            //logger.warn("KJ trade ->" + JSONObject.toJSONString(trade));
            JobTrade jobTrade = new JobTrade();
            jobTrade.setId(trade.getId());
            jobTrade.setStatus(0);
            jobTrade.setCount(0);
            jobTrade.setChannelType(ChannelType.SHPAY);
            jobTrade.setNotifyUrl(notifyurl);
            jobTradeDao.save(jobTrade);
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







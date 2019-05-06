package co.b4pay.api.service;

import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.model.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mr.constant.ValueConstant;
import com.mr.model.OrgBaseMsgReq;
import com.mr.model.OrgBaseMsgRsp;
import com.mr.model.OrgScanCodeModeReq;
import com.mr.model.OrgScanCodeModeRsp;
import com.mr.util.HttpClientUtil;
import com.mr.util.MerchantUtil;
import com.mr.util.TimeUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static co.b4pay.api.common.utils.DateUtil.now;

@Service
@Transactional
public class SHPayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(SHPayService.class);

    //节点号
    private static String nodeId = "10060151";

    //商户号
    private static String orgId = "1006200000000169";

    //加密密钥
    private static String aesKey = "UxD1yy83cpReVisisnoZcQ==";

    //签名密钥
    private static String orgPrivateKey = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBANG5+4fv0P+waLPJJkegp12KGNLHDGcWoZz73NfqmPul7cSIBeJLG4ZkB4O2l6D2uRlQURJKHRoxyh36p+m8zD2jlofI5Pgp0Nxhewt97W/JDY6oa2tFGj518+2DIPrWqdFwbR0yVOjCb5fVq2E46eRb2YFksk4jOrqSFraa178DAgMBAAECgYBw9GsdZrM40ulBU2fzkfoyLet6skvixdSbOSdTfv2QI2jwvZX1sCAN/Jfzf4cg4WEF2jUf1Zzg+8nw5YxEHjQdUNXun+usl2OOg/+wVffqpEwzc6O5v7qIC0jvMp6WVdIObM06Syuu4ea1ZTjmw52MdGuekz5AZONIh30jt/b0gQJBAPCRtHlwbm4P241GlTh27CSGumyadOxo5+muIprYSstd/lU1+G2lzxFm6/v8qRRx0AfG+h02eRBGtWULa2j2ekECQQDfLdJj7XO2sMAjQEY3mAgvWj+NujjdhHPX1PNyN9cCmOmIZpNrpGqr4VDWD6yOW1fyKOrs9qj9qVfNA1JLZcBDAkEAulo0Mmo8sPeJvHtztkSxEm5nVR+k+UkedS9WURrBfZ33GWzwX4e0yqcuoImNNHAhlRS2xRBgYZJUi8x3zNw5wQJAGm+gE0wmP8ayC7rqVl2A4rMLAivD3qF442ELMUViB6G7T/fukHqaVB/NZn3Wz8oMIdgs88LIA7wSdWtmTR4RZwJBAKsHqkQZdAQNMsqz6pGd8b15AvcannclG3kY8Rn/sTpZ+mN/+A49NIQN1nRKk+stkX6sBC8Q6ZRGqwU0XYM3Hss=";

    //验签密钥
    private static String tcosPublicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCfRRqiTyiDRvgPwAnHm+odB6kEY1O51Zh5rlr3iSYEgDKfO00yD6ZCAh6MlKfYT0DD+WKN91lt6t9g/u0Cw2WJwGeUiOEWUDso/MiOGmdGYrfsarEzGCTSRmu1tIdwFKNi9HThcMTs7aU99lBtoGIYu2mxsXoWnLbdExZ9TaOBgwIDAQAB";

    //请求地址
    private static String submitUrl = "https://120.78.222.201/tnPay";

    //private static final String ALIPAY_API_DOMAIN = MainConfig.getConfig("ALIPAY_API_DOMAIN");

    private HmacSHA1Signature signature = new HmacSHA1Signature();

    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        Channel channel = getChannel(merchantId, router, totalAmount); // 预校验
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
            String result = HttpsUtils.post(channel.getIp4() + "/pay/shPayExecute.do", null, m);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);
        long time = System.currentTimeMillis();

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));

        Channel channel = null;
        if (params.containsKey("channelId")) {
            channel = channelDao.getOne(Long.parseLong(params.getString("channelId")));
        }
        if (channel == null || channel.getStatus() < 0) {
            throw new BizException("渠道异常,请稍后再试！");
        }
        if (channel.getUnitPrice().compareTo(totalAmount) < 0) {
            throw new BizException(String.format("单笔交易不能大于%s元", channel.getUnitPrice()));
        }
        if (channel.getMinPrice().compareTo(totalAmount) > 0) {
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
        if (totalAmount.subtract(merchantRate.getPayCost()).doubleValue() <= 0) {
            throw new BizException(String.format("[%s]支付金额不能少于%s元", router.getId(), merchantRate.getPayCost()));
        }

        // 商户订单号，商户网站订单系统中唯一订单号，必填
        String outTradeNo = params.getString("tradeNo");
        // 订单名称，必填
        String subject = params.getString("subject");
        // 商品描述，可空
        String body = params.getString("body");
        // 超时时间 可空
        String timeoutExpress = "30m";
        // 销售产品码 必填
        String productCode = "QUICK_WAP_WAY";
        String merchantNotifyUrl = params.getString("notifyUrl");
        String returnUrl = params.getString("returnUrl");

        //准备请求参数
        OrgBaseMsgReq msgReq = new OrgBaseMsgReq();
        //1、组装业务报文
        OrgScanCodeModeReq req = new OrgScanCodeModeReq();
        //req.setOutTradeNo(TimeUtil.getCurDate("yyyyMMddHHmmss") + System.currentTimeMillis()); //这里仅举例，一般建议通过时间+序号等方式实现订单号的全局唯一
        req.setOutTradeNo(outTradeNo);
        req.setTotalAmount(totalAmount.toString());
        req.setBody("订单描述");
        req.setTranType("JH002");//业务种类
        req.setNotifyUrl(String.format("%s/notify/sHPayNotify.do", serverUrl));//异步通知地址，根据实际情况填写
        req.setCurrency("CNY");
        req.setOrgCreateIp("127.0.0.1"); //建议获取真实终端IP地址

        //2、对业务报文进行签名
        String signData = JSON.toJSONString(req);
        String sign = MerchantUtil.sign(signData, orgPrivateKey, MerchantUtil.SIGNTYPE_RSA, MerchantUtil.CHARSET);

        //3、对业务报文进行加密
        String bizContext = MerchantUtil.encryptDataByAES(signData, aesKey, MerchantUtil.CHARSET);

        //4、组装公共报文（含业务报文）
        msgReq.setVersion(ValueConstant.VERSION);
        msgReq.setNodeId(nodeId);
        msgReq.setOrgId(orgId);
        msgReq.setOrderTime(TimeUtil.getCurDate("yyyyMMddHHmmss"));
        msgReq.setTxnType("T20104");//交易种类
        msgReq.setSignType(MerchantUtil.SIGNTYPE_RSA);
        msgReq.setCharset(MerchantUtil.CHARSET);
        msgReq.setBizContext(bizContext);
        msgReq.setSign(sign);

        @SuppressWarnings("unchecked")
        Map<String, String> requestParams = JSON.parseObject(JSON.toJSONString(msgReq), Map.class);
        System.out.println("requestParams:  " + requestParams);

        //5、发起POST请求并获得回应
        String rspStr = HttpClientUtil.doPost(requestParams, submitUrl);

        System.out.println("[统一下单（支付链接A）]应答报文:  " + rspStr);
        //6、对应答报文进行处理
        OrgBaseMsgRsp msgRsp = JSON.parseObject(rspStr, OrgBaseMsgRsp.class);

        //判断公共应答报文里的返回码进行判断（通讯级结果）
        if (msgRsp.getCode().equals(ValueConstant.CODE_SUCCESS)) {

            //7、对应答报文的业务报文进行解密
            String rspBizContext = msgRsp.getBizContext();
            String rspMsg = MerchantUtil.decryptDataByAES(rspBizContext, aesKey, msgReq.getCharset());
            logger.info("[统一下单（支付链接A）]业务报文:{}", rspMsg);
            System.out.println("[统一下单（支付链接A）]业务报文:{}" + rspMsg);

            //8、对应答报文的业务报文进行验签
            boolean isTrue = MerchantUtil.verify(rspMsg, msgRsp.getSign(), tcosPublicKey, MerchantUtil.SIGNTYPE_RSA, msgReq.getCharset());
            if (isTrue) {
                logger.info("[统一下单（支付链接A）]验签成功");
                System.out.println("[统一下单（支付链接A）]验签成功");
                //9、获取支付链接后进行相应处理
                OrgScanCodeModeRsp rsp = JSON.parseObject(rspMsg, OrgScanCodeModeRsp.class);
                //验签成功且二维码地址返回不为空，则算调用支付成功
                if (StringUtils.isNotBlank(rsp.getQrCode())) {
                    logger.info("[统一下单（支付链接A）]支付链接:", rsp.getQrCode());
                    System.out.println("[统一下单（支付链接A）]支付链接:{}" + rsp.getQrCode());
                    JSONObject jsonObject = new JSONObject();

                    jsonObject.put("out_trade_no", "");
                    jsonObject.put("sub_code", "");
                    jsonObject.put("sub_msg", "");
                    jsonObject.put("msg", "接口调用成功");
                    jsonObject.put("code", "10000");

                    BigDecimal serviceCharge = totalAmount.multiply(merchantRate.getCostRate(), new MathContext(2, RoundingMode.HALF_UP)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP).add(merchantRate.getPayCost());

                    // B4系统订单号
                    String tradeId = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号
                    Trade trade = new Trade();
                    trade.setId(tradeId);
                    trade.setCostRate(merchantRate.getCostRate());
                    trade.setPayCost(merchantRate.getPayCost());
                    trade.setTotalAmount(totalAmount);
                    trade.setMerchantId(merchantId);
                    trade.setChannelId(channel.getId());
                    trade.setServiceCharge(serviceCharge); // 服务费
                    trade.setAccountAmount(totalAmount.subtract(serviceCharge));
                    trade.setNotifyUrl(merchantNotifyUrl);
                    trade.setMerchantOrderNo(outTradeNo);
                    trade.setRequest(params.toJSONString());
                    trade.setResponse("");
                    trade.setTime(System.currentTimeMillis() - time);
                    trade.setFzStatus(0);
                    trade.setTradeState(0);
                    trade.setStatus(1);
                    tradeDao.save(trade);
                    logger.warn("SH trade ->" + JSONObject.toJSONString(trade));
                    JobTrade jobTrade = new JobTrade();
                    jobTrade.setId(trade.getId());
                    jobTrade.setStatus(0);
                    jobTrade.setCount(0);
                    jobTrade.setChannelType(ChannelType.SHPAY);
                    jobTrade.setNotifyUrl(merchantNotifyUrl);
                    jobTradeDao.save(jobTrade);

                    jsonObject.put("qr_code", rsp.getQrCode());
                    return jsonObject;
                }
            } else {
                //logger.info("[统一下单（支付链接A）]验签失败！");
                logger.warn("[统一下单（支付链接A）]验签失败" + msgRsp.getMsg());
                logger.warn("渠道调用异常，关闭渠道！！！！");
                channel.setStatus(-1);
                channel.setUpdateTime(now());
                channelDao.save(channel);
            }
        } else {
            //logger.info("[统一下单（支付链接A）]交易失败:{}", msgRsp.getMsg());
            logger.warn("[统一下单（支付链接A）]交易失败:{}：" + msgRsp.getMsg());
            logger.warn("渠道调用异常，关闭渠道！！！！");
            channel.setStatus(-1);
            channel.setUpdateTime(now());
            channelDao.save(channel);
            throw new BizException("服务器异常!!!");
        }
        throw new BizException("服务器异常!!!");
    }
}

package co.b4pay.api.controller.notify;

import co.b4pay.api.common.alipay.AlipayConfig;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.tenpay.util.ServletUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.model.*;
import co.b4pay.api.service.ChannelService;
import co.b4pay.api.service.JobTradeService;
import co.b4pay.api.service.MerchantService;
import co.b4pay.api.service.TradeService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.mr.constant.RetCdConstant;
import com.mr.constant.ValueConstant;
import com.mr.model.OrgBaseMsgReq;
import com.mr.model.OrgBaseMsgRsp;
import com.mr.model.OrgPayNotice;
import com.mr.model.OrgPayQryReq;
import com.mr.model.enums.TxnTypeEnum;
import com.mr.util.HttpClientUtil;
import com.mr.util.MerchantUtil;
import com.mr.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SignatureException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import static co.b4pay.api.common.utils.DateUtil.now;
import static co.b4pay.api.common.utils.DateUtil.yMdhms;

/**
 * 支付宝扫码支付通知
 *
 * @author YK
 * @version $Id v 0.1 2018年06月06日 16:32 Exp $
 */
@RestController
@RequestMapping({"/notify/sHPayNotify.do"})
public class SHPayNotifyController {
    private static final Logger logger = LoggerFactory.getLogger(SHPayNotifyController.class);

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

    @Autowired
    private TradeService tradeService;

    @Autowired
    private JobTradeService jobTradeService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private HmacSHA1Signature signature = new HmacSHA1Signature();

    @Autowired
    private MerchantService merchantService;


    @RequestMapping(method = {RequestMethod.POST})
    public void doPost(HttpServletRequest request, HttpServletResponse response, OrgBaseMsgReq req) throws Exception {
        logger.error("中转支付回调进来了");
        System.out.println("中转支付结果通用通知：" + ServletUtil.getQueryString(request));
        JSONObject jsonObject = new JSONObject();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            jsonObject.put(parameterName, request.getParameter(parameterName));
        }

        if (jsonObject.isEmpty()) {
            return;
        }
        logger.warn("sHPay notify parms ->" + jsonObject.toJSONString());
        //String merchantOrderNo = request.getParameter("outTradeNo");
        Trade trade = tradeService.findByMerchantOrderNo(req.getReserve1());
        String merchantOrderNo = req.getReserve1();
        logger.warn("merchantOrderNo:" + merchantOrderNo);

        if (trade == null) {
            logger.warn(String.format("[%s]订单信息不存在", merchantOrderNo));
        }
        Channel channel = channelService.findById(trade.getChannelId() == null ? 0 : trade.getChannelId());
        if (channel == null) {
            logger.warn(String.format("[%s]渠道信息不存在", trade.getChannelId()));
        }
        String tradeNo = request.getParameter("tradeNo");
        trade.setPayOrderNo(tradeNo);
        String payStatus = null;
        try {
            payStatus = payNoticeProcess(req);
            Router router = channel.getRouter() == null ? null : channel.getRouter();
            if (router != null) {

            }

        } catch (Exception e) {
            logger.error(String.format("请求商户api错误，错误原因：%s", e.getMessage()));
        }
        trade.setStatus(1);
        if (ValueConstant.CODE_SUCCESS.equals(payStatus)) {
            String rspBizContext = req.getBizContext();
            logger.error("136:rspBizContext字符串未转换前:" + rspBizContext);
            //签名解码
            rspBizContext = MerchantUtil.decryptDataByAES(rspBizContext, aesKey, req.getCharset());
            logger.error("139:rspBizContext字符串转换后:" + rspBizContext);
            OrgPayNotice rsp = JSON.parseObject(rspBizContext, OrgPayNotice.class);

            logger.warn(String.format("%s 订单交易状态：%s", merchantOrderNo, rsp.getRetMsg()));
            logger.warn(String.format("%s 返回码：%s", merchantOrderNo, rsp.getRetCode()));
            if (RetCdConstant.RET_CD_0000.equals(rsp.getRetCode())) {
                logger.error("[消息通知]交易初验成功,等待再次查询通用订单结果");
                //通用订单查询
                if (payQry(merchantOrderNo)) {
                    logger.error("[消息通知]订单查询完成");
                    //if (true) {
                    trade.setTradeState(1);
                    trade.setPaymentTime(jsonObject.containsKey("gmt_payment") ? jsonObject.getString("gmt_payment") : yMdhms.format(DateUtil.now()));
                    trade.setUpdateTime(now());
                    try {
                        PrintWriter pw = response.getWriter();
                        pw.println("SUCCESS");
                        pw.flush();
                        pw.close();
                    } catch (Exception e) {
                        logger.error(String.format("支付成功，通知商户失败，失败原因：%s", e.getMessage()));
                    }
                    //交易成功，减去渠道相对应金额
                    if (channel != null && channel.getAmountLimit() == null) {
                        logger.warn(String.format("[%s]渠道剩余额度异常", trade.getChannelId()));
                    }
                    if (trade.getTotalAmount() == null) {
                        logger.warn(String.format("订单号[%s]订单金额异常", trade.getMerchantOrderNo()));
                    }
                    if (channel != null && channel.getAmountLimit() != null) {
                        channel.setAmountLimit(channel.getAmountLimit().subtract(trade.getTotalAmount()));
                        channel.setLastSuccessTime(now());
                        if (channel.getAmountLimit().compareTo(channel.getAmountMin()) < 0) {
                            channel.setStatus(-1);
                        }
                        channel.setUpdateTime(now());
                        channelService.save(channel);
                    }
                    //交易成功，给用户发回调

                    logger.error(String.format("生成回调的订单号:%s", trade.getId()));
                    JobTrade jobTrade = jobTradeService.findById(trade.getId());
                    jobTrade.setExecTime(DateUtil.getTime());
                    jobTrade.setContent(rspBizContext);
                    try {
                        logger.error(String.format("即将发起回调,回调内容为:%s", jobTrade));
                        notify(jobTrade);
                        logger.error("回调完毕！");
                    } catch (Exception e) {
                        logger.error(String.format("首次异步回调通知异常,订单号:[%s]", jobTrade.getId()), e);
                    }
                    jobTradeService.save(jobTrade);
                } else {
                    logger.warn(String.format("%s 订单交易状态：%s", merchantOrderNo, jsonObject.get("retMsg")));
                    logger.warn("[支付失败]订单查询失败,交易失败");
                    try {
                        PrintWriter pw = response.getWriter();
                        pw.println("FAIL");
                        pw.flush();
                        pw.close();
                    } catch (Exception e) {
                        logger.error(String.format("支付失败，通知商户失败，失败原因：%s", e.getMessage()));
                    }
                }
            }
        } else {
            logger.warn(String.format("%s 订单交易状态：%s", merchantOrderNo, jsonObject.get("retMsg")));
            logger.warn("交易失败");
            try {
                PrintWriter pw = response.getWriter();
                pw.println("FAIL");
                pw.flush();
                pw.close();
            } catch (Exception e) {
                logger.error(String.format("支付失败，通知商户失败，失败原因：%s", e.getMessage()));
            }
        }
        tradeService.save(trade);
    }


    /**
     * 消息通知平台以post form表单形式下发，具体接收客户自行处理，此处只是针对收到报文后如何处理
     *
     * @param req
     * @throws Exception
     */
    public String payNoticeProcess(OrgBaseMsgReq req) throws Exception {
        String rspBizContext = req.getBizContext();
        rspBizContext = MerchantUtil.decryptDataByAES(rspBizContext, aesKey, req.getCharset());
        logger.error("[消息通知]业务报文:{}", rspBizContext);
        /*验签*/
        boolean isTrue = MerchantUtil.verify(rspBizContext, req.getSign(), tcosPublicKey, MerchantUtil.SIGNTYPE_RSA, req.getCharset());
        if (isTrue) {
            logger.error("[消息通知]验签成功");
            //处理应答报文
            logger.error("226:rspBizContext字符串未转换前:" + rspBizContext);
            OrgPayNotice rsp = JSON.parseObject(rspBizContext, OrgPayNotice.class);
            if (RetCdConstant.RET_CD_0000.equals(rsp.getRetCode())) {
                logger.error("[消息通知]交易成功");
            }
            return ValueConstant.CODE_SUCCESS;
        } else {
            logger.error("[消息通知]验签失败！");
            return ValueConstant.CODE_FAIL;
        }
    }

    /**
     * 统一订单查询
     *
     * @throws Exception
     */
    public Boolean payQry(String merchantOrderNo) {
        OrgBaseMsgReq msgReq = new OrgBaseMsgReq();
        OrgPayQryReq req = new OrgPayQryReq();
        req.setOutTradeNo(merchantOrderNo); //原交易请求的商户订单号
        //req.setTradeNo("c301d3e661b14f949ce762807d120f66");

        //生成签名数据
        String signData = JSON.toJSONString(req);
        //签名
        String sign = null;
        try {
            sign = MerchantUtil.sign(signData, orgPrivateKey, MerchantUtil.SIGNTYPE_RSA, MerchantUtil.CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("生成签名异常");
        }
        //加密
        String bizContext = null;
        try {
            bizContext = MerchantUtil.encryptDataByAES(signData, aesKey, MerchantUtil.CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("加密传输数据异常");
        }
        msgReq.setVersion(ValueConstant.VERSION);
        msgReq.setNodeId(nodeId);
        msgReq.setOrgId(orgId);
        msgReq.setOrderTime(TimeUtil.getCurDate("yyyyMMddHHmmss"));
        msgReq.setTxnType(TxnTypeEnum.PAY_QRY.getValue());
        msgReq.setSignType(MerchantUtil.SIGNTYPE_RSA);
        msgReq.setCharset(MerchantUtil.CHARSET);
        msgReq.setSign(sign);
        msgReq.setBizContext(bizContext);

        @SuppressWarnings("unchecked")
        Map<String, String> requestParams = JSON.parseObject(JSON.toJSONString(msgReq), Map.class);
        String rspStr = null;
        try {
            rspStr = HttpClientUtil.doPost(requestParams, submitUrl);
            logger.error("[交易状态查询]应答报文:{}", rspStr);
            OrgBaseMsgRsp msgRsp = JSON.parseObject(rspStr, OrgBaseMsgRsp.class);
            if (ValueConstant.CODE_SUCCESS.equals(msgRsp.getCode())) {
                /*解密*/
                String rspBizContext = msgRsp.getBizContext();
                String rspMsg = null;
                try {
                    rspMsg = MerchantUtil.decryptDataByAES(rspBizContext, aesKey, msgReq.getCharset());
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("加密传输数据异常");
                }
                logger.error("[交易状态查询]业务报文:{}", rspMsg);
                /*验签*/
                boolean isTrue = false;
                try {
                    isTrue = MerchantUtil.verify(rspMsg, msgRsp.getSign(), tcosPublicKey, MerchantUtil.SIGNTYPE_RSA, msgReq.getCharset());
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("验签异常！");
                }
                if (isTrue) {
                    logger.error("[交易状态查询]验签成功");
                    OrgPayNotice rsp = JSONObject.parseObject(rspMsg, OrgPayNotice.class);
                    if (RetCdConstant.RET_CD_0000.equals(rsp.getRetCode())) {
                        logger.error("[消息通知]交易成功");
                        return true;
                    }
                } else {
                    logger.error("[交易状态查询]验签失败！");
                }
            } else {
                logger.error("[交易状态查询]交易失败:{}", msgRsp.getMsg());
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("发送post查询支付状态请求失败");
        }
        logger.error("系统异常,查看日志");
        return false;
    }


    /**
     * 给用户发的回调
     *
     * @param jobTrade 返回给用户的信息
     * @throws IOException
     * @throws SignatureException
     */
    public void notify(JobTrade jobTrade) throws IOException, SignatureException {
        JSONObject contentJson = JSONObject.parseObject(jobTrade.getContent());
        JSONObject notifyJson = new JSONObject();
        notifyJson.put("trade_status", "TRADE_SUCCESS");
        notifyJson.put("total_amount", contentJson.getString("totalAmount"));
        notifyJson.put("out_trade_no", contentJson.getString("outTradeNo"));
        notifyJson.put("trade_no", contentJson.getString("tradeNo"));
        notifyJson.put("notify_id", jobTrade.getId() == null ? "" : jobTrade.getId());
        notifyJson.put("payment_time", jobTrade.getExecTime() == null ? new Date().toString() : jobTrade.getExecTime().toString());

        logger.info(String.format("回调方法中:trade_status[%s]", contentJson.getString("retCode")));
        logger.info(String.format("回调方法中:total_amount[%s]", contentJson.getString("totalAmount")));
        logger.info(String.format("回调方法中:out_trade_no[%s]", contentJson.getString("outTradeNo")));
        logger.info(String.format("回调方法中:trade_no[%s]", contentJson.getString("tradeNo")));
        logger.info(String.format("回调方法中:notify_id[%s]", jobTrade.getId() == null ? "" : jobTrade.getId()));
        logger.info(String.format("回调方法中:payment_time[%s]", jobTrade.getExecTime() == null ? new Date().toString() : jobTrade.getExecTime().toString()));
        Trade trade = tradeService.findByMerchantOrderNo(contentJson.getString("outTradeNo"));
        if (trade == null) {
            return;
        }
        Merchant merchant = merchantService.findById(trade.getMerchantId());
        if (merchant == null) {
            return;
        }

        String content = SignatureUtil.getSignatureContent(notifyJson, true);
        String sign = signature.sign(content, merchant.getSecretKey(), Constants.CHARSET_UTF8);
        notifyJson.put("signature", sign);

        int status = 0;
        logger.warn("商户支付结果调度器执行：" + JSONObject.toJSONString(jobTrade));
        logger.warn("给用户发回调的地址：" + jobTrade.getNotifyUrl());
        logger.warn("给用户发回调的内容：" + notifyJson.toJSONString());
        String result = HttpsUtils.post(jobTrade.getNotifyUrl(), null, notifyJson.toJSONString());
        status = Constants.SUCCESS.equals(result) ? 1 : 0;
        logger.warn("执行结果：" + (status == 1 ? "成功" : "失败"));
        jobTrade.setStatus(status);
        jobTrade.setExecTime(DateUtil.getTime());
        jobTrade.setUpdateTime(DateUtil.getTime());
    }


    public static void main(String[] args) throws Exception {
       /* AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo("20181210112333856072601200654117");
        model.setTradeNo("2018121022001401881012110550");
        alipayRequest.setBizModel(model);
        String appId = "2017040806593878";
        String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDK1hHM56FkoY7Znbel9MJFwfcogG2mnhAXlVlgVlPzkfITEpYU9aNCAbMQ97IKdDpPkf/899FJns0fHm9f48e66+gW3Mnp7d9uk+ZNUyMjfDFOpwrhdOyqpMNu5vhF+9XqmFIsyjY4pN6SEQ7VJPNtIj8rEJkS4d01RgFXkLaSlcXvX0uO31qJEuxYsL/ztKl6/LGAiqKK9sc4nd09JjWqOuAz0/Ecagc+60kD2wfeS0tbBkQzOKy1DQ4UdXqfE70w2ihIcVNfkj2D/ieyNPP4lWrBhz4QSg8/iTdG2I61xcdXNhJTycE//suFvGXm2VWAkcVW3p5YbhTktojTqKV3AgMBAAECggEAehjmWivMcSD3NnPECrgNAaTCvLSiTLu1AB080crleicOSwDTKwa6IY+YVMxldfmE6EUAjSw35VMcMnFFpbkdj51V9f0t5gz0hEGvTjiPXFrz616OJC0YEZhgtC+An8/6ct8CCEgo32wPNmniPdeiWL1WvPueyUkAuYLMaAOKgy1mnqq85jnI1/QgbSCs/jExkFAEnDLCLC60KLfAXEwKkiEFWSMoc/txWB7Th4F4cHu41Zc173Nkq+Ioik8/ttO+kiAnEU2D0miLSbZcoD1QS4rUzFFxVSPeIZLJUCSOcZW4cjG0tvNGuHyXliKGbnK2STEf+XTIlcArN1BtnQO6YQKBgQDrYP19yivBpRr5woSZz9PopqENMsia6RU/5Z3m3GW3yEOhqrRuUV0Klr+NoSNTnEh0yt2YpFdKX9mUJnYvx4lbXmAHG0oYit2tSnW8JeGP6WbXKZ4wg2IhfUfySD7Xqps7QBPu/H2J/aOiWuoaCm7cH/YGpokvaKWh9IlkO6bwfwKBgQDcmzjOwu6acxoDefaPZxrpSVyIBbSliPuMVnCRyBjD+mt7koDTdbfTooYrCX9JgqfdFTAO1skPy2dIlSKX9Z16MlF5LIAjTStA2Ms0fVlYMQJr78/Wiz/6s7M9k+scsKCf//dAV/Wte0ISrxI5eilek9aRkmnZIsiPPAz57+1PCQKBgBBo5aNUafKJKTVKa2YxyAtLOqUp6jRqlZGr13NV4D2M17I4rXWXdI4dbmNYXZchqSeDUSmoI3HK9udOOyUfmyLklHtKWsMVQ1kmfMjON0iKNCJCA4ZMUpYVHuP0R3VPrpzV3c3minYJWdQi1HlWSt5L2CIkkCHfANUmsEFtqzjPAoGBAIzD1vnH1Kxk6aJyPmu+pKNbFloNGfnOcIRZ27aJ8ZOLxO2yy9UidkvqlqX7h2cGocombOXrE5yqc5sS3mIMbZG9bdNeG91qrkDQlMlHeq0ViLAK85m4fPihlaujKRDDe5rcn45FEhTCY6S8ZWqbkq4ws2dr/3J4CXWsIbOR7uDRAoGBAJuZqUObNPy4iGPSxcsTvXWStgVHSLHusoGHfSxbMOJJFJzlUzmNNeUVfUXKO2mzSLjzvu//U+lcWDQMvQMJ6ZxYXu65AEdDxqr/hqFdxQ2S3SWgmwu4CSBJiAcdJRjfOo+EAnG8jcSYlNJGW1XgEKsMEdmdjNo9USGciaVezdHW";
        String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDI6d306Q8fIfCOaTXyiUeJHkrIvYISRcc73s3vF1ZT7XN8RNPwJxo8pWaJMmvyTn9N4HQ632qJBVHf8sxHi/fEsraprwCtzvzQETrNRwVxLO5jVmRGi60j8Ue1efIlzPXV9je9mkjzOmdssymZkh2QhUrCmZYI/FCEa3/cNMW0QIDAQAB";

        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                appId, privateKey, AlipayConfig.FORMAT, AlipayConfig.CHARSET,
                publicKey, "RSA");
        try {
            AlipayTradeQueryResponse alipayTradeQueryResponse = alipayClient
                    .execute(alipayRequest);
            System.out.println(JSONObject.toJSONString(alipayTradeQueryResponse));
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        SHPayNotifyController c = new SHPayNotifyController();
        c.payQry("11");
    }

}

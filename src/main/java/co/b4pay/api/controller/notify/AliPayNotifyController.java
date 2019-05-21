package co.b4pay.api.controller.notify;

import co.b4pay.api.common.alipay.AlipayConfig;
import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.dao.MerchantDao;
import co.b4pay.api.model.*;
import co.b4pay.api.service.*;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayResponse;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.TradeFundBill;
import com.alipay.api.request.AlipayTradeOrderSettleRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeOrderSettleResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.demo.trade.model.builder.AlipayTradeQueryRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.model.result.AlipayF2FQueryResult;
import com.alipay.demo.trade.utils.Utils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.math.BigDecimal;
import java.security.SignatureException;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import static co.b4pay.api.common.utils.DateUtil.now;
import static co.b4pay.api.common.utils.DateUtil.yMdhms;

/**
 * 支付宝扫码支付通知
 *
 * @author YK
 * @version $Id v 0.1 2018年06月06日 16:32 Exp $
 */
@RestController
@RequestMapping({"/notify/aliSPayNotify.do", "/notify/aliH5PayNotify.do"})
public class AliPayNotifyController {
    private static final Logger logger = LoggerFactory.getLogger(AliPayNotifyController.class);

    @Autowired
    private TradeService tradeService;

    @Autowired
    private JobTradeService jobTradeService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private AliSPayService aliSPayService;

    @Autowired
    private AliH5PayService aliH5PayService;

    @Autowired
    private HmacSHA1Signature signature = new HmacSHA1Signature();

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private TransinService transinService;

    @RequestMapping(method = {RequestMethod.POST})
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        //System.out.println("支付结果通用通知：" + ServletUtil.getQueryString(request));
        JSONObject jsonObject = new JSONObject();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            jsonObject.put(parameterName, request.getParameter(parameterName));
        }

        if (jsonObject.isEmpty()) {
            return;
        }
        logger.warn("aliPay notify parms ->" + jsonObject.toJSONString());
        String merchantOrderNo = request.getParameter("out_trade_no");
        String appId = request.getParameter("app_id");
        Trade trade = tradeService.findByMerchantOrderNo(merchantOrderNo);
        if (trade == null) {
            logger.warn(String.format("[%s]订单信息不存在", merchantOrderNo));
        }
        Channel channel = channelService.findById(trade.getChannelId() == null ? 0 : trade.getChannelId());
        if (channel == null) {
            logger.warn(String.format("[%s]渠道信息不存在", trade.getChannelId()));
        }
        String tradeNo = request.getParameter("trade_no");
        trade.setPayOrderNo(tradeNo);
        AlipayTradeQueryResponse alipayTradeQueryResponse = null;
        Router router = null;
        try {
            router = channel != null ? channel.getRouter() : null;
            if (router != null) {
                if ("aliSPay".equalsIgnoreCase(router.getId())) {
                    AlipayTradeQueryRequestBuilder builder = new AlipayTradeQueryRequestBuilder()
                            .setTradeNo(tradeNo)
                            .setOutTradeNo(merchantOrderNo);
                    alipayTradeQueryResponse = aliSPayService
                            .getAlipayTradeService(channel)
                            .queryTradeResult(builder)
                            .getResponse();
                } else if ("aliH5Pay".equalsIgnoreCase(router.getId())) {
                    AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
                    AlipayTradeQueryModel model = new AlipayTradeQueryModel();
                    model.setOutTradeNo(merchantOrderNo);
                    model.setTradeNo(tradeNo);
                    model.setOrgPid(appId);
                    alipayRequest.setBizModel(model);
                    alipayTradeQueryResponse = aliH5PayService
                            .getAlipayClient(channel)
                            .execute(alipayRequest);
                }
            }

        } catch (Exception e) {
            logger.error(String.format("请求支付宝api错误，错误原因：%s", e.getMessage()));
        }
        trade.setStatus(1);
        if (alipayTradeQueryResponse != null) {
            logger.warn(String.format("%s 订单查询结果状态：%s", merchantOrderNo, JSONObject.toJSONString(alipayTradeQueryResponse)));
            logger.warn(String.format("%s 订单交易状态：%s", merchantOrderNo, alipayTradeQueryResponse.getTradeStatus()));
            switch (alipayTradeQueryResponse.getTradeStatus()) {
                case "TRADE_SUCCESS":
                    logger.warn("查询返回该订单支付成功: )");
                    trade.setTradeState(1);
                    trade.setPaymentTime(jsonObject.containsKey("gmt_payment") ? jsonObject.getString("gmt_payment") : yMdhms.format(DateUtil.now()));
                    trade.setUpdateTime(now());
                    try {
                        PrintWriter pw = response.getWriter();
                        pw.println("success");
                        pw.flush();
                        pw.close();
                    } catch (Exception e) {
                        logger.error(String.format("支付成功，通知支付宝失败，失败原因：%s", e.getMessage()));
                    }
                    //交易成功，减去渠道相对应金额
                    if (channel.getAmountLimit() == null) {
                        logger.warn(String.format("[%s]渠道剩余额度异常", trade.getChannelId()));
                    }
                    if (trade.getTotalAmount() == null) {
                        logger.warn(String.format("订单号[%s]订单金额异常", trade.getMerchantOrderNo()));
                    }
                    if (channel.getAmountLimit() != null) {
                        channel.setAmountLimit(channel.getAmountLimit().subtract(trade.getTotalAmount()));
                        channel.setLastSuccessTime(now());
                        if (channel.getAmountLimit().compareTo(channel.getAmountMin()) < 0) {
                            channel.setStatus(-1);
                        }
                        channel.setUpdateTime(now());
                        channelService.save(channel);
                    }
                    //交易成功,给用户发回调
                    JobTrade jobTrade = jobTradeService.findById(trade.getId());
                    jobTrade.setExecTime(DateUtil.getTime());
                    jobTrade.setContent(jsonObject.toJSONString());
                    try {
                        notify(jobTrade);
                    } catch (Exception e) {
                        logger.error(String.format("首次异步回调通知异常,订单号:[%s]", jobTrade.getId()), e);
                    }
                    jobTradeService.save(jobTrade);
                    //如果是扫码支付，就进行分账
                    //if (router != null && "aliSPay".equalsIgnoreCase(router.getId())) {
                    //----------------分账代码------------------------------
                    /*AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                            channel.getProdAppid(),
                            channel.getProdPrivateKey(),
                            "json",
                            "GBK",
                            channel.getProdPublicKey(),
                            "RSA2");
                    AlipayTradeOrderSettleRequest request_fz = new AlipayTradeOrderSettleRequest();
                    String out_request_no = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号
                    String transIn = WebUtil.getTransIn();
                    double amonut = trade.getTotalAmount().doubleValue() * channel.getFzPercentage().doubleValue();
                    logger.error("----------------------------");
                    logger.error("appid:" + channel.getProdAppid());
                    logger.error("PrivateKey:" + channel.getProdPrivateKey());
                    logger.error("PublicKey:" + channel.getProdPublicKey());
                    logger.error("-----------------------------------------");
                    logger.error("trade_no:" + trade.getPayOrderNo());
                    logger.error("trans_out:" + channel.getProdPid());

                    List<Transin> transinList = transinService.findByStatus(1);
                    if (transinList != null && transinList.size() > 0) {
                        Random random = new Random();
                        Transin transin = transinList.get(random.nextInt(transinList.size()));

                        logger.error("trans_in:" + transin.getPid());
                        logger.error("amount:" + (double) Math.round(amonut * 100) / 100);
                        logger.error("----------------------------");

                        request_fz.setBizContent("{" +
                                "\"out_request_no\":\"" + out_request_no + "\"," +//该笔分账的请求单号，每一次请求保证唯一
                                "\"trade_no\":\"" + trade.getPayOrderNo() + "\"," +//分账的交易的支付宝交易号
                                "      \"royalty_parameters\":[{" +
                                "        \"trans_out\":\"" + channel.getProdPid() + "\"," +//分出账户,也就是商家的账户pid
                                "\"trans_in\":\"" + transin.getPid() + "\"," +//分账金额收款账户pid
                                "\"amount\":" + (double) Math.round(amonut * 100) / 100 + "," +
                                //"\"amount_percentage\":100," +//分账百分比
                                "\"desc\":\"分账给" + transIn + "\"" +
                                "        }]" +
                                "  }");
                        //index %= 3;
                        AlipayTradeOrderSettleResponse response_fz = null;
                        try {
                            response_fz = alipayClient.execute(request_fz);
                        } catch (AlipayApiException e) {
                            e.printStackTrace();
                            logger.error("分账请求异常");
                        }
                        if (response_fz != null && response_fz.isSuccess()) {
                            trade.setFzStatus(1);
                            trade.setTransinId(transin.getId());
                            BigDecimal fzAmount = trade.getTotalAmount().multiply(new BigDecimal(channel.getFzPercentage()));
                            logger.info("232:分账金额为:" + (double) Math.round((fzAmount.floatValue() * 100)) / 100);
                            trade.setFzAmount(fzAmount);
                            System.out.println("分账调用成功");
                            System.out.println(response_fz.getBody());
                        } else {
                            trade.setFzStatus(2);
                            System.out.println("分账调用成功");
                            System.out.println(response_fz.getBody());
                        }
                    } else {
                        logger.error("没有可用的分账id");
                        tradeService.save(trade);
                        throw new RuntimeException("没有可用的分账id");
                    }
                    //---------------------------------------------------
                    //}
                    break;*/
                case "TRADE_CLOSED":
                    logger.warn("查询返回该订单支付失败或被关闭!!!");
                    break;
                case "WAIT_BUYER_PAY":
                    logger.warn("系统异常，订单支付状态未知!!!");
                    break;
                default:
                    logger.warn("不支持的交易状态，交易返回异常!!!");
                    break;
            }
        }
        tradeService.save(trade);
    }

    public void notify(JobTrade jobTrade) throws IOException, SignatureException {
        JSONObject contentJson = JSONObject.parseObject(jobTrade.getContent());
        JSONObject notifyJson = new JSONObject();
        notifyJson.put("trade_status", contentJson.getString("trade_status"));
        notifyJson.put("total_amount", contentJson.getString("total_amount"));
        notifyJson.put("out_trade_no", contentJson.getString("out_trade_no"));
        notifyJson.put("trade_no", contentJson.getString("trade_no"));
        notifyJson.put("notify_id", contentJson.getString("notify_id"));
        notifyJson.put("payment_time", contentJson.getString("gmt_payment"));

        Trade trade = tradeService.findByMerchantOrderNo(contentJson.getString("out_trade_no"));
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
        logger.warn("支付宝支付结果调度器执行：" + JSONObject.toJSONString(jobTrade));
        logger.warn("给用户发回调的地址：" + jobTrade.getNotifyUrl());
        logger.warn("给用户发回调的内容：" + notifyJson.toJSONString());
        String result = HttpsUtils.post(jobTrade.getNotifyUrl(), null, notifyJson.toJSONString());
        status = Constants.SUCCESS.equals(result) ? 1 : 0;
        logger.warn("给用户回调后的执行结果：" + (status == 1 ? "成功" : "失败"));
        jobTrade.setStatus(status);
        jobTrade.setExecTime(DateUtil.getTime());
        jobTrade.setUpdateTime(DateUtil.getTime());
    }


    public static void main(String[] args) {
        AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
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
        }

    }

}

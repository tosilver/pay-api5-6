package co.b4pay.api.service;

import co.b4pay.api.common.alipay.AlipayConfig;
import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.controller.pay.PersonalFixedCodePayController;
import co.b4pay.api.model.*;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import org.apache.commons.lang3.BooleanUtils;
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

/**
 * 支付宝扫码付
 *
 * @author YK
 * @version $Id: AliH5PayService.java, v 0.1 2018年6月6日 上午9:28:58 YK Exp $
 */
@Service
@Transactional
public class AliH5PayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(AliH5PayService.class);
    private static final String ALIPAY_API_DOMAIN = MainConfig.getConfig("ALIPAY_API_DOMAIN");

    //private AlipayClient client;
    private Map<Long, AlipayClient> alipayClientMap = new HashMap<>();

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
            String result = HttpsUtils.post(channel.getIp4() + "/pay/aliH5PayExecute.do", null, m);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException, AlipayApiException {
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

        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();

        // 封装请求支付信息
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(outTradeNo);
        model.setSubject(subject);
        model.setTotalAmount(totalAmount.toPlainString());
        model.setBody(body);
        model.setTimeoutExpress(timeoutExpress);
        model.setProductCode(productCode);
        alipayRequest.setBizModel(model);
        // 设置异步通知地址
        alipayRequest.setNotifyUrl(String.format("%s/notify/aliH5PayNotify.do", serverUrl));
        // 设置同步地址
        alipayRequest.setReturnUrl(returnUrl);

        AlipayTradeWapPayResponse response = null;
        try {
            response = getAlipayClient(channel).pageExecute(alipayRequest);
        } catch (Exception e) {
            logger.warn("请求支付宝异常，异常原因：" + e.getMessage());
            logger.warn("渠道调用异常，关闭渠道！！！！");
            channel.setStatus(-1);
            channel.setUpdateTime(now());
            channelDao.save(channel);
        }
        if (response != null) {
            if (response.isSuccess()) {
                //System.out.println("调用成功");
//            String responseBody = WebUtils.decode(response.getBody());
//            Map<String, Object> queryStringToMap = WebUtil.convertQueryStringToMap(responseBody);
//            JSONObject jsonObject = new JSONObject(queryStringToMap);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no", outTradeNo);

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
                trade.setResponse(response.getBody());
                trade.setTime(System.currentTimeMillis() - time);
                trade.setFzStatus(0);
                trade.setTradeState(0);
                trade.setStatus(1);
                tradeDao.save(trade);
                logger.warn("ali h5 trade ->" + JSONObject.toJSONString(trade));
                JobTrade jobTrade = new JobTrade();
                jobTrade.setId(trade.getId());
                jobTrade.setStatus(0);
                jobTrade.setCount(0);
                jobTrade.setChannelType(ChannelType.ALIPAY);
                jobTrade.setNotifyUrl(merchantNotifyUrl);
                jobTradeDao.save(jobTrade);

                jsonObject.put("qr_code", serverUrl + "/alipay/cashier.htm?tradeId=" + tradeId);
                return jsonObject;
            } else {
                channel.setStatus(-1);
                channel.setUpdateTime(now());
                channelDao.save(channel);
                logger.warn("渠道调用异常，关闭渠道！！！！");
                throw new BizException("调用失败");
            }
        } else {
            throw new BizException("服务器异常!!!");
        }
    }

    public AlipayClient getAlipayClient(Channel channel) {
        String appId = BooleanUtils.toString(MainConfig.isDevMode, channel.getTestAppid(), channel.getProdAppid());
        String privateKey = BooleanUtils.toString(MainConfig.isDevMode, channel.getTestPrivateKey(), channel.getProdPrivateKey());
        String publicKey = BooleanUtils.toString(MainConfig.isDevMode, channel.getTestPublicKey(), channel.getProdPublicKey());

        AlipayClient alipayClient = alipayClientMap.get(channel.getId());
        if (alipayClient == null) {
            // SDK 公共请求类，包含公共请求参数，以及封装了签名与验签，开发者无需关注签名与验签
            // 调用RSA签名方式
            System.out.println(Configs.getSignType());
            alipayClient = new DefaultAlipayClient(ALIPAY_API_DOMAIN, appId, privateKey, AlipayConfig.FORMAT, AlipayConfig.CHARSET, publicKey, AlipayConfig.SIGNTYPE);

            alipayClientMap.put(channel.getId(), alipayClient);
        }
        return alipayClient;
    }
}

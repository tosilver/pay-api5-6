package co.b4pay.api.service;

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
import co.b4pay.api.dao.MerchantDao;
import co.b4pay.api.model.*;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.TradeStatus;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static co.b4pay.api.common.utils.DateUtil.now;

/**
 * 支付宝扫码付
 *
 * @author YK
 * @version $Id: AliSPayService.java, v 0.1 2018年6月6日 上午9:28:58 YK Exp $
 */
@Service
//@Transactional
public class AliSPayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(AliSPayService.class);

    private static final String ALIPAY_API_DOMAIN = MainConfig.getConfig("ALIPAY_API_DOMAIN");

    private HmacSHA1Signature signature = new HmacSHA1Signature();

    // 支付宝当面付2.0服务
    private Map<Long, AlipayTradeService> alipayTradeServiceMap = new HashMap<>();

    // 支付宝当面付2.0服务（集成了交易保障接口逻辑）
//    private static AlipayTradeService tradeWithHBService;

    // 支付宝交易保障接口服务，供测试接口api使用，请先阅读readme.txt
//    private static AlipayMonitorService monitorService;

    static {
        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
//        alipayTradeService = new AlipayTradeServiceImpl.ClientBuilder().build();

        // 支付宝当面付2.0服务（集成了交易保障接口逻辑）
//        tradeWithHBService = new AlipayTradeWithHBServiceImpl.ClientBuilder().build();

        /** 如果需要在程序中覆盖Configs提供的默认参数, 可以使用ClientBuilder类的setXXX方法修改默认参数 否则使用代码中的默认设置 */
//        monitorService = new AlipayMonitorServiceImpl.ClientBuilder()
//                .setGatewayUrl("http://mcloudmonitor.com/gateway.do").setCharset("GBK")
//                .setFormat("json").build();
    }

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
            String result = HttpsUtils.post(channel.getIp4() + "/pay/aliSPayExecute.do", null, m);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {
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

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = params.getString("tradeNo");//"tradeprecreate" + System.currentTimeMillis() + (long) (Math.random() * 10000000L);

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = params.getString("subject");

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = params.getString("body");

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<>();
        // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
        synchronized (this) {
            Collections.shuffle(goodsList);
            Goods goods1 = goodsList.get(0);
            GoodsDetail goodsDetail = GoodsDetail.newInstance(
                    "goods_id00" + goods1.getId(),
                    goods1.getName(),
                    totalAmount.longValue(),
                    1);
            // 创建好一个商品后添加至商品明细列表
            goodsDetailList.add(goodsDetail);
        }

        // 继续创建并添加第一条商品信息，用户购买的产品为“黑人牙刷”，单价为5.00元，购买了两件
//        GoodsDetail goods2 = GoodsDetail.newInstance("goods_id002", "飞利浦电动牙刷", 500, 2);
//        goodsDetailList.add(goods2);

        String merchantNotifyUrl = params.getString("notifyUrl");


        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject)//
                .setTotalAmount(totalAmount.toPlainString())//
                .setOutTradeNo(outTradeNo)//
                .setUndiscountableAmount(undiscountableAmount)//
                .setSellerId(sellerId)//
                .setBody(body)//
                .setOperatorId(operatorId)//
                .setStoreId(storeId)//
                .setExtendParams(extendParams)//
                .setTimeoutExpress(timeoutExpress) //
                .setNotifyUrl(String.format("%s/notify/aliSPayNotify.do", serverUrl)) //支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);

        AlipayF2FPrecreateResult result = null;
        try {
            result = getAlipayTradeService(channel).tradePrecreate(builder);
        } catch (Exception e) {
            logger.warn("请求支付宝异常，异常原因：" + e.getMessage());
            logger.warn("渠道调用异常，关闭渠道！！！！");
            channel.setStatus(-1);
            channel.setUpdateTime(now());
            channelDao.save(channel);

        }
        if (result != null) {
            if (result.getTradeStatus() == TradeStatus.SUCCESS) {
                AlipayTradePrecreateResponse response = result.getResponse();
                JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                // 服务费
                BigDecimal serviceCharge = totalAmount.multiply(merchantRate.getCostRate(), new MathContext(2, RoundingMode.HALF_UP)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP).add(merchantRate.getPayCost());
                String tradeNo = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号
                Trade trade = new Trade();
                trade.setId(tradeNo);
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
                logger.warn("ali qrcode trade ->" + JSONObject.toJSONString(trade));
                JobTrade jobTrade = new JobTrade();
                jobTrade.setId(trade.getId());
                jobTrade.setStatus(0);
                jobTrade.setCount(0);
                jobTrade.setChannelType(ChannelType.ALIPAY);
                jobTrade.setNotifyUrl(merchantNotifyUrl);
                jobTradeDao.save(jobTrade);
                jsonObject.remove("sign");
                return jsonObject.getJSONObject("alipay_trade_precreate_response");
            } else if (result.getTradeStatus() == TradeStatus.FAILED) {
                channel.setStatus(-1);
                channel.setUpdateTime(now());
                channelDao.save(channel);
                logger.warn("渠道调用异常，关闭渠道！！！！");
                throw new BizException(String.format("[%s]通道预下单失败!!!", channel.getId()));
            } else if (result.getTradeStatus() == TradeStatus.UNKNOWN) {
                throw new BizException(String.format("[%s]通道异常，预下单状态未知!!!", channel.getId()));
            } else {
                throw new BizException("不支持的交易状态，交易返回异常!!!");
            }
        } else {
            throw new BizException("服务器异常!!!");
        }
    }

    // 获取支付宝交易服务对象
    public AlipayTradeService getAlipayTradeService(Channel channel) {
        String appId = BooleanUtils.toString(MainConfig.isDevMode, channel.getTestAppid(), channel.getProdAppid());
        String privateKey = BooleanUtils.toString(MainConfig.isDevMode, channel.getTestPrivateKey(), channel.getProdPrivateKey());
        String publicKey = BooleanUtils.toString(MainConfig.isDevMode, channel.getTestPublicKey(), channel.getProdPublicKey());

        AlipayTradeService alipayTradeService = alipayTradeServiceMap.get(channel.getId());
        if (alipayTradeService == null) {
            /** 使用Configs提供的默认参数
             *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
             */
            AlipayTradeServiceImpl.ClientBuilder builder = new AlipayTradeServiceImpl.ClientBuilder();
            builder.setGatewayUrl(ALIPAY_API_DOMAIN);
            builder.setAppid(appId);
            builder.setPrivateKey(privateKey);
            builder.setAlipayPublicKey(publicKey);
            builder.setFormat("json");
            builder.setCharset("utf-8");
            builder.setSignType(Configs.getSignType());
            alipayTradeService = builder.build();

            alipayTradeServiceMap.put(channel.getId(), alipayTradeService);
        }
        return alipayTradeService;
    }
}

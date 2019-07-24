package co.b4pay.api.service;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.tosdomutils.AESUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.model.*;
import com.alibaba.fastjson.JSONObject;
import org.apache.catalina.util.URLEncoder;
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
import java.util.concurrent.TimeUnit;

import static co.b4pay.api.common.utils.DateUtil.now;


/**
 * qr支付
 */
@Service
public class QRPayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(QRPayService.class);


    /**
     * 定时链接
     */
    private static final String timer_api_domain = MainConfig.getConfig("TIMER_API_DOMAIN");

    private static HmacSHA1Signature signature = new HmacSHA1Signature();


    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {

        logger.info("QR支付Service-->参加校验的参数:" + params);
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        //装换金额为元的单位
        BigDecimal totalMOney = totalAmount.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP);
        logger.warn("QR支付Service-->校验支付金额为:" + totalMOney);
        Channel channel = getChannel(merchantId, router, totalMOney);// 预校验
        logger.warn("QR支付Service-->轮询得到的通道为:" + channel.getName());
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
            String result = HttpsUtils.post(channel.getIp4() + "/pay/qrPayExecute.do", null, m);
            logger.warn("result:" + result);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }


    @Transactional(rollbackFor = Exception.class)
    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {

            logger.info("QR支付Service-->支付参数:" + params);
            String serverUrl = WebUtil.getServerUrl(request);

            long time = System.currentTimeMillis();
            // (必填) 订单总金额，单位为元，不能超过1亿元
            // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
            BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
            String channelId = params.getString("channelId");
            //转换为单位为元的金额
            BigDecimal totalMOney = totalAmount.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP);
            Channel channel = channelDao.getOne(Long.parseLong(channelId));
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


            // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
            // 需保证商户系统端不能重复，建议通过数据库sequence生成，
            String outTradeNo = params.getString("tradeNo");//"tradeprecreate" + System.currentTimeMillis() + (long) (Math.random() * 10000000L);
            Trade byMerchantOrderNotrade = tradeDao.findByMerchantOrderNo(outTradeNo);
            if (byMerchantOrderNotrade != null) {
                Merchant merchant2 = merchantDao.getOne(merchantId);
                merchant2.setStatus(-1);
                merchantDao.save(merchant2);
                throw new BizException(String.format("[%s,%s]订单重复,冻结账户!", merchantId, outTradeNo));
            }
            // B4系统订单号
            String tradeId = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号

            //订单时间
            String orderTime = params.getString("time");
            //回调地址
            String notifyUrl = params.getString("notifyUrl");
            //支付方式
            String payType = params.getString("type");
            int type = Integer.valueOf(payType).intValue();

            //组装响应参数
            JSONObject jsonObject = new JSONObject();
            //校验二维码
            qrcode qrcode = qrCheckOutService.getQrcode(channel.getId().toString(), type, totalMOney);
            String codeData = qrcode.getCodeData();
            Long qrcodeId = qrcode.getId();
            if ("4".equals(payType)) {
                //因为无法跨越shrio登录验证,所以先在这里组装好信息
                BankCardInformation bankCrad = bankCradDao.findByCardNo(codeData);
                String customerName = bankCrad.getCustomerName();
                String bankMark = bankCrad.getBankMark();
                String bankName = bankCrad.getBankName();
                String sign = ToCradSign(codeData, customerName, totalMOney.toPlainString(), bankMark, bankName);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("http://122.114.77.182:8080/qrcode.do").append("?");
                stringBuilder.append("no=").append(sign);
                jsonObject.put("qrcode", stringBuilder.toString());
            } else {
                jsonObject.put("qrcode", codeData);
            }
            jsonObject.put("out_trade_no", outTradeNo);
            jsonObject.put("msg", "接口调用成功");
            jsonObject.put("code", "10000");
            BigDecimal serviceCharge = totalMOney.multiply(merchantRate.getCostRate(), new MathContext(4, RoundingMode.HALF_UP)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP).add(merchantRate.getPayCost());
        try {
            Trade trade = new Trade();
            trade.setId(tradeId);
            trade.setCostRate(merchantRate.getCostRate());
            trade.setPayCost(merchantRate.getPayCost());
            trade.setTotalAmount(totalMOney);
            trade.setRequestAmount(totalMOney);
            trade.setMerchantId(merchantId);
            trade.setChannelId(channel.getId());
            trade.setQrchannelId(channel.getId());
            trade.setQrcodeId(qrcodeId);
            trade.setServiceCharge(serviceCharge); // 服务费
            trade.setAccountAmount(totalMOney.subtract(serviceCharge));
            trade.setNotifyUrl(notifyUrl);
            trade.setMerchantOrderNo(outTradeNo);
            trade.setRequest(params.toJSONString());
            trade.setResponse(jsonObject.toJSONString());
            trade.setTime(System.currentTimeMillis() - time);
            trade.setFzStatus(0);
            trade.setTradeState(0);
            trade.setStatus(1);
            trade.setPayOrderNo(tradeId);
            tradeDao.save(trade);
            logger.warn("qr trade ->" + JSONObject.toJSONString(trade));
            JobTrade jobTrade = new JobTrade();
            jobTrade.setId(trade.getId());
            jobTrade.setStatus(0);
            jobTrade.setCount(0);
            jobTrade.setChannelType(ChannelType.SHPAY);
            jobTrade.setNotifyUrl(notifyUrl);
            jobTradeDao.save(jobTrade);
            logger.info("响应参数为:" + jsonObject.toJSONString());
            /* *//*qrCheckOutService.timer1(outTradeNo);*//*
            //构建向定时任务的请求参数
            StringBuilder sb = new StringBuilder();
            sb.append("?");
            sb.append("tradeNo=").append(outTradeNo);
            // 向商城发送get请求
            HttpClient httpClient = new HttpClient(timer_api_domain + sb.toString());
            httpClient.get();
            String content = httpClient.getContent();
            logger.info("定时任务返回:"+content.trim());*/
            //通道余额

            BigDecimal amountLimit = channel.getAmountLimit();
            logger.info("请求前通道剩余额度为:" + amountLimit);
            //
            BigDecimal subtract = amountLimit.subtract(totalMOney);
            logger.info("请求成功后的通道剩余额度为:" + subtract);
            channel.setAmountLimit(subtract);
            channelDao.save(channel);
            setRedis(outTradeNo);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (BizException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * 生成支付到卡的加密数据
     *
     * @param
     * @return
     */
    public static String ToCradSign(String CardNo, String bankAccount, String money, String bankMark, String bankName) {
        StringBuilder sb = new StringBuilder();
        sb.append("CardNo=").append(CardNo);
        sb.append("&bankAccount=").append(bankAccount);
        sb.append("&money=").append(money);
        sb.append("&amount=").append(money);
        sb.append("&bankMark=").append(bankMark);
        sb.append("&bankName=").append(bankName);
        String encrypt = AESUtil.encrypt(sb.toString(), "bzl");
        logger.warn("加密后的字符串:----" + encrypt);
        URLEncoder urlEncoder = new URLEncoder();
        String encode = urlEncoder.encode(encrypt);
        /*System.out.println("加密:"+encrypt);
        String decrypt = AESUtil.decrypt(encrypt, "bzl");
        System.out.println("解密"+decrypt);*/
        return encode;
    }


    /**
     * 把订单id存进redis缓存,设置过期时间10分钟,10分钟后redis会将失效数据会通知RedisKeyExpirationListener 类
     * 实现的业务为:当请求的订单十分钟内未确认收款,系统回滚订单号对应金额给所属通道
     */
    public void setRedis(String outTradeNo) {
        redisTemplate.opsForValue().set("Order:" + outTradeNo, outTradeNo, 600, TimeUnit.SECONDS);
    }


    /**
     * 补单
     */
    public JSONObject replacement(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {
        long time = System.currentTimeMillis();
        logger.info("QR补单Service-->参数:" + params);
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        //装换金额为元的单位
        BigDecimal totalMOney = totalAmount.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP);
        logger.warn("QR补单Service-->支付金额为:" + totalMOney);
        String outTradeNo = params.getString("tradeNo");
        //回调地址
        String notifyUrl = params.getString("notifyUrl");
        // B4系统订单号
        String tradeId = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号
        MerchantRate merchantRate = merchantRateDao.findByMerchantIdAndRouterId(merchantId, router.getId());
        BigDecimal serviceCharge = totalMOney.multiply(merchantRate.getCostRate(), new MathContext(4, RoundingMode.HALF_UP)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP).add(merchantRate.getPayCost());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no", outTradeNo);
        jsonObject.put("msg", "接口调用成功");
        jsonObject.put("code", "10000");
        jsonObject.put("qrcode", "仅用于补单操作,无需生成二维码!");
        Trade trade = new Trade();
        trade.setId(tradeId);
        trade.setCostRate(merchantRate.getCostRate());
        trade.setPayCost(merchantRate.getPayCost());
        trade.setTotalAmount(totalMOney);
        trade.setRequestAmount(totalMOney);
        trade.setMerchantId(merchantId);
        trade.setChannelId(0L);
        trade.setQrchannelId(0L);
        trade.setQrcodeId(0L);
        trade.setServiceCharge(serviceCharge); // 服务费
        trade.setAccountAmount(totalMOney.subtract(serviceCharge));
        trade.setNotifyUrl(notifyUrl);
        trade.setMerchantOrderNo(outTradeNo);
        trade.setRequest(params.toJSONString());
        trade.setResponse(jsonObject.toJSONString());
        trade.setTime(System.currentTimeMillis() - time);
        trade.setFzStatus(0);
        trade.setTradeState(0);
        trade.setStatus(0);
        trade.setPayOrderNo(tradeId);
        tradeDao.save(trade);
        logger.warn("qr trade ->" + JSONObject.toJSONString(trade));
        JobTrade jobTrade = new JobTrade();
        jobTrade.setId(trade.getId());
        jobTrade.setStatus(0);
        jobTrade.setCount(0);
        jobTrade.setChannelType(ChannelType.SHPAY);
        jobTrade.setNotifyUrl(notifyUrl);
        jobTradeDao.save(jobTrade);
        logger.info("响应参数为:" + jsonObject.toJSONString());
        return jsonObject;
    }






    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("cardNo=").append("6227003105030118573");
        sb.append("&bankAccount=").append("123");
        sb.append("&money=").append("100");
        sb.append("&amount=").append("100");
        sb.append("&bankMark=").append("CCB");
        sb.append("&bankName=").append("中国建设银行");
        String encrypt = AESUtil.encrypt(sb.toString(), "bzl");
        System.out.println("加密:" + encrypt);
        String decrypt = AESUtil.decrypt(encrypt, "bzl");
        System.out.println("解密" + decrypt);
        URLEncoder urlEncoder = new URLEncoder();
        String encode = urlEncoder.encode(encrypt);
        System.out.println(encode);

    }


}
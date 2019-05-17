package co.b4pay.api.service;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.tosdomutils.HttpClient;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.common.zengutils.GetToQuery;
import co.b4pay.api.common.zengutils.RandomAmount;
import co.b4pay.api.model.*;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static co.b4pay.api.common.utils.DateUtil.now;

/**
 * mall支付
 *
 * @author zgp
 */
@Service
//@Transactional
public class MallPayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(MallPayService.class);
    //回调地址
    private static final String notifyIp="http://223.26.48.13:9988/notify/mallPayNotify.do"; //BISI
    //private static final String notifyIp="http://122.114.77.138:9988/notify/mallPayNotify.do"; //彗星专属


    /**
     * 支付链接
     */
    //private static final String MALLPAY_API_DOMAIN = MainConfig.getConfig("MALLPAY_API_DOMAIN");

    private HmacSHA1Signature signature = new HmacSHA1Signature();


    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {

        logger.info("MallPayService-->executeReturn:" + params);
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        //装换金额为元的单位
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
            String result = HttpsUtils.post(channel.getIp4() + "/pay/mallPayExecute.do", null, m);
            logger.warn("result:" + result);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        logger.info("mallPayService-->execute:" + params);
        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);
        long time = System.currentTimeMillis();
        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
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

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = params.getString("tradeNo");//"tradeprecreate" + System.currentTimeMillis() + (long) (Math.random() * 10000000L);
        Trade byMerchantOrderNotrade = tradeDao.findByMerchantOrderNo(outTradeNo);
        if (byMerchantOrderNotrade != null){
            Merchant merchant2 = merchantDao.getOne(merchantId);
            merchant2.setStatus(-1);
            merchantDao.save(merchant2);
            throw new BizException(String.format("[%s,%s]订单重复,冻结账户!",merchantId,outTradeNo));
        }
        // B4系统订单号
        String tradeId = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号

        //订单时间
        String orderTime = params.getString("time");
        //回调地址
        String notifyUrl = params.getString("notifyUrl");
        //支付方式
        //1--支付宝
        String pay_type = "1";
        //得到商户所属通道地址
        MallAccessControl accessControl = mallAccessControlDao.findByMerchantId(merchantId);
        String zfbAccess = accessControl.getZfbAccess();
        logger.info("获取到---[" + merchantId.toString() + "]---的所属通道地址有:" + zfbAccess);

        //从数据库中获取商户地址数组
        String[] arr = zfbAccess.split(",");
        //校验地址数组,获取能正常访问商城地址
        MallAddress mallAddress = mallCheckOutService.checkout(arr, totalMOney,request);
        if (mallAddress == null){
            logger.info("没有符合条件的通道!");
        }
        //获取商城地址
        String address = mallAddress.getAddress();
        Long id = mallAddress.getId();
        //把id转化为long类型
        long addressLongId = Long.valueOf(id);
        //把客户请求金额减去千分一以内的随机数
        BigDecimal amount = RandomAmount.randomAmount(totalMOney);
        //转换为分的单位
        BigDecimal multiply = amount.multiply(new BigDecimal(100));
        int i = multiply.intValue();
        //构建向商场的请求参数
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        sb.append("out_trade_no=").append(tradeId).append("&");
        sb.append("total_fee=").append(i).append("&");
        sb.append("time=").append(orderTime).append("&");
        sb.append("ali_notify_url=").append(notifyIp).append("&");
        sb.append("pay_type=").append(pay_type);
        logger.warn("参数:" + sb.toString());
        // 向商城发送get请求
        HttpClient httpClient = new HttpClient(address + sb.toString());
        // 返回数据
        logger.warn("请求开始:");
        httpClient.get();
        logger.warn("请求结束!!!!!!!");
        String content = httpClient.getContent();
        logger.info("[商城支付]应答报文:  " + content);
        JSONObject rspJson = JSON.parseObject(content);
        if (content != null) {
            if (StringUtils.isNotBlank(rspJson.getString("code")) && "2000".equals(rspJson.getString("code"))) {
                String qrcode = rspJson.getString("data").replace("\\", "");
                logger.info("[商城支付]支付链接:", qrcode);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no", outTradeNo);
                jsonObject.put("msg", "接口调用成功");
                jsonObject.put("code", "10000");
                BigDecimal serviceCharge = totalMOney.multiply(merchantRate.getCostRate(), new MathContext(2, RoundingMode.HALF_UP)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP).add(merchantRate.getPayCost());
                //String tradeId = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号
                Trade trade = new Trade();
                trade.setId(tradeId);
                trade.setCostRate(merchantRate.getCostRate());
                trade.setPayCost(merchantRate.getPayCost());
                trade.setTotalAmount(totalMOney);
                trade.setRequestAmount(amount);
                trade.setMerchantId(merchantId);
                trade.setChannelId(channel.getId());
                trade.setAddressId(addressLongId);
                trade.setServiceCharge(serviceCharge); // 服务费
                trade.setAccountAmount(amount.subtract(serviceCharge));
                trade.setNotifyUrl(notifyUrl);
                trade.setMerchantOrderNo(outTradeNo);
                trade.setRequest(params.toJSONString());
                trade.setResponse(rspJson.toJSONString());
                trade.setTime(System.currentTimeMillis() - time);
                trade.setFzStatus(0);
                trade.setTradeState(0);
                trade.setStatus(1);
                trade.setPayOrderNo(tradeId);
                tradeDao.save(trade);
                logger.warn("MALL trade ->" + JSONObject.toJSONString(trade));
                JobTrade jobTrade = new JobTrade();
                jobTrade.setId(trade.getId());
                jobTrade.setStatus(0);
                jobTrade.setCount(0);
                jobTrade.setChannelType(ChannelType.SHPAY);
                jobTrade.setNotifyUrl(notifyUrl);
                jobTradeDao.save(jobTrade);
                jsonObject.put("qr_code", qrcode);
                //向查询接口发送订单信息
                GetToQuery.getToQuery1(tradeId, i, pay_type, address);
                //触发订单取消计时器
                mallCheckOutService.timer1(outTradeNo);

                return jsonObject;
            } else {
                String msg = rspJson.getString("msg");
                logger.warn("[商城下单]交易失败:" + msg);
                logger.warn("渠道调用异常,冻结账户");
                Merchant merchant = merchantDao.getOne(merchantId);
                merchant.setStatus(-1);
                merchantDao.save(merchant);
                throw new BizException(msg);
            }
        } else {
            throw new BizException("服务器异常!!!");
        }
    }

}
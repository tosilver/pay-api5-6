package co.b4pay.api.service;

import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.tosdomutils.HttpClient;
import co.b4pay.api.common.tosdomutils.IdWorker;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.model.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static co.b4pay.api.common.utils.DateUtil.now;

@Service
@Transactional
public class GRPayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(GRPayService.class);

    //private static final String ALIPAY_API_DOMAIN = MainConfig.getConfig("ALIPAY_API_DOMAIN");

    private IdWorker idWorker = new IdWorker();

    private HmacSHA1Signature signature = new HmacSHA1Signature();

    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {

        logger.info("GRPayService-->executeReturn:" + params);
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        Channel channel = getChannel(merchantId, router, totalAmount); // 预校验
        if (channel.getIp4() == null) {
            channel.setStatus(-1);
            channel.setUpdateTime(now());
            channelDao.save(channel);
            throw new BizException("渠道地址设置异常");
        }
        Map map = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            map.put(parameterName, request.getParameter(parameterName));
        }
        map.put("channelId", channel.getId().toString());
        map.remove("signature");
        try {
            String content = SignatureUtil.getSignatureContent(map, true);
            String sign = signature.sign(content, merchantDao.getOne(merchantId).getSecretKey(), Constants.CHARSET_UTF8);
            map.put("signature", sign);
            String result = HttpsUtils.post(channel.getIp4() + "/pay/grPayExecute.do", null, map);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }
    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        logger.info("GRPayService-->execute:" + params);

        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);

        String getgrOrderUrl = WebUtil.getgrOrderUrl(request);
        logger.warn("getgrOrderUrl :" + getgrOrderUrl);
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

        //构建请求参数
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        //long diyOrderId = idWorker.nextId();
        sb.append("orderid=").append(outTradeNo).append("&");
        sb.append("order_money=").append(totalAmount).append("&");
        sb.append("memo=test_").append(outTradeNo).append("&");
        sb.append("notifyUrl=").append(String.format("%s/notify/grPayNotify.do", serverUrl));
        logger.warn("参数:" + sb.toString());
        //String url = "http://112.213.118.86:8888/order/add";
        //String url = "http://112.213.118.133:8888/order/add";
        //String url = "http://api.b4bishi.com:8888/order/add";
        HttpClient httpClient = new HttpClient(
                getgrOrderUrl + sb.toString());
        httpClient.get();
        String content = httpClient.getContent();
        if (content == null || "".equals(content)) {
            throw new BizException("响应数据为空");
        }
        System.out.println("[个码下单]应答报文:  " + content);
        JSONObject rspJson = JSON.parseObject(content);
        if (StringUtils.isNotBlank(rspJson.getString("code")) && "200".equals(rspJson.getString("code"))) {
            String qrcode = rspJson.getString("qrcode").replace("\\", "");
            logger.info("[个码下单]支付链接:", qrcode);

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
            logger.warn("GR trade ->" + JSONObject.toJSONString(trade));
            JobTrade jobTrade = new JobTrade();
            jobTrade.setId(trade.getId());
            jobTrade.setStatus(0);
            jobTrade.setCount(0);
            jobTrade.setChannelType(ChannelType.SHPAY);
            jobTrade.setNotifyUrl(merchantNotifyUrl);
            jobTradeDao.save(jobTrade);

            jsonObject.put("qr_code", qrcode);
            return jsonObject;
        } else {
            String msg = rspJson.getString("msg");
            logger.warn("[个码下单]交易失败:" + msg);
            logger.warn("渠道调用异常，关闭渠道！！！！");
            channel.setStatus(-1);
            channel.setUpdateTime(now());
            channelDao.save(channel);
            throw new BizException(msg);
        }
    }
}

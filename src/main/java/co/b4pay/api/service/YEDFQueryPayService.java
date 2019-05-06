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
import co.b4pay.api.common.zengutils.ESIDGenerate;
import co.b4pay.api.common.zengutils.HCMD5;
import co.b4pay.api.common.zengutils.HttpClientUtil;
import co.b4pay.api.model.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.demo.trade.config.Configs;
import com.google.zxing.WriterException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

import static co.b4pay.api.common.utils.DateUtil.now;

/**
 * 快捷支付/余额代付查询
 *
 * @author zgp
 * @version
 */
@Service
//@Transactional
public class YEDFQueryPayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(YEDFQueryPayService.class);

    private static final String kJPAY_API_DOMAIN = MainConfig.getConfig("kJPAY_API_DOMAIN");

    private HmacSHA1Signature signature = new HmacSHA1Signature();

    static {
        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("kjinfo.properties");
    }

    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {

        logger.info("yedfqueryPayService-->executeReturn:" + params);
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
            String result = HttpsUtils.post(channel.getIp4() + "/pay/kyqueryPayExecute.do", null, m);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        logger.info("yedfqueryPayService-->execute:" + params);

        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);
        long time = System.currentTimeMillis();
        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        //BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        Channel channel = null;
        if (params.containsKey("channelId")) {
            channel = channelDao.getOne(Long.parseLong(params.getString("channelId")));
        }
        if (channel == null || channel.getStatus() < 0) {
            throw new BizException("渠道异常,请稍后再试！");
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

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = params.getString("tradeNo");//"tradeprecreate" + System.currentTimeMillis() + (long) (Math.random() * 10000000L);
        //商户订单号
        String merchOrderNo = params.getString("merch_order_no");
        //协议类型  Q查询
        String agreType = params.getString("agre_type");
        //交易平台订单号
        String platformOrderNo = params.getString("platform_order_no");
        //订单时间
        String orderDatetime = params.getString("order_datetime");
        //查询类型 0 交易订单查询   1 余额代付订单查询
        String queryId = params.getString("query_id");
        //商户密钥
        String secretKey = "1998919a22884b37a9b3e3d316a54a45";

        //封装查询所需要的参数
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("version", "V001");
        paramMap.put("agre_type",agreType);
        paramMap.put("inst_no", "10000094");
        paramMap.put("merch_id", "100000941000087");
        paramMap.put("merch_order_no", merchOrderNo);
        paramMap.put("platform_order_no", platformOrderNo);
        paramMap.put("query_id",queryId);
        paramMap.put("order_datetime", orderDatetime);
        //生成签名
        String sign = this.getPerSign(paramMap, secretKey);
        paramMap.put("sign", sign);
        //转换成JSON字符串
        String jsonString = JSONObject.toJSONString(paramMap);
        logger.warn("查询JSON字符串为:" + jsonString);
        // 发送post请求
        HttpClientUtil httpClientUtil = new HttpClientUtil();
        // 返回数据
        logger.warn("查询请求开始:");
        String result = httpClientUtil.doPost(kJPAY_API_DOMAIN, jsonString, "utf-8");
        logger.warn("查询请求结束!!!!!!!");
        HashMap<String, String> resultInfo = getResultInfo(result, jsonString, secretKey, kJPAY_API_DOMAIN);
        String responseCreateOrder = resultInfo.get("responseCreateOrder");
        System.out.println("[快捷查询]应答报文:  " + responseCreateOrder);
        JSONObject rspJson = JSONObject.parseObject(responseCreateOrder);
        logger.warn("[快捷查询]查询应答报文转为json字符串"+rspJson);
        String rsqString = JSON.toJSONString(rspJson);
        if (rspJson != null) {
            if (StringUtils.isNotBlank(rspJson.getString("retcode")) && "00".equals(rspJson.getString("retcode"))) {
                String retmsg = rspJson.getString("retmsg");
                logger.info("[快捷查询]返回的结果:", retmsg);
                //支付方式
                String payType = rspJson.getString("pay_type");
                //金额
                String money = rspJson.getString("amount");
                //应答码
                String retcode = rspJson.getString("retcode");
                //交易平台订单号
                String platformOrderNo1 = rspJson.getString("platform_order_no");
                //支付时间
                String payTime = rspJson.getString("pay_time");
                //借贷标识
                String isCredit = rspJson.getString("is_credit");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no",outTradeNo);
                jsonObject.put("retcode",retcode);
                jsonObject.put("retmsg", retmsg);
                jsonObject.put("pay_type", payType);
                jsonObject.put("money", money);
                jsonObject.put("platform_order_no", platformOrderNo1);
                jsonObject.put("pay_time", payTime);
                jsonObject.put("is_credit", isCredit);
               /* BigDecimal serviceCharge = totalAmount.multiply(merchantRate.getCostRate(), new MathContext(2, RoundingMode.HALF_UP)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP).add(merchantRate.getPayCost());
                // B4系统订单号
                String tradeId = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号*//*
                Trade trade = tradeDao.findByMerchantOrderNo(merchOrderNo);
                trade.setStatus(1);
                trade.setTradeState(1);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                trade.setPaymentTime(sdf.format(new Date()));
                trade.setUpdateTime(now());
                tradeDao.save(trade);
                logger.warn("KJquery trade ->" + JSONObject.toJSONString(trade));
                JobTrade jobTrade = jobTradeService.findById(trade.getId());
                jobTrade.setExecTime(DateUtil.getTime());
                jobTrade.setContent(rsqString);
                jobTradeDao.save(jobTrade);*/
                return jsonObject;
            } else if (StringUtils.isNotBlank(rspJson.getString("retcode")) && "99".equals(rspJson.getString("retcode"))){
                String retmsg = rspJson.getString("retmsg");
                logger.info("[快捷查询]返回的结果:", retmsg);
                //支付方式
                String payType = rspJson.getString("pay_type");
                //金额
                String money = rspJson.getString("amount");
                //应答码
                String retcode = rspJson.getString("retcode");
                //交易平台订单号
                String platformOrderNo1 = rspJson.getString("platform_order_no");
                //支付时间
                String payTime = rspJson.getString("pay_time");
                //借贷标识
                String isCredit = rspJson.getString("is_credit");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no",outTradeNo);
                jsonObject.put("retcode",retcode);
                jsonObject.put("retmsg", retmsg);
                jsonObject.put("pay_type", payType);
                jsonObject.put("money", money);
                jsonObject.put("platform_order_no", platformOrderNo1);
                jsonObject.put("pay_time", payTime);
                jsonObject.put("is_credit", isCredit);
                return jsonObject;
            }else {
                String msg = rspJson.getString("retmsg");
                logger.warn("[快捷查询]查询交易失败:" + msg);
                throw new BizException(msg);
            }
        } else {
            throw new BizException("服务器异常!!!");
        }
    }

    /**
     * 生成签名前数据处理，并生成签名
     */
    public String getPerSign(HashMap<String, String> map, String secretKey) {
        // 获取非空的数据放入signMap    按字典序排序
        TreeMap<String, String> signMap = new TreeMap<String, String>();
        for (String key1 : map.keySet()) {
            String str = (String) map.get(key1);
            // 对于空的数据或者为空字符串的数据不参与签名
            if (str != null && str.length() > 0) {
                signMap.put(key1, str);
            }
        }
        return getSign(signMap, secretKey);
    }

    /**
     * 生成签名
     */
    public String getSign(Map<String, String> map, String secretKey) {
        // 移除前端多传的数据，这个根据系统的情况来定，如果没有则不需要移除，最终只保留文档上且传进来非空的数据
        map.remove("requestType");
        map.remove("secretKey");
        map.remove("tradeNo");
        map.remove("subject");
        map.remove("body");

        StringBuffer str = new StringBuffer();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            str.append(entry.getKey() + "=" + entry.getValue() + "&");
        }
        // 商户秘钥拼接
        str.append("key=" + secretKey);
        // MD5加密
        String sign = HCMD5.MD5(str.toString(), "utf-8").toLowerCase();
        logger.info("生成签名数据 :" + str.toString() + " \n生成的签名 :" + sign);
        return sign;
    }

    /***
     * 对服务器返回数据进行验签，然后生成二维码返回给前端
     *
     * @param result
     *            返回数据
     * @param jsonParam
     *            请求数据
     * @param secretKey
     *            秘钥
     * @param url
     *            请求URL
     * @return map 返回给浏览器
     * @throws WriterException
     * @throws IOException
     */
    public HashMap<String, String> getResultInfo(String result, String jsonParam, String secretKey, String url)
            throws WriterException, IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("requestCreateOrder", jsonParam + "请求Url:  " + url);
        if (result == null) {
            map.put("responseCreateOrder", "请求地址不正确");
        } else {
            // 将返回数据转换为map
            Map<?, ?> resultMap = (Map<?, ?>) JSON.parse(result);
            @SuppressWarnings("unchecked")
            Map<String, String> perMap = (Map<String, String>) resultMap;
            //封装参与签名的数据放到resignMap   按字典序排序
            TreeMap<String, String> resignMap = new TreeMap<String, String>();
            logger.info("[返回数据resultMap]:" + resultMap.toString());
            for (String key : perMap.keySet()) {
                String value = resultMap.get(key).toString();
                if (value != null && value.length() > 0)
                    resignMap.put(key, value);
            }
            resignMap.remove("sign");
            String resign = getSign(resignMap, secretKey);// 返回数据也需要验签
            if (resign.equals(resultMap.get("sign").toString())) {// 判断返回签名和本地签名是否一致
                map.put("responseCreateOrder", result);
            } else {
                map.put("responseCreateOrder", "返回数据验签失败");
            }
        }
        return map;
    }

}
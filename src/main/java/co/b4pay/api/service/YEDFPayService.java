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
import co.b4pay.api.model.base.AjaxResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.demo.trade.config.Configs;
import com.google.zxing.WriterException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
 * 快捷支付
 *
 * @author zgp
 */
@Service
//@Transactional
public class YEDFPayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(YEDFPayService.class);

    private static final String kJPAY_API_DOMAIN = MainConfig.getConfig("kJPAY_API_DOMAIN");

    private HmacSHA1Signature signature = new HmacSHA1Signature();

    static {
        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("kjinfo.properties");
    }

    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {

        logger.info("yedfPayService-->executeReturn:" + params);
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
            String result = HttpsUtils.post(channel.getIp4() + "/pay/yedfPayExecute.do", null, m);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        logger.info("yedfPayService-->execute:" + params);

        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);
        long time = System.currentTimeMillis();
        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        //把交易金额转换成元的单位
        BigDecimal totalMoney = totalAmount.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP);
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
        //判断商户余额
        Merchant merchant = merchantDao.getOne(merchantId);
        BigDecimal accountBalance = merchant.getAccountBalance();
        BigDecimal balance = merchant.getBalance();
        logger.info("商户入账余额:" + accountBalance);
        logger.info("下发金额:" + totalMoney);

        int i1 = accountBalance.compareTo(totalMoney);
        if (i1 < 0) {
            throw new BizException(String.format("[%s]商户余额不足!", router.getId(), merchantRate.getPayCost()));
        }

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = params.getString("tradeNo");
        if (yedfPayrollDao.findByMerchantOrderNo(outTradeNo) != null) {
            throw new BizException("交易流水重复");
        }
        //"tradeprecreate" + System.currentTimeMillis() + (long) (Math.random() * 10000000L);

        //商户订单号
        String merchantOrderNo = ESIDGenerate.getUUID();
        //对公标识 0对私 1对公
        String isCompay = params.getString("is_compay");
        //订单时间 创建订单的日期，格式:2017-11-04 00:00:00
        String orderDatetime = params.getString("order_datetime");
        //(非必填)
        String tradeOrderNo = params.getString("trade_order_no");

        //持卡人姓名 入账户主姓名
        String customerName = params.getString("customer_name");
        //证件类型  入账户主证件类型
        //01：身份证;
        //02：军官证;
        //03：护照;
        //04：回乡证;
        //05：台胞证;
        //06：警官证;
        String customerCertType = params.getString("customer_cert_type");
        //持卡人证件编号 入账户主证件号
        String customerCertId = params.getString("customer_cert_id");
        //持卡人手机号码 入账户主预留手机号
        String customerPhone = params.getString("customer_phone");
        //联行号
        String bankNo = params.getString("bank_no");
        //银行简称 例农业银行缩写为ABC
        String bankShortName = params.getString("bank_short_name");
        //开户行名称
        String bankName = params.getString("bank_name");
        //入账卡号 收款人的银行账户
        String bankCardNo = params.getString("bank_card_no");
        //金额 单位分，字符串型
        //如1元 ，填 “100”
        String money = params.getString("money");
        //备注
        String remark = params.getString("remark");
        //支付方式
        String payType = params.getString("pay_type");
        //协议类型
        String agreType = params.getString("agre_type");
        //商户密钥
        String secretKey = "91c9fa29a1534e4b95555cf7d265e570";


        //封装调用快捷接口时需要上传的参数
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("version", "V001");
        paramMap.put("agre_type", agreType);
        paramMap.put("pay_type", payType);
        paramMap.put("inst_no", "10000094");
        paramMap.put("merch_id", "100000941000167");
        paramMap.put("is_compay", isCompay);
        paramMap.put("order_datetime", orderDatetime);
        paramMap.put("amount", money);
        paramMap.put("merch_order_no", merchantOrderNo);
        paramMap.put("trade_order_no", tradeOrderNo);
        paramMap.put("customer_name", customerName);
        paramMap.put("customer_cert_type", customerCertType);
        paramMap.put("customer_cert_id", customerCertId);
        paramMap.put("customer_phone", customerPhone);
        paramMap.put("bank_no", bankNo);
        paramMap.put("bank_short_name", bankShortName);
        paramMap.put("bank_name", bankName);
        paramMap.put("bank_card_no", bankCardNo);
        paramMap.put("remark", remark);
        //生成签名
        String sign = this.getPerSign(paramMap, secretKey);
        paramMap.put("sign", sign);
        //转换成JSON字符串
        String jsonString = JSONObject.toJSONString(paramMap);
        logger.warn("JSON字符串为:" + jsonString);

       /* HttpClient httpClient = new HttpClient(kJPAY_API_DOMAIN, paramMap);
        httpClient.post();
        String content = httpClient.getContent();
        if (content == null || "".equals(content)) {
            throw new BizException("响应数据为空");
        }*/
        // 发送post请求
        HttpClientUtil httpClientUtil = new HttpClientUtil();
        // 返回数据
        logger.warn("请求开始:");
        String result = httpClientUtil.doPost(kJPAY_API_DOMAIN, jsonString, "utf-8");
        logger.warn("请求结束!!!!!!!");
        HashMap<String, String> resultInfo = getResultInfo(result, jsonString, secretKey, kJPAY_API_DOMAIN);
        String responseCreateOrder = resultInfo.get("responseCreateOrder");
        System.out.println("[余额代付]应答报文:  " + responseCreateOrder);
        /**
         * 账户余额操作
         */
        //需要从账户中扣掉的金额 请求金额+手续费
        BigDecimal deductedAmount = totalMoney.add(new BigDecimal("3.00"));
        logger.warn("需要从账户中扣掉的金额" + deductedAmount);
        //请求有应答则从账户中扣除金额
        merchant.setBalance(balance.subtract(deductedAmount));
        merchant.setAccountBalance(accountBalance.subtract(deductedAmount));
        //把金额加进去冻结金额
        BigDecimal accountFrozen = merchant.getAccount_frozen();
        merchant.setAccount_frozen(accountFrozen.add(deductedAmount));
        merchantDao.save(merchant);
        //费率
        BigDecimal serviceCharge = totalAmount.multiply(merchantRate.getCostRate(), new MathContext(2, RoundingMode.HALF_UP)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP).add(merchantRate.getPayCost());
        //保存订单
        //系统订单号
        String tradeId = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号
        //YEDFPayroll yedfPayroll = yedfPayrollDao.findByMerchantOrderNo(merchantOrderNo);
        YEDFPayroll yedfPayroll = new YEDFPayroll();
        yedfPayroll.setId(tradeId);
        yedfPayroll.setCostRate(merchantRate.getCostRate());
        yedfPayroll.setPayCost(merchantRate.getPayCost());
        yedfPayroll.setTotalAmount(totalMoney);
        yedfPayroll.setMerchantId(merchantId);
        yedfPayroll.setChannelId(channel.getId());
        yedfPayroll.setServiceCharge(serviceCharge); // 服务费
        yedfPayroll.setAccountAmount(totalMoney.subtract(serviceCharge));
        yedfPayroll.setMerchantOrderNo(outTradeNo);
        yedfPayroll.setRequest(params.toJSONString());
        yedfPayroll.setTradeState(0);
        yedfPayroll.setStatus(1);
        yedfPayroll.setPayOrderNo(merchantOrderNo);
        yedfPayroll.setBankCardNo(bankCardNo);
        yedfPayroll.setBankName(bankName);
        yedfPayroll.setBankShortName(bankShortName);
        yedfPayroll.setCustomer_name(customerName);
        yedfPayrollDao.save(yedfPayroll);
        logger.warn("KJ trade ->" + JSONObject.toJSONString(yedfPayroll));
        JobTrade jobTrade = new JobTrade();
        jobTrade.setId(yedfPayroll.getId());
        jobTrade.setNotifyUrl("");
        jobTrade.setContent(responseCreateOrder);
        jobTrade.setStatus(-1);
        jobTrade.setCount(0);
        jobTrade.setChannelType(ChannelType.YEDFPAY);
        jobTradeDao.save(jobTrade);

        if (responseCreateOrder != null) {
            JSONObject rspJson = JSONObject.parseObject(responseCreateOrder);
            if (StringUtils.isNotBlank(rspJson.getString("retcode")) && "00".equals(rspJson.getString("retcode"))) {
                String retmsg = rspJson.getString("retmsg");
                logger.info("[余额代付]返回结果:", retmsg);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no", outTradeNo);
                jsonObject.put("merch_order_no", merchantOrderNo);
                jsonObject.put("code", "00");
                jsonObject.put("retmsg", retmsg);

                //从冻结金额减去已经成功代付的金额
                Merchant merchant1 = merchantDao.getOne(merchantId);
                BigDecimal accountFrozen1 = merchant1.getAccount_frozen();
                logger.warn("现在的冻结金额为:" + accountFrozen1);
                merchant1.setAccount_frozen(accountFrozen1.subtract(deductedAmount));
                merchantDao.save(merchant1);
                logger.warn("交易成功,从冻结金额中减去代付金额!" + deductedAmount);
                //修改状态为已支付
                YEDFPayroll yedfPayroll1 = yedfPayrollDao.getOne(tradeId);
                yedfPayroll1.setTradeState(1);
                yedfPayrollDao.save(yedfPayroll1);
                logger.warn("KJquery trade ->" + JSONObject.toJSONString(yedfPayroll));
                JobTrade jobTrade1 = jobTradeService.findById(yedfPayroll.getId());
                jobTrade1.setStatus(1);
                jobTradeDao.save(jobTrade);
                return jsonObject;
            } else if (StringUtils.isNotBlank(rspJson.getString("retcode")) && "99".equals(rspJson.getString("retcode"))) {
                JSONObject ordernquiry = ordernquiry(rspJson, totalAmount, merchantRate, channel, merchantId);
                logger.warn("ordernquiry" + ordernquiry.toString());
                if ("00".equals(ordernquiry.getString("retcode"))) {

                    //从冻结金额减去已经成功代付的金额
                    Merchant merchant2 = merchantDao.getOne(merchantId);
                    BigDecimal accountFrozen2 = merchant2.getAccount_frozen();
                    logger.warn("现在的冻结金额为" + accountFrozen2);
                    merchant2.setAccount_frozen(accountFrozen2.subtract(deductedAmount));
                    merchantDao.save(merchant2);
                    logger.warn("查询成功,从冻结金额减去请求成功的金额" + deductedAmount);
                    //修改状态为已支付
                    YEDFPayroll yedfPayroll1 = yedfPayrollDao.getOne(tradeId);
                    yedfPayroll1.setTradeState(1);
                    Date date = new Date();
                    String dateStr = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(date);
                    yedfPayroll.setPaymentTime(dateStr);
                    yedfPayrollDao.save(yedfPayroll1);
                    logger.warn("KJquery trade ->" + JSONObject.toJSONString(yedfPayroll));
                    JobTrade jobTrade1 = jobTradeService.findById(yedfPayroll.getId());
                    jobTrade1.setStatus(1);
                    jobTradeDao.save(jobTrade);
                    return ordernquiry;
                } else if ("01".equals(ordernquiry.getString("retcode"))) {
                    String retmsg = ordernquiry.getString("retmsg");
                    logger.warn("返回的消息:" + retmsg);
                    //修改状态为支付失败
                    YEDFPayroll yedfPayroll1 = yedfPayrollDao.getOne(tradeId);
                    yedfPayroll1.setTradeState(-1);
                    yedfPayrollDao.save(yedfPayroll1);
                    logger.warn("KJquery trade ->" + JSONObject.toJSONString(yedfPayroll));
                    JobTrade jobTrade1 = jobTradeService.findById(yedfPayroll.getId());
                    jobTrade1.setStatus(0);
                    jobTradeDao.save(jobTrade);
                    logger.warn("交易失败,冻结交易金额:" + deductedAmount);
                    return ordernquiry;
                } else if ("99".equals(ordernquiry.getString("retcode"))) {
                    return ordernquiry;
                }
                String retmsg = rspJson.getString("retmsg");
                logger.info("[余额代付]返回结果:", retmsg);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no", outTradeNo);
                jsonObject.put("merch_order_no", merchantOrderNo);
                jsonObject.put("retcode", "99");
                jsonObject.put("retmsg", retmsg);
                /*//修改状态为支付失败
                YEDFPayroll yedfPayroll1 = yedfPayrollDao.getOne(tradeId);
                yedfPayroll1.setTradeState(-1);
                yedfPayrollDao.save(yedfPayroll1);
                logger.warn("KJquery trade ->" + JSONObject.toJSONString(yedfPayroll));
                JobTrade jobTrade1 = jobTradeService.findById(yedfPayroll.getId());
                jobTrade1.setStatus(0);
                jobTradeDao.save(jobTrade);*/
                return jsonObject;
            } else {
                String msg = rspJson.getString("retmsg");
                logger.warn("[余额代付]交易失败:" + msg);
                logger.warn("冻结交易金额:" + deductedAmount);
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


    /**
     * 订单状态查询
     */
    public JSONObject ordernquiry(JSONObject params, BigDecimal totalAmount, MerchantRate merchantRate, Channel channel, Long merchantId) throws IOException, WriterException {
        //提取所需参数
        String outTradeNo = params.getString("tradeNo");
        //商户订单号
        String merchOrderNo = params.getString("merch_order_no");
        //交易平台订单号
        String platformOrderNo = params.getString("platform_order_no");
        //订单时间
        String orderDatetime = params.getString("order_datetime");
        String bankNo = params.getString("bank_no");
        //银行简称 例农业银行缩写为ABC
        String bankShortName = params.getString("bank_short_name");
        //开户行名称
        String bankName = params.getString("bank_name");
        //入账卡号 收款人的银行账户
        String bankCardNo = params.getString("bank_card_no");
        //持卡人姓名 入账户主姓名
        String customerName = params.getString("customer_name");
        //商户密钥
        String secretKey = "1998919a22884b37a9b3e3d316a54a45";

        //封装查询所需要的参数
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("version", "V001");
        paramMap.put("agre_type", "Q");
        paramMap.put("inst_no", "10000094");
        paramMap.put("merch_id", "100000941000087");
        paramMap.put("merch_order_no", merchOrderNo);
        paramMap.put("platform_order_no", platformOrderNo);
        paramMap.put("query_id", "1");
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
        System.out.println("[余额查询]查询应答报文:  " + responseCreateOrder);
        JSONObject rspJson = JSONObject.parseObject(responseCreateOrder);
        logger.warn("[余额查询]查询应答报文转为json字符串" + rspJson);
        if (rspJson != null) {
            if (StringUtils.isNotBlank(rspJson.getString("retcode")) && "00".equals(rspJson.getString("retcode"))) {
                String retmsg = rspJson.getString("retmsg");
                String merchOrderNo1 = rspJson.getString("merch_order_no");
                logger.info("[余额查询]返回的结果:", retmsg);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no", outTradeNo);
                jsonObject.put("merch_order_no", merchOrderNo1);
                jsonObject.put("retcode", "00");
                jsonObject.put("retmsg", retmsg);
                return jsonObject;
            } else if (StringUtils.isNotBlank(rspJson.getString("retcode")) && "99".equals(rspJson.getString("retcode"))) {
                String retmsg = rspJson.getString("retmsg");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no", outTradeNo);
                jsonObject.put("merch_order_no", merchOrderNo);
                jsonObject.put("retcode", "99");
                jsonObject.put("retmsg", retmsg);
                return jsonObject;
            } else {
                String retmsg = rspJson.getString("retmsg");
                logger.warn("[余额查询]查询交易失败:" + retmsg);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no", outTradeNo);
                jsonObject.put("merch_order_no", merchOrderNo);
                jsonObject.put("retcode", rspJson.getString("retcode"));
                jsonObject.put("retmsg", retmsg);
                return jsonObject;
            }
        } else {
            throw new BizException("服务器异常!!!");
        }
    }


    /**
     * 金额格式有误,返回需要回退的金额
     *
     * @param amount 易付宝那边的返回的金额数量
     * @return 回退的金额
     * @throws Exception
     */
    protected BigDecimal changeF2Y(String amount) throws Exception {

        String CURRENCY_FEN_REGEX = "\\-?[0-9]+";

        if (!amount.matches(CURRENCY_FEN_REGEX)) {
            throw new Exception("金额格式有误");
        }
        return BigDecimal.valueOf(Long.parseLong(amount)).divide(new BigDecimal(100));
    }
}
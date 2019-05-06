package co.b4pay.api.controller.notify;

import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.tosdomutils.HttpClient;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.dao.MerchantDao;
import co.b4pay.api.model.*;
import co.b4pay.api.service.*;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeOrderSettleRequest;
import com.alipay.api.response.AlipayTradeOrderSettleResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static co.b4pay.api.common.utils.DateUtil.now;

/**
 * mall支付通知(新版测试)
 *
 * @author YK
 * @version $Id v 0.1 2018年06月06日 16:32 Exp $
 */
@RestController
@RequestMapping({"/notify/mallPayNotify.do"})
public class MALLPayNotifyController {
    private static final Logger logger = LoggerFactory.getLogger(MALLPayNotifyController.class);

    private static final String query="http://202.53.137.124:8080/notify.do";

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

    @Autowired
    private MerchantDao merchantDao;

    @Autowired
    private MallAddressService mallAddressService;

    @Autowired
    private TransinService transinService;


    @RequestMapping(method = {RequestMethod.POST})
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.error("商城支付回调进来了");
        String param = null;
        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);

            JSONObject jsonObject = JSONObject.parseObject(responseStrBuilder.toString());
            param = jsonObject.toJSONString();
            logger.warn("request回来的参数:" + param);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSONObject.parseObject(param);
        //订单号
        String PayOrderNo = jsonObject.getString("out_trade_no");
        logger.warn("merchantOrderNo------------>" + PayOrderNo);
        //支付金额
        String totalAmount = jsonObject.getString("total_amount");
        //支付状态
        String tradestatus = jsonObject.getString("trade_status");

        //支付宝订单号
        String tradeNo = jsonObject.getString("trade_no");

        Trade trade = tradeService.findById(PayOrderNo);
        Long merchantId = trade.getMerchantId();
        Integer tradeState = trade.getTradeState();
        //获取请求金额
        BigDecimal requestAmount = trade.getRequestAmount();
        //到账金额
        BigDecimal accountAmount = trade.getAccountAmount();
        //获取订单的所属商城地址
        Long addressId = trade.getAddressId();
        logger.warn("trade--------------------->" + trade);
        Channel channel = channelService.findById(trade.getChannelId() == null ? 0 : trade.getChannelId());
        if (channel == null) {
            logger.warn(String.format("[%s]渠道信息不存在", trade.getChannelId()));
        }
        if ("TRADE_SUCCESS".equalsIgnoreCase(tradestatus) && new BigDecimal(totalAmount).compareTo(requestAmount)==0) {
            if (trade != null && tradeState == 0) {
                logger.info("收款成功，改变状态为1");
                trade.setStatus(1);
                trade.setTradeState(1);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                trade.setPaymentTime(sdf.format(new Date()));
                trade.setUpdateTime(now());
                tradeService.save(trade);
                /**
                 * 上游回调支付成功,增加商家余额
                 */
                Merchant merchant = merchantDao.getOne(merchantId);
                //总金额
                BigDecimal balance = merchant.getBalance();
                //可提现余额
                BigDecimal withdrawBalance = merchant.getWithdrawBalance();
                merchant.setBalance(balance.add(accountAmount));
                merchant.setWithdrawBalance(withdrawBalance.add(accountAmount));
                merchantDao.save(merchant);
                //获取商城地址营业额
                MallAddress mallAddress = mallAddressService.findById(addressId);
                BigDecimal turnover = mallAddress.getTurnover();
                //充值资金池
                BigDecimal rechargeAmount = mallAddress.getRechargeAmount();
                //冻结资金池
                BigDecimal frozenCapitalPool = mallAddress.getFrozenCapitalPool();
                //加上回调金额
                mallAddress.setTurnover(turnover.add(accountAmount));
                //充值资金池减去回调金额
                mallAddress.setRechargeAmount(rechargeAmount.subtract(accountAmount));
                //冻结资金池减去回调金额
                mallAddress.setFrozenCapitalPool(frozenCapitalPool.subtract(accountAmount));
                mallAddressService.save(mallAddress);
                try {
                    PrintWriter pw = response.getWriter();
                    pw.println("success");
                    pw.flush();
                    pw.close();
                    logger.warn("返回给商城success成功!");
                } catch (IOException e) {
                    logger.error(String.format("支付成功，通知商城服务器失败，失败原因：%s", e.getMessage()));
                }
                //交易成功，给用户发回调
                logger.warn(String.format("生成回调的订单号:%s", trade.getId()));
                JobTrade jobTrade = jobTradeService.findById(trade.getId());
                jobTrade.setExecTime(DateUtil.getTime());
                jobTrade.setContent(param);
                try {
                    logger.info(String.format("即将发起回调,回调内容为:%s", jobTrade));
                    //交易成功,给查询端口发回调
                    notifyToQuery(jobTrade);
                    notify(jobTrade);
                    logger.info("回调完毕！");

                } catch (Exception e) {
                    logger.error(String.format("首次异步回调通知异常,订单号:[%s]", jobTrade.getId()), e);
                }
                jobTradeService.save(jobTrade);

               /* //----------------分账代码------------------------------
                AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                        mallAddress.getAppid(),
                        mallAddress.getPrivatekey(),
                        "json",
                        "GBK",
                        mallAddress.getPublickey(),
                        "RSA2");
                AlipayTradeOrderSettleRequest request_fz = new AlipayTradeOrderSettleRequest();
                String out_request_no = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号
                String transIn = WebUtil.getTransIn();
                double amonut = trade.getTotalAmount().doubleValue() * mallAddress.getFzPercentage().doubleValue();
                logger.error("----------------------------");
                logger.error("appid:" + mallAddress.getAppid());
                logger.error("PrivateKey:" + mallAddress.getPrivatekey());
                logger.error("PublicKey:" + mallAddress.getPublickey());
                logger.error("-----------------------------------------");
                logger.error("trade_no:" + tradeNo);
                logger.error("trans_out:" + mallAddress.getTransOut());

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
                            "        \"trans_out\":\"" + mallAddress.getTransOut() + "\"," +//分出账户,也就是商家的账户pid
                            "\"trans_in\":\"" + transin.getPid() + "\"," +//分账金额收款账户pid
                            "\"amount\":" + (double) Math.round(amonut * 100) / 100 + "," +
                            //"\"amount_percentage\":100," +//分账百分比
                            "\"desc\":\"分账给" + transin.getPid() + "\"" +
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
                tradeService.save(trade);*/

            } else {
                logger.warn(String.format("[%s]订单信息不存在或者已经支付成功!", PayOrderNo));
            }
        }
    }

    /**
     * 给用户发的回调
     *
     * @param jobTrade 返回给用户的信息
     * @throws IOException
     * @throws SignatureException
     */
    public void notify(JobTrade jobTrade) throws IOException, SignatureException, ParseException {
        JSONObject contentJson = JSONObject.parseObject(jobTrade.getContent());
        JSONObject notifyJson = new JSONObject();
        /*//得到回调内容中的金额字符串
        String amount = contentJson.getString("total_amount");*/
        notifyJson.put("trade_status", "TRADE_SUCCESS");
        /*//取得webservice传给api的消息中的成功金额和商户订单号
        notifyJson.put("total_amount", amount);*/
        //notifyJson.put("trade_no", contentJson.getString("out_trade_no"));
        notifyJson.put("notify_id", jobTrade.getId() == null ? "" : jobTrade.getId());

        logger.info(String.format("回调方法中:trade_status[%s]", notifyJson.getString("trade_status")));
        //logger.info(String.format("回调方法中:total_amount[%s]", notifyJson.getString("total_amount")));
        //logger.info(String.format("回调方法中:trade_no[%s]", notifyJson.getString("trade_no")));
        logger.info(String.format("回调方法中:notify_id[%s]", jobTrade.getId() == null ? "" : jobTrade.getId()));
        Trade trade = tradeService.findById(contentJson.getString("out_trade_no"));
        if (trade == null) {
            logger.error("trade为null！！！");
            return;
        }
        String tradeNo = trade.getMerchantOrderNo();
        notifyJson.put("out_trade_no", tradeNo);
        //取得客户请求金额
        BigDecimal totalAmount = trade.getTotalAmount();
        //把请求金额放到回调参数中
        notifyJson.put("total_amount", totalAmount.toPlainString());
        logger.info(String.format("回调方法中:total_amount[%s]", totalAmount));
        Merchant merchant = merchantService.findById(trade.getMerchantId());
        if (merchant == null) {
            logger.error("Merchant为空！！！");
            return;
        }
        String content = SignatureUtil.getSignatureContent(notifyJson, true);
        String sign = signature.sign(content, merchant.getSecretKey(), Constants.CHARSET_UTF8);
        notifyJson.put("signature", sign);

        int status = 0;
        logger.warn("商户支付结果调度器执行：" + JSONObject.toJSONString(jobTrade));
        logger.warn("给用户发回调的地址：" + jobTrade.getNotifyUrl());
        logger.warn("给用户发回调的内容：" + notifyJson.toJSONString());

        /*Map<String, String> map = new HashMap<>(10);
        for (Map.Entry<String, Object> entry : notifyJson.entrySet()) {
            map.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        HttpClient http = new HttpClient(jobTrade.getNotifyUrl(),map);
        http.post();
        String result = http.getContent();*/

        String result = HttpsUtils.post(jobTrade.getNotifyUrl(), null, notifyJson.toJSONString());
        logger.info("商户:"+merchant.getCompany()+"----->响应的内容为:"+result);
        jobTrade.setStatus(status);
        jobTrade.setExecTime(DateUtil.getTime());
        jobTrade.setUpdateTime(DateUtil.getTime());
    }


    /**
     * 给查询端口发送已收到回调
     */
    public void notifyToQuery(JobTrade jobTrade) throws IOException, ParseException {
        String id = jobTrade.getId();
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        sb.append("out_trade_no=").append(id).append("&");
        sb.append("trade_status=").append("TRADE_SUCCESS").append("&");
        sb.append("content=").append("huixing回调确认");
        HttpClient httpClient = new HttpClient(query + sb.toString());
        httpClient.get();
        String content = httpClient.getContent();
        logger.warn("给查询发完回调以后-->响应的结果为："+content);
    }

}

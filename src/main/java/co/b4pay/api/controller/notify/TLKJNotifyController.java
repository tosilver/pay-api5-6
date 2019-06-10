package co.b4pay.api.controller.notify;

import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.dao.MerchantDao;
import co.b4pay.api.model.Channel;
import co.b4pay.api.model.JobTrade;
import co.b4pay.api.model.Merchant;
import co.b4pay.api.model.Trade;
import co.b4pay.api.service.ChannelService;
import co.b4pay.api.service.JobTradeService;
import co.b4pay.api.service.MerchantService;
import co.b4pay.api.service.TradeService;
import com.alibaba.fastjson.JSONObject;
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
import java.util.Enumeration;

import static co.b4pay.api.common.utils.DateUtil.now;

/**
 * TL快捷支付通知
 *
 * @author YK
 * @version $Id v 0.1 2018年06月06日 16:32 Exp $
 */
@RestController
@RequestMapping({"/notify/KLKJPayNotify.do"})
public class TLKJNotifyController {
    private static final Logger logger = LoggerFactory.getLogger(TLKJNotifyController.class);

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


    @RequestMapping(method = {RequestMethod.POST})
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.error("TL快捷支付回调进来了");
        JSONObject jsonObject = new JSONObject();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            jsonObject.put(parameterName, request.getParameter(parameterName));
        }
        if (jsonObject.isEmpty()) {
            return;
        }
        logger.warn("TLkj notify parms ->" + jsonObject.toJSONString());
        String cusorderid = jsonObject.getString("cusorderid");
        String amount = jsonObject.getString("trxamt");
        String trxstatus = jsonObject.getString("trxstatus");
        logger.info("返回的状态码为: ->"+trxstatus);
        BigDecimal totalAmount = new BigDecimal(amount);
        //BigDecimal totalMOney = totalAmount.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP);
        logger.info("返回的金额*100等于 ->"+totalAmount);
        logger.warn("b4订单号------------>" + cusorderid);
        Trade trade = tradeService.findById(cusorderid);
        if (trade!=null){
            String merchantOrderNo = trade.getMerchantOrderNo();
            logger.warn("商户订单号------------>" + merchantOrderNo);
            Long merchantId = trade.getMerchantId();
            Integer tradeState = trade.getTradeState();
            //到账金额
            BigDecimal accountAmount = trade.getAccountAmount();
            logger.warn("到账金额--------------------->" + accountAmount);
            Channel channel = channelService.findById(trade.getChannelId() == null ? 0 : trade.getChannelId());
            if (channel == null) {
                logger.warn(String.format("[%s]渠道信息不存在", trade.getChannelId()));
            }
            if ("0000".equals(trxstatus) && tradeState==0 && trade.getTotalAmount().compareTo(totalAmount) == 0) {
                logger.info("收款成功，改变状态为1");
                trade.setStatus(1);
                trade.setTradeState(1);
                trade.setRequestAmount(totalAmount);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                trade.setPaymentTime(sdf.format(new Date()));
                trade.setUpdateTime(now());
                tradeService.save(trade);
                /**
                 * 上游回调支付成功,增加商家余额
                 */
                Merchant merchant = merchantDao.getOne(merchantId);
                logger.warn("回调的商户是------------>" + merchant.getCompany());
                //总余额
                BigDecimal balance = merchant.getBalance();
                //入账余额
                BigDecimal accountBalance = merchant.getAccountBalance();
                logger.warn("商户总余额为:"+balance+"----入账余额:"+accountBalance+"增加的金额为:"+accountAmount);
                merchant.setBalance(balance.add(accountAmount));
                merchant.setAccountBalance(accountBalance.add(accountAmount));
                merchantDao.save(merchant);
                try {
                    PrintWriter pw = response.getWriter();
                    pw.println("success");
                    pw.flush();
                    pw.close();
                    logger.warn("返回给上游success成功!");
                } catch (IOException e) {
                    logger.error(String.format("支付成功，通知上游失败，失败原因：%s", e.getMessage()));
                }
                //交易成功，给用户发回调
                logger.warn(String.format("生成回调的订单号:%s", trade.getId()));
                JobTrade jobTrade = jobTradeService.findById(trade.getId());
                jobTrade.setExecTime(DateUtil.getTime());
                jobTrade.setContent(jsonObject.toJSONString());
                try {
                    logger.info(String.format("即将发起回调,回调内容为:%s", jobTrade));
                    notify(jobTrade);
                    logger.info("回调完毕！");
                } catch (Exception e) {
                    logger.error(String.format("首次异步回调通知异常,订单号:[%s]", jobTrade.getId()), e);
                }
                jobTradeService.save(jobTrade);
            } else {
                logger.warn(String.format("[%s]订单信息不存在或者已经支付成功!", merchantOrderNo));
            }
        }else {
            throw new BizException("服务器异常!!!");
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
        //得到回调内容中的金额字符串
        String amount = contentJson.getString("trxamt");
        //转换成BigDecimal格式
        //BigDecimal decimalAmonut = new BigDecimal(amount);
        //转换成单位为元的金额
        //BigDecimal Amonut = decimalAmonut.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP);
        //String totalAmonut = amount.toString();
        notifyJson.put("trade_status", "TRADE_SUCCESS");
        notifyJson.put("total_amount", amount);
        notifyJson.put("trade_no", contentJson.getString("cusorderid"));
        notifyJson.put("notify_id", jobTrade.getId() == null ? "" : jobTrade.getId());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //notifyJson.put("payment_time", jobTrade.getExecTime() == null ? sdf.format(new Date()) : sdf.format(jobTrade.getExecTime()));

        logger.info(String.format("回调方法中:trade_status[%s]", notifyJson.getString("trade_status")));
        logger.info(String.format("回调方法中:total_amount[%s]", notifyJson.getString("total_amount")));
        //logger.info(String.format("回调方法中:out_trade_no[%s]", notifyJson.getString("out_trade_no")));
        logger.info(String.format("回调方法中:trade_no[%s]", notifyJson.getString("trade_no")));
        logger.info(String.format("回调方法中:notify_id[%s]", jobTrade.getId() == null ? "" : jobTrade.getId()));
        //logger.info(String.format("回调方法中:payment_time[%s]", notifyJson.getString("payment_time")));
        Trade trade = tradeService.findById(contentJson.getString("cusorderid"));
        if (trade == null) {
            logger.error("trade为null！！！");
            return;
        }
        String tradeNo = trade.getMerchantOrderNo();
        notifyJson.put("out_trade_no", tradeNo);
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
        String result = HttpsUtils.post(jobTrade.getNotifyUrl(), null, notifyJson.toJSONString());
        status = "SUCCESS".equalsIgnoreCase(result) ? 1 : 0;
        logger.warn("用户发完回调以后-->执行结果：" + (status == 1 ? "成功" : "失败"));
        jobTrade.setStatus(status);
        jobTrade.setExecTime(DateUtil.getTime());
        jobTrade.setUpdateTime(DateUtil.getTime());
    }


}

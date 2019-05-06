package co.b4pay.api.controller.notify;

import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.dao.MerchantDao;
import co.b4pay.api.model.*;
import co.b4pay.api.service.*;
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

import static co.b4pay.api.common.utils.DateUtil.now;

/**
 * mall支付通知
 *
 * @author YK
 * @version $Id v 0.1 2018年06月06日 16:32 Exp $
 */
@RestController
@RequestMapping({"/notify/JHH5PayNotify.do"})
public class JHH5NotifyController {
    private static final Logger logger = LoggerFactory.getLogger(JHH5NotifyController.class);

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
        logger.error("JHH5支付回调进来了");
        String param = null;
        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }
            JSONObject jsonObject = JSONObject.parseObject(responseStrBuilder.toString());
            param = jsonObject.toJSONString();
            logger.warn("request回来的参数:" + param);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = JSONObject.parseObject(param);
        //订单号
        String tradeId = jsonObject.getString("merch_order_no");
        logger.warn("merchantOrderNo------------>" + tradeId);
        //回调内容的支付金额
        String amount = jsonObject.getString("amount");
        BigDecimal totalAmount = new BigDecimal(amount);

        //支付状态
        String tradestatus = jsonObject.getString("retcode");

        Trade trade = tradeService.findById(tradeId);
        Long merchantId = trade.getMerchantId();
        Integer tradeState = trade.getTradeState();
        //得到的是分为单位的金额
        BigDecimal requestAmount = trade.getTotalAmount();
        //转换为分为单位
        BigDecimal multiply = requestAmount.multiply(new BigDecimal(100));
        logger.warn("trade--------------------->" + trade);
        Channel channel = channelService.findById(trade.getChannelId() == null ? 0 : trade.getChannelId());
        if (channel == null) {
            logger.warn(String.format("[%s]渠道信息不存在", trade.getChannelId()));
        }
        if ("00".equalsIgnoreCase(tradestatus) && totalAmount.compareTo(multiply) == 0) {
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
                merchant.setBalance(balance.add(requestAmount));
                merchant.setWithdrawBalance(withdrawBalance.add(requestAmount));
                merchantDao.save(merchant);
                try {
                    PrintWriter pw = response.getWriter();
                    pw.println("success");
                    pw.flush();
                    pw.close();
                    logger.warn("返回给JHH5的success成功!");
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
                    notify(jobTrade);
                    logger.info("回调完毕！");

                } catch (Exception e) {
                    logger.error(String.format("首次异步回调通知异常,订单号:[%s]", jobTrade.getId()), e);
                }
                jobTradeService.save(jobTrade);
            } else {
                logger.warn(String.format("[%s]订单信息不存在或者已经支付成功!", tradeId));
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
        notifyJson.put("trade_status", "TRADE_SUCCESS");
        notifyJson.put("notify_id", jobTrade.getId() == null ? "" : jobTrade.getId());

        logger.info(String.format("回调方法中:trade_status[%s]", notifyJson.getString("trade_status")));
        logger.info(String.format("回调方法中:notify_id[%s]", jobTrade.getId() == null ? "" : jobTrade.getId()));
        Trade trade = tradeService.findById(contentJson.getString("merch_order_no"));
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
        String result = HttpsUtils.post(jobTrade.getNotifyUrl(), null, notifyJson.toJSONString());
        logger.warn("用户发完回调以后-->执行结果：" + result);
        jobTrade.setStatus(status);
        jobTrade.setExecTime(DateUtil.getTime());
        jobTrade.setUpdateTime(DateUtil.getTime());
        jobTradeService.save(jobTrade);
    }

}

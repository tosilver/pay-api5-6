package co.b4pay.api.controller.notify;

import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.tenpay.util.ServletUtil;
import co.b4pay.api.common.tosdomutils.HttpClient;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.model.*;
import co.b4pay.api.service.ChannelService;
import co.b4pay.api.service.JobTradeService;
import co.b4pay.api.service.MerchantService;
import co.b4pay.api.service.TradeService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mr.constant.RetCdConstant;
import com.mr.constant.ValueConstant;
import com.mr.model.OrgBaseMsgReq;
import com.mr.model.OrgBaseMsgRsp;
import com.mr.model.OrgPayNotice;
import com.mr.model.OrgPayQryReq;
import com.mr.model.enums.TxnTypeEnum;
import com.mr.util.HttpClientUtil;
import com.mr.util.MerchantUtil;
import com.mr.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static co.b4pay.api.common.utils.DateUtil.now;
import static co.b4pay.api.common.utils.DateUtil.yMdhms;

/**
 * 支付宝扫码支付通知
 *
 * @author YK
 * @version $Id v 0.1 2018年06月06日 16:32 Exp $
 */
@RestController
@RequestMapping({"/notify/grPayNotify.do"})
public class GRPayNotifyController {
    private static final Logger logger = LoggerFactory.getLogger(GRPayNotifyController.class);

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


    @RequestMapping(method = {RequestMethod.GET})
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.error("个码支付回调进来了");
        System.out.println("个码支付结果通用通知：" + ServletUtil.getQueryString(request));
        JSONObject rollJson = new JSONObject();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            rollJson.put(parameterName, request.getParameter(parameterName));
        }

        if (rollJson.isEmpty()) {
            logger.warn("grPay notify parms ->为空");
            return;
        }
        logger.warn("grPay notify parms ->" + rollJson.toJSONString());
        String orderid = rollJson.getString("orderid");
        if (!StringUtils.isEmpty(orderid)) {
            Trade trade = tradeService.findByMerchantOrderNo(orderid);
            if (trade != null) {
                logger.info("收款成功，改变状态为1");
                trade.setStatus(1);
                trade.setTradeState(1);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                trade.setPaymentTime(sdf.format(new Date()));
                trade.setUpdateTime(now());
                tradeService.save(trade);

                try {
                    PrintWriter pw = response.getWriter();
                    pw.println("success");
                    pw.flush();
                    pw.close();
                } catch (IOException e) {
                    logger.error(String.format("支付成功，通知个码服务器失败，失败原因：%s", e.getMessage()));
                }

                //交易成功，给用户发回调
                logger.warn(String.format("生成回调的订单号:%s", trade.getId()));
                JobTrade jobTrade = jobTradeService.findById(trade.getId());
                jobTrade.setExecTime(DateUtil.getTime());
                jobTrade.setContent(rollJson.toJSONString());
                try {
                    logger.info(String.format("即将发起回调,回调内容为:%s", jobTrade));
                    notify(jobTrade);
                    logger.info("回调完毕！");
                } catch (Exception e) {
                    logger.error(String.format("首次异步回调通知异常,订单号:[%s]", jobTrade.getId()), e);
                }
                jobTradeService.save(jobTrade);
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
        //取得webservice传给api的消息中的成功金额和商户订单号
        notifyJson.put("total_amount", contentJson.getString("money"));
        notifyJson.put("out_trade_no", contentJson.getString("orderid"));
        notifyJson.put("trade_no", "");
        notifyJson.put("notify_id", jobTrade.getId() == null ? "" : jobTrade.getId());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        notifyJson.put("payment_time", jobTrade.getExecTime() == null ? sdf.format(new Date()) : sdf.format(jobTrade.getExecTime()));

        logger.info(String.format("回调方法中:trade_status[%s]", notifyJson.getString("trade_status")));
        logger.info(String.format("回调方法中:total_amount[%s]", notifyJson.getString("total_amount")));
        logger.info(String.format("回调方法中:out_trade_no[%s]", notifyJson.getString("out_trade_no")));
        logger.info(String.format("回调方法中:trade_no[%s]", notifyJson.getString("trade_no")));
        logger.info(String.format("回调方法中:notify_id[%s]", jobTrade.getId() == null ? "" : jobTrade.getId()));
        logger.info(String.format("回调方法中:payment_time[%s]", notifyJson.getString("payment_time")));
        Trade trade = tradeService.findByMerchantOrderNo(contentJson.getString("orderid"));
        if (trade == null) {
            logger.error("trade为null！！！");
            return;
        }
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
        status = Constants.SUCCESS.equals(result) ? 1 : 0;
        logger.warn("用户发完回调以后-->执行结果：" + (status == 1 ? "成功" : "失败"));
        jobTrade.setStatus(status);
        jobTrade.setExecTime(DateUtil.getTime());
        jobTrade.setUpdateTime(DateUtil.getTime());
    }


}

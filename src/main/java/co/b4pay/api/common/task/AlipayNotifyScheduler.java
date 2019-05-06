package co.b4pay.api.common.task;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.dao.JobTradeDao;
import co.b4pay.api.dao.MerchantDao;
import co.b4pay.api.dao.TradeDao;
import co.b4pay.api.model.JobTrade;
import co.b4pay.api.model.Merchant;
import co.b4pay.api.model.Trade;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

@Component
public class AlipayNotifyScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AlipayNotifyScheduler.class);

    private String[] jobIntervalTime = MainConfig.jobIntervalTime.split(",");

    @Autowired
    private HmacSHA1Signature signature = new HmacSHA1Signature();

    @Autowired
    private JobTradeDao jobTradeDao;

    @Autowired
    private TradeDao tradeDao;

    @Autowired
    private MerchantDao merchantDao;


    @Scheduled(fixedDelay = 3 * 1000) // 单位毫秒
    public void runJob() {
        //System.out.println("AlipayNotifyScheduler间隔调度:::" + Instant.now());
        List<JobTrade> jobTradeList = jobTradeDao.findByChannelTypeAndStatusAndExecTimeLessThanEquals(ChannelType.ALIPAY, 0, DateUtil.getTime());
        if (CollectionUtils.isEmpty(jobTradeList)) {
            return;
        }
        Iterator<JobTrade> jobTradeIterator = jobTradeList.iterator();
        while (jobTradeIterator.hasNext()) {
            JobTrade jobTrade = jobTradeIterator.next();
            try {
                JSONObject contentJson = JSONObject.parseObject(jobTrade.getContent());
                JSONObject notifyJson = new JSONObject();
                notifyJson.put("trade_status", contentJson.getString("trade_status"));
                notifyJson.put("total_amount", contentJson.getString("total_amount"));
                notifyJson.put("out_trade_no", contentJson.getString("out_trade_no"));
                notifyJson.put("trade_no", contentJson.getString("trade_no"));
                notifyJson.put("notify_id", contentJson.getString("notify_id"));
                notifyJson.put("payment_time", contentJson.getString("gmt_payment"));

                Trade trade = tradeDao.findByMerchantOrderNo(contentJson.getString("out_trade_no"));
                if (trade == null) {
                    return;
                }
                Merchant merchant = merchantDao.findById(trade.getMerchantId()).orElse(null);
                if (merchant == null) {
                    return;
                }

                String content = SignatureUtil.getSignatureContent(notifyJson, true);
                String sign = signature.sign(content, merchant.getSecretKey(), Constants.CHARSET_UTF8);
                notifyJson.put("signature", sign);

                int status = 0;
                try {
                    logger.warn("支付宝支付结果调度器执行：" + JSONObject.toJSONString(jobTrade));
                    String result = HttpsUtils.post(jobTrade.getNotifyUrl(), null, notifyJson.toJSONString());
                    System.out.println("支付宝回调:" + jobTrade.getId());
                    status = Constants.SUCCESS.equals(result) ? 1 : 0;
                } catch (IOException e) {
                    logger.error(String.format("异步回调通知异常,订单号:[%s]", jobTrade.getId()), e);
                }

                if (jobTrade.getCount() < this.jobIntervalTime.length) {
                    int jobIntervalTime = Integer.parseInt(this.jobIntervalTime[jobTrade.getCount()]);
                    jobTrade.setStatus(status);
                    jobTrade.setCount(jobTrade.getCount() + 1);
                    jobTrade.setExecTime(DateUtil.add(DateUtil.getTime(), Calendar.MINUTE, jobIntervalTime));
                } else {
                    jobTrade.setStatus(-1); // 超过重复次数，状态设置为-1不再通知
                }
                jobTrade.setUpdateTime(DateUtil.getTime());
                jobTradeDao.save(jobTrade);
            } catch (Exception e) {
                logger.error(String.format("异步回调通知异常,订单号:[%s]", jobTrade.getId()), e);
            }
        }
    }
}

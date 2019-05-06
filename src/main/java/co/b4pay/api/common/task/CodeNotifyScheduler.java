package co.b4pay.api.common.task;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.dao.JobTradeDao;
import co.b4pay.api.dao.MerchantDao;
import co.b4pay.api.dao.TradeDao;
import co.b4pay.api.model.JobTrade;
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
public class CodeNotifyScheduler {
    private static final Logger logger = LoggerFactory.getLogger(CodeNotifyScheduler.class);

    private String[] jobIntervalTime = MainConfig.jobPartitionTime.split(",");

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
        List<JobTrade> jobTradeList = jobTradeDao.findByChannelTypeAndStatusAndExecTimeLessThanEquals(ChannelType.QRCODE, 0, DateUtil.getTime());
        if (CollectionUtils.isEmpty(jobTradeList)) {
            return;
        }
        Iterator<JobTrade> jobTradeIterator = jobTradeList.iterator();
        while (jobTradeIterator.hasNext()) {
            JobTrade jobTrade = jobTradeIterator.next();
            try {
                int status = 0;
                try {
                    String result = HttpsUtils.post(jobTrade.getNotifyUrl(), null, jobTrade.getContent());
                    status = "SUCCESS".equals(result) ? 1 : 0;
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

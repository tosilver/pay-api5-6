package co.b4pay.api.common.task;

import co.b4pay.api.dao.MallAddressDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WipeDateScheduler {
    private static final Logger logger = LoggerFactory.getLogger(WipeDateScheduler.class);

    @Autowired
    private MallAddressDao mallAddressDao;

    @Scheduled(cron = "0 0 0 * * ?")
    public void wipeDate(){
        logger.info("0点清空当天营业额定时任务开始!");
        BigDecimal bigDecimal = new BigDecimal("0.00");
        mallAddressDao.updateTurnover(bigDecimal);
        logger.info("更新营业额成功");
    }

}

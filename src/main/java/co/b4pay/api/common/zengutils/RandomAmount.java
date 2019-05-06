package co.b4pay.api.common.zengutils;

import co.b4pay.api.service.MallPayTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class RandomAmount {

    private static final Logger logger = LoggerFactory.getLogger(RandomAmount.class);


    /**
     * 客户请求金额减掉千分之一内的数值
     *
     * @param money
     * @return
     */

    public static BigDecimal randomAmount(BigDecimal money) {
        BigDecimal num = money.divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);
        logger.info("千分之一金额为" + num);
        //把请求的金额除于10元,得到一个可以随机的范围
        BigDecimal randomNum = money.divide(new BigDecimal(10), 2, RoundingMode.HALF_UP);
        //把范围转为int类型
        int i = randomNum.intValue();
        //从范围内取出一个随机数int类型
        Random random = new Random();
        int nextInt = random.nextInt(i);
        //随机数转换为金额
        BigDecimal randomBigDecimal = new BigDecimal(nextInt);
        BigDecimal randomMoney = randomBigDecimal.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        logger.info("随机金额:" + randomMoney);
        if (randomMoney.compareTo(num) <= 0) {
            //请求金额减去随机金额
            logger.info("从请求金额减去随机金额!");
            BigDecimal endTotalMoney = money.subtract(randomMoney);
            //得到最终请求金额
            //BigDecimal preciseEndMoney = endMoney.setScale(2, RoundingMode.HALF_UP);
            //BigDecimal  endTotalMoney= convert.multiply(new BigDecimal(100));
            logger.info("最终的请求金额是:" + endTotalMoney);
            return endTotalMoney;
        } else {
            logger.info("随机金额太大了!!!");
            return money;
        }
    }
}

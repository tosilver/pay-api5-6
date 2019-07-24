package co.b4pay.api.common.listener;


import co.b4pay.api.model.Trade;
import co.b4pay.api.service.RedisListenerService;
import co.b4pay.api.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * 监听所有db的过期事件__keyevent@*__:expired"
 */

@Component
public class RedisKeyExpirationListener  extends KeyExpirationEventMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisKeyExpirationListener.class);

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }


    @Autowired
    private TradeService tradeService;

    @Autowired
    private RedisListenerService redisListenerService;

    /**
     * 针对redis数据失效事件，进行数据处理
     * @param message
     * @param pattern
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 用户做自己的业务处理即可,注意message.toString()可以获取失效的key

        logger.info("==============[Redis 的 失效通知回来了]==================");
        String expiredKey = message.toString();
        logger.info("===============[新消息为----->:"+expiredKey+"]=================");
        if(expiredKey.startsWith("Order:")){
            //如果是Order:开头的key，进行处理
            String[] split = expiredKey.split(":");
            String tradeNo=split[1];
            logger.info("==================需要资金回滚的订单为:"+tradeNo+"=============");
            System.out.println(tradeNo);
            //获取订单数据
            Trade trade = tradeService.findByMerchantOrderNo(tradeNo);
            if (trade != null){
                logger.info("===================数据库查询订单存在=====================");
                logger.info("===================执行资金回滚操作=====================");
                boolean b = redisListenerService.rollbackTradeMoneys(trade);
                if (b){
                    logger.info("+++++++++++++++执行资金回滚操作成功+++++++++++++++++++++");
                }else {
                    logger.info("---------------执行资金回滚操作成功---------------------");
                }
            }else {
                logger.info("***************数据库查询订单不存在********************");

            }
        }
    }
}

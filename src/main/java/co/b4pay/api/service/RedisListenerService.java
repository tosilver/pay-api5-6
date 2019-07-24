package co.b4pay.api.service;


import co.b4pay.api.model.Channel;
import co.b4pay.api.model.DisabledTrade;
import co.b4pay.api.model.Merchant;
import co.b4pay.api.model.Trade;
import com.alibaba.druid.sql.visitor.functions.Now;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
@Transactional(rollbackFor = Exception.class)
public class RedisListenerService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(RedisListenerService.class);


    /**
     * 针对redis 失效数据通知后续业务回滚未确认收款订单金额
     */
    public boolean rollbackTradeMoneys(Trade trade){
        //获取订单状态
        Integer tradeState = trade.getTradeState();
        //如果订单为未支付
        if (tradeState==0){
            logger.info("订单为未支付状态!");
            //获取订单的请求金额
            BigDecimal totalAmount = trade.getTotalAmount();
            logger.info("订单的请求金额为"+totalAmount);
            //获取订单通道id
            Long channelId = trade.getChannelId();
            //根据通道id查找通道信息
            Channel one = channelDao.getOne(channelId);
            //获取通道的剩余额度
            BigDecimal amountLimit = one.getAmountLimit();
            logger.info("通道["+one.getName()+"]的剩余额度为"+amountLimit);
            BigDecimal add = amountLimit.add(totalAmount);
            logger.info("["+amountLimit+"]+["+totalAmount+"]=["+add+"]");
            one.setAmountLimit(add);
            channelDao.save(one);
            //关闭订单
            trade.setTradeState(-2);
            trade.setUpdateTime(new Date());
            tradeDao.save(trade);
            logger.info("关闭订单:"+trade.getMerchantOrderNo() +"=====>成功<====");
            logger.info("==开始生成一条关闭订单的记录");
            DisabledTrade disabledTrade = new DisabledTrade();
            disabledTrade.setMerchantOrderNo(trade.getMerchantOrderNo());
            disabledTrade.setChannelId(channelId);
            disabledTrade.setChannelName(one.getName());
            Long merchantId = trade.getMerchantId();
            Merchant merchant = merchantDao.getOne(merchantId);
            disabledTrade.setMerchantId(merchantId);
            disabledTrade.setMerchantCompany(merchant.getCompany());
            disabledTrade.setAmount(totalAmount);
            disabledTrade.setCreateTime(trade.getCreateTime());
            disabledTrade.setCloseTime(new Date());
            disabledTrade.setChannelInitAmount(amountLimit);
            disabledTrade.setChannelAlterAmount(add);
            disabledTrade.setStatus(0);
            disabledTradeDao.save(disabledTrade);
            logger.info("生成关闭订单记录===>成功");
            return true;
        }else {
            logger.info("订单已被支付!");
            return false;
        }
    }


}

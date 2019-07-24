package co.b4pay.api.service;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.dao.*;
import co.b4pay.api.model.Channel;
import co.b4pay.api.model.Router;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static co.b4pay.api.common.utils.DateUtil.now;

public class BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(BasePayService.class);

    @Autowired
    protected ChannelDao channelDao;

    @Autowired
    protected TradeDao tradeDao;

    @Autowired
    protected JobTradeDao jobTradeDao;

    @Autowired
    protected JobTradeService jobTradeService;

    @Autowired
    protected MerchantRateDao merchantRateDao;

    @Autowired
    protected MerchantDao merchantDao;

    @Autowired
    protected MerchantService merchantService;

    @Autowired
    protected GoodsDao goodsDao;

    @Autowired
    protected YEDFPayrollDao yedfPayrollDao;

    @Autowired
    protected MallAddressDao mallAddressDao;

    @Autowired
    protected MallAccessControlDao mallAccessControlDao;

    @Autowired
    protected MallCheckOutService mallCheckOutService;

    @Autowired
    protected QRChannelDao qrChannelDao;

    @Autowired
    protected QRCodeDao qrCodeDao;

    @Autowired
    protected QRCheckOutService qrCheckOutService;

    @Autowired
    protected BankCradDao bankCradDao;

    @Autowired
    protected KJAgreeapplyDao kjAgreeapplyDao;


    @Autowired
    protected FrozenCapitalTradeDao frozenCapitalTradeDao;


    @Autowired
    protected RedisTemplate<String, String> redisTemplate;

    @Autowired
    protected DisabledTradeDao disabledTradeDao;



    protected synchronized Channel getChannel(Long merchantId, Router router, BigDecimal totalAmount, String ip4) {
        List<Channel> channelList = channelDao.findByRouterIdAndStatus(router.getId(), 1, ip4);
        if (CollectionUtils.isEmpty(channelList)) {
            throw new BizException(String.format("[%s, %s]暂无可用通道", merchantId, router.getId()));
        }

        Date now = Calendar.getInstance().getTime();

        Channel channel = null;
        Iterator<Channel> channelIterator = channelList.iterator();
        while (channelIterator.hasNext()) { // 轮训通道
            Channel _channel = channelIterator.next();
            if (_channel.getResetTime().before(now)) {
                _channel.setAmountLimit(_channel.getAmountInit());
                _channel.setResetTime(DateUtils.addHours(now, 24));
                channelDao.save(_channel);
            }
            if (_channel.getAmountLimit().compareTo(totalAmount) > 0) {
                channel = _channel;
                _channel.setUpdateTime(now());
                channelDao.save(_channel);
                break;
            }
        }
        if (channel == null) {
            throw new BizException(String.format("[%s, %s]商户暂无可用通道", merchantId, router.getId()));
        }
        return channel;
    }

    protected synchronized Channel getChannel(Long merchantId, Router router, BigDecimal totalAmount) {
        logger.info("进入校验通道方法:");
        //根据路由id,和状态查找通道集合
        List<Channel> channelList = channelDao.findByRouterIdAndStatus(router.getId(), 1);
        //判断集合是否为空
        if (CollectionUtils.isEmpty(channelList)) {
            //如果通道集合为空,直接抛无可用通道异常
            throw new BizException(String.format("[%s, %s]暂无可用通道", merchantId, router.getId()));
        }
        Channel channel = null;

        Iterator<Channel> channelIterator = channelList.iterator();
        while (channelIterator.hasNext()) { // 轮训通道
            Channel _channel = channelIterator.next();
            logger.info("轮询的通道名称为:"+_channel.getName());
            //需要通道满足如下条件
            //1.通道剩余额度>请求金额+通道最低额度
            //2.请求的时间>最后请求时间+通道速率
            if (_channel.getAmountLimit().compareTo(totalAmount.add(_channel.getAmountMin() == null ? BigDecimal.ZERO : _channel.getAmountMin())) > 0
                    && (_channel.getLastSuccessTime() == null || now().after(DateUtils.addSeconds(_channel.getLastSuccessTime(), _channel.getRate().intValue())))) {
                _channel.setUpdateTime(now());
                channelDao.save(_channel);
                channel = _channel;
                logger.info("轮询通过的通道为:"+channel.getName());
                break;
            }
        }
        if (channel == null) {
            throw new BizException(String.format("[%s, %s]商户暂无可用通道", merchantId, router.getId()));
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return channel;

    }


    protected Channel getChannel(String channelName, String routerId) {
        Channel channel = channelDao.findByRouterAndName(routerId, channelName);
        if (channel == null) {
            channel = new Channel();
            channel.setId(0L);
        }
        return channel;
    }

    public static void main(String[] args) {
        BigDecimal b = new BigDecimal("1.001");
        System.out.println(b.intValue());
        Date date = now();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(now().after(DateUtils.addSeconds(date, b.intValue())));
    }

}

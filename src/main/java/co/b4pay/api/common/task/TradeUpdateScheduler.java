package co.b4pay.api.common.task;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.dao.TradeDao;
import co.b4pay.api.model.Channel;
import co.b4pay.api.model.JobTrade;
import co.b4pay.api.model.Merchant;
import co.b4pay.api.model.Trade;
import co.b4pay.api.service.ChannelService;
import co.b4pay.api.service.JobTradeService;
import co.b4pay.api.service.MerchantService;
import co.b4pay.api.socket.SocketUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static co.b4pay.api.common.utils.DateUtil.now;
import static co.b4pay.api.socket.SocketConstants.SOCKET_MAP;

@Component
public class TradeUpdateScheduler {
    private static final Logger logger = LoggerFactory.getLogger(TradeUpdateScheduler.class);

    @Autowired
    private TradeDao tradeDao;

    @Autowired
    private HmacSHA1Signature signature = new HmacSHA1Signature();

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private JobTradeService jobTradeService;


    @Scheduled(cron = "* */1 * * * ?") // 每1分钟
    public void runJob() {
        List<Trade> tradeList = tradeDao.findAllUnPaidTrade();
        if (CollectionUtils.isEmpty(tradeList)) {
            return;
        }
        Iterator<Trade> tradeIterator = tradeList.iterator();
        while (tradeIterator.hasNext()) {
            Trade trade = tradeIterator.next();
            Long now = new Date().getTime();
            Long createTime = trade.getCreateTime().getTime();
            if (now - createTime >= Long.parseLong(MainConfig.EXPIRY_TIME)) {
                trade.setTradeState(-2);
                tradeDao.save(trade);
                //更新渠道的最后失败时间
                Channel channel = channelService.findById(trade.getChannelId());
                if (channel != null && "unFixedCodePay".equals(channel.getRouter().getId())) {
                    channel.setLastFailTime(now());
                    channelService.save(channel);
                }
                Merchant merchant = merchantService.findById(trade.getMerchantId());
                if (merchant == null) {
                    logger.warn(String.format("[%s]商户号不存在", trade.getMerchantId()));
                }
                try {
                    if (merchant != null) {
                        //更新调度类
                        JSONObject returnData = new JSONObject();
                        returnData.put("tradeNo", trade.getMerchantOrderNo());
                        returnData.put("amount", trade.getTotalAmount().toPlainString());
                        returnData.put("tradeState", String.valueOf(trade.getTradeState()));
                        returnData.put("merchantId", String.valueOf(trade.getMerchantId()));
                        returnData.put("payTime", String.valueOf(trade.getUpdateTime().getTime()));
                        String content = SignatureUtil.getSignatureContent(returnData, true);
                        String sign = signature.sign(content, merchant.getSecretKey(), Constants.CHARSET_UTF8);
                        returnData.put("signature", sign);
                        //更新调度类
                        JobTrade jobTrade = jobTradeService.findById(trade.getId());
                        if (jobTrade != null) {
                            jobTrade.setStatus(0);
                            jobTrade.setExecTime(DateUtil.getTime());
                            jobTrade.setContent(returnData.toJSONString());
                            jobTradeService.save(jobTrade);
                        }
                    }
                } catch (Exception e) {
                    logger.error("通知下游失败： " + e.getMessage());
                }
                //通知apk
                logger.warn(String.format("通知手机端订单关闭,socket个数:%s ", SOCKET_MAP.size()));
                if (SOCKET_MAP.size() > 0) {
                    Iterator<Map.Entry<SocketAddress, Socket>> socketIterator = SOCKET_MAP.entrySet().iterator();
                    while (socketIterator.hasNext()) {
                        Socket socket = new Socket();
                        //下发给apk
                        try {
                            Map.Entry<SocketAddress, Socket> socketEntries = socketIterator.next();
                            socket = socketEntries.getValue();
                            SocketUtil socketUtil = new SocketUtil(socket);
                            JSONObject returnJson = new JSONObject();
                            returnJson.put("code", channel.getName() + "003");
                            JSONObject tradeJSON = new JSONObject();
                            tradeJSON.put("tradeNo", trade.getMerchantOrderNo());
                            tradeJSON.put("amount", trade.getTotalAmount());
                            tradeJSON.put("merchantId", trade.getMerchantId());
                            tradeJSON.put("phoneCode", channel.getName());
                            tradeJSON.put("tradeStatus", trade.getTradeState());
                            tradeJSON.put("createTime", trade.getCreateTime().getTime());
                            tradeJSON.put("payWay", channel.getProduct());
                            tradeJSON.put("qrType", channel.getRouter().getId());
                            returnJson.put("data", tradeJSON);
                            socketUtil.sendData(returnJson.toJSONString() + "*");
                        } catch (IOException e) {
                            logger.warn("订单关闭，下发给apk失败：" + e.getMessage());
                            try {
                                socketIterator.remove();
                                socket.close();
                            } catch (IOException e1) {
                                logger.warn("关闭socket失败：" + e.getMessage());
                            }
                        }
                    }
                } else {
                    logger.warn("订单关闭，无socket端口，下发给apk失败。");
                }
            }
        }
    }
}

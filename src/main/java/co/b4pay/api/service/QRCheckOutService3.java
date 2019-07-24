package co.b4pay.api.service;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.zengutils.TradeIdUtlis;
import co.b4pay.api.model.FrozenCapitalTrade;
import co.b4pay.api.model.QRChannel;
import co.b4pay.api.model.qrcode;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import static co.b4pay.api.common.utils.DateUtil.now;

@Service
public class QRCheckOutService3 extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(QRCheckOutService.class);


    /**
     * 从所属地址拿出符合请求条件的地址
     */
    public qrcode checkout(List<QRChannel> qrChannelList, BigDecimal totalMOney, int payType, Long merchantId, HttpServletRequest request,String outTradeNo) {
        logger.info("校验开始:");
        qrcode qrcode = null;
        /*for (int i = 0; i <= qrChannelList.size(); i++) {
            //轮询开始
            Subject subject = SecurityUtils.getSubject();
            Session session = subject.getSession(true);
            if (session == null) {
                session.setAttribute("lunxun", 0);
                //session.setAttribute("qrlunxun",0);
            }
            Integer lunxun = (Integer) session.getAttribute("lunxun");
            if (lunxun == null || lunxun >= qrChannelList.size()) {
                lunxun = 0;
            }
            i = lunxun;
            QRChannel qrChannel = qrChannelList.get(i);
            logger.info("校验的通道为:" + qrChannel.getName());
            //添加校验:控制二维码通道的访问速率
            if (qrChannel != null && (qrChannel.getLastRequestTime() == null || now().after(DateUtils.addSeconds(qrChannel.getLastRequestTime(), qrChannel.getRate().intValue())))) {
                //得到二维码通道的充值金额
                BigDecimal rechargeAmount = qrChannel.getRechargeAmount();
                //得到二维码通道的冻结资金额
                BigDecimal frozenCapitalPool = qrChannel.getFrozenCapitalPool();
                //防止充值资金池溢出,要求充值资金池不能少于1000元
                //BigDecimal actualAmount = rechargeAmount.multiply(new BigDecimal(1000));
                BigDecimal actualAmount = new BigDecimal(1000.00);
                logger.info("资金池----->" + qrChannel.getName() + "限制金额为:" + actualAmount);
                int status = actualAmount.compareTo(rechargeAmount.subtract(totalMOney));
                if (status <= 0) {
                    logger.info("二维码通道:" + qrChannel.getName() + "校验成功!");
                    session.setAttribute("lunxun", lunxun + 1);
                    //得到校验成功的二维码通道的商户id
                    Long qrMerchantId = qrChannel.getMerchantId();
                    //得到对应商户id,支付类型,金额,状态为开始的支付二维码集合
                    List<qrcode> qrcodeList = qrCodeDao.findBymerchantIdAndStatusAndCodeTypeAndMoney(qrMerchantId, 1, payType, totalMOney);
                    logger.info("得到" + qrChannel.getName() + "符合条件的二维码有" + qrcodeList.size() + "个");
                    //创富金行的商户ID为
                    Long[] cf ={100000000000068L,100000000000093L};
                    if (qrcodeList.size() == 0 && !cf[0].equals(merchantId) && !cf[1].equals(merchantId)) {
                        logger.info("调用任意额度码");
                        qrcodeList = qrCodeDao.findBymerchantIdAndStatusAndCodeTypeAndMoney(qrMerchantId, 1, payType, new BigDecimal(0));
                        logger.info("任意额度码有" + qrcodeList.size() + "个");
                        if (qrcodeList.size() != 0){
                            Random random = new Random();
                            for (int j = 0; j < qrcodeList.size(); j++) {
                                //修改为随机抽取二维码,不要轮询
                                j = random.nextInt(qrcodeList.size());
                                logger.info("j----------->" + j);
                                qrcode = qrcodeList.get(j);
                                logger.info("校验的二维码是" + qrcode.getName());
                                if (qrcode != null && (qrcode.getLastRequestTime() == null || now().after(DateUtils.addSeconds(qrcode.getLastRequestTime(), qrcode.getRate().intValue())))) {
                                    String codeData = qrcode.getCodeData();
                                    if (codeData != null) {
                                        logger.info("校验通过的二维码是" + qrcode.getName());
                                        qrcode.setLastRequestTime(now());
                                        qrcode.setTurnover(totalMOney);
                                        qrCodeDao.save(qrcode);
                                        //把请求金额从充值资金池减去然后加进冻结资金池
                                        BigDecimal subtract = rechargeAmount.subtract(totalMOney);
                                        logger.info("充值资金池:----->" + rechargeAmount + "请求金额为:------->" + totalMOney + "相减等于:----->" + subtract);
                                        BigDecimal add = frozenCapitalPool.add(totalMOney);
                                        logger.info("冻结资金池:----->" + frozenCapitalPool + "请求金额为:------->" + totalMOney + "相加等于:----->" + add);
                                        qrChannel.setRechargeAmount(subtract);
                                        qrChannel.setFrozenCapitalPool(add);
                                        qrChannel.setLastRequestTime(now());
                                        qrChannelDao.save(qrChannel);
                                        //创建冻结资金流水
                                        FrozenCapitalTrade frozenCapitalTrade = new FrozenCapitalTrade();
                                        TradeIdUtlis tradeIdUtlis = new TradeIdUtlis();
                                        frozenCapitalTrade.setId(Long.valueOf(tradeIdUtlis.getOrderIdByUUId()));
                                        frozenCapitalTrade.setFrozenCapitalPoolId(qrChannel.getId());
                                        frozenCapitalTrade.setOutTradeNo(outTradeNo);
                                        frozenCapitalTrade.setStatus(1);
                                        frozenCapitalTrade.setMoney(totalMOney);
                                        frozenCapitalTrade.setFrozenCapitalStatus(1);
                                        frozenCapitalTradeDao.save(frozenCapitalTrade);
                                        return qrcode;
                                    }
                                } else {
                                    //如果刚好随机抽取到的二维码不符合规则.则j再随机一次
                                    j = random.nextInt(qrcodeList.size());
                                    logger.info("j------------------>" + j);
                                    //session.setAttribute("qrlunxun",qrlunxun+1 );
                                    *//*throw new BizException(String.format("暂无可用通道,稍等一会再试!"));*//*
                                }
                            }
                        }else {
                            session.setAttribute("lunxun",lunxun + 1);
                        }
                    }else {
                        if (qrcodeList.size() != 0){
                            Random random = new Random();
                            for (int j = 0; j < qrcodeList.size(); j++) {
                                //修改为随机抽取二维码,不要轮询
                                j = random.nextInt(qrcodeList.size());
                                logger.info("j----------->" + j);
                                qrcode = qrcodeList.get(j);
                                logger.info("校验的二维码是" + qrcode.getName());
                                if (qrcode != null && (qrcode.getLastRequestTime() == null || now().after(DateUtils.addSeconds(qrcode.getLastRequestTime(), qrcode.getRate().intValue())))) {
                                    String codeData = qrcode.getCodeData();
                                    if (codeData != null) {
                                        logger.info("校验通过的二维码是" + qrcode.getName());
                                        qrcode.setLastRequestTime(now());
                                        qrcode.setTurnover(totalMOney);
                                        qrCodeDao.save(qrcode);
                                        //把请求金额从充值资金池减去然后加进冻结资金池
                                        BigDecimal subtract = rechargeAmount.subtract(totalMOney);
                                        logger.info("充值资金池:----->" + rechargeAmount + "请求金额为:------->" + totalMOney + "相减等于:----->" + subtract);
                                        BigDecimal add = frozenCapitalPool.add(totalMOney);
                                        logger.info("冻结资金池:----->" + frozenCapitalPool + "请求金额为:------->" + totalMOney + "相加等于:----->" + add);
                                        qrChannel.setRechargeAmount(subtract);
                                        qrChannel.setFrozenCapitalPool(add);
                                        qrChannel.setLastRequestTime(now());
                                        qrChannelDao.save(qrChannel);
                                        //创建冻结资金流水
                                        FrozenCapitalTrade frozenCapitalTrade = new FrozenCapitalTrade();
                                        TradeIdUtlis tradeIdUtlis = new TradeIdUtlis();
                                        frozenCapitalTrade.setId(Long.valueOf(tradeIdUtlis.getOrderIdByUUId()));
                                        frozenCapitalTrade.setFrozenCapitalPoolId(qrChannel.getId());
                                        frozenCapitalTrade.setOutTradeNo(outTradeNo);
                                        frozenCapitalTrade.setStatus(1);
                                        frozenCapitalTrade.setMoney(totalMOney);
                                        frozenCapitalTrade.setFrozenCapitalStatus(1);
                                        frozenCapitalTradeDao.save(frozenCapitalTrade);
                                        return qrcode;
                                    }
                                } else {
                                    //如果刚好随机抽取到的二维码不符合规则.则j再随机一次
                                    j = random.nextInt(qrcodeList.size());
                                    logger.info("j------------------>" + j);
                                    //session.setAttribute("qrlunxun",qrlunxun+1 );
                                    *//*throw new BizException(String.format("暂无可用通道,稍等一会再试!"));*//*
                                }
                            }
                        }else {
                            session.setAttribute("lunxun",lunxun + 1);
                            throw new BizException(String.format("暂无可用通道,稍等一会再试!"));
                        }
                    }
                } else {
                    session.setAttribute("lunxun", lunxun + 1);
                    throw new BizException(String.format("暂无可用通道,稍等一会再试!"));
                }
            } else {
                session.setAttribute("lunxun", lunxun + 1);
                *//*throw new BizException(String.format("暂无可用通道,稍等一会再试!"));*//*
            }
        }*/
        //先检验二维码通道集合长度
        int size = qrChannelList.size();
        if (size != 0){
            //开始轮询通道
            for (int i = 0; i <=qrChannelList.size(); i++) {
                //轮询开始
                Subject subject = SecurityUtils.getSubject();
                Session session = subject.getSession(true);
                if (session == null) {
                    session.setAttribute("lunxun", 0);
                    //session.setAttribute("qrlunxun",0);
                }
                Integer lunxun = (Integer) session.getAttribute("lunxun");
                logger.info("当前的轮询数字为:"+lunxun);
                if (lunxun == null || lunxun >= qrChannelList.size()) {
                    lunxun = 0;
                }
                i = lunxun;
                QRChannel qrChannel = qrChannelList.get(i);
                logger.info("校验的通道为:" + qrChannel.getName());
                //添加校验:控制二维码通道的访问速率
                boolean b = chenckRata(qrChannel);
                if (b){

                }else {

                }


            }
        }else {
            throw new BizException(String.format("暂无可用通道,稍等一会再试!"));
        }


        return qrcode;
    }



    public boolean chenckRata(QRChannel qrChannel){
        if (qrChannel != null && (qrChannel.getLastRequestTime() == null || now().after(DateUtils.addSeconds(qrChannel.getLastRequestTime(), qrChannel.getRate().intValue())))) {
            return true;
        }else {
            return false;
        }
    }





/*
    *//**
     * 订单计时器
     * (请求时触发此计时器,10分钟后执行把未支付的订单,把请求的金额从冻结资金池中减去,重新注入充值资金池)
     *
     * @param outTradeNo
     *//*
    public void timer1(String outTradeNo) {
        Timer ntimer = new Timer();
        ntimer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("计时器开始执行---------->");
                //根据客户订单号查询订单信息
                Trade trade = tradeDao.findByMerchantOrderNo(outTradeNo);
                if (trade != null) {
                    //得到订单创建时间
                    Date createTime = trade.getCreateTime();
                    logger.info("当前时间:" + format.format(new Date()));
                    logger.info("订单创建时间:" + createTime);
                    //得到订单状态
                    Integer tradeState = trade.getTradeState();
                    //如果订单为未支付,且当前时间>订单创建时间+10分钟
                    if (tradeState < 1 && now().after(DateUtils.addSeconds(createTime, 600))) {
                        //客户请求的金额
                        BigDecimal totalAmount = trade.getTotalAmount();
                        //获得商城id
                        Long qrchannelId = trade.getQrchannelId();
                        //获得商城地址记录
                        QRChannel qrChannel = qrChannelDao.getOne(qrchannelId);
                        //获得商城名称
                        String qrChannelName = qrChannel.getName();
                        //获得冻结资金池
                        BigDecimal frozenCapitalPool = qrChannel.getFrozenCapitalPool();
                        logger.info("二维码通道:[" + qrChannelName + "]当前的冻结资金池为:" + frozenCapitalPool);
                        //获得充值金额
                        BigDecimal rechargeAmount = qrChannel.getRechargeAmount();
                        logger.info("二维码通道:[" + qrChannelName + "]当前的已充值资金池为:" + rechargeAmount);
                        //从冻结资金池减去请求金额
                        BigDecimal subtract = frozenCapitalPool.subtract(totalAmount);
                        //从资金池中加上请求金额
                        BigDecimal add = rechargeAmount.add(totalAmount);
                        //保存冻结资金池
                        qrChannel.setFrozenCapitalPool(subtract);
                        //保存资金池
                        qrChannel.setRechargeAmount(add);
                        qrChannelDao.save(qrChannel);
                        logger.info("请求金额:--[" + totalAmount + "]--已从冻结资金池减去,目前还剩[" + subtract + "]");
                        logger.info("请求金额:--[" + totalAmount + "]--已重新加进充值资金池,目前充值资金池为[" + add + "]");
                        ntimer.cancel();
                    }
                }
            }
        }, 600500);
    }*/


}

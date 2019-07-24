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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static co.b4pay.api.common.utils.DateUtil.now;

@Service
public class QRCheckOutService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(QRCheckOutService.class);


    /**
     * 从所属地址拿出符合请求条件的地址
     */
    public qrcode checkout(List<QRChannel> qrChannelList, BigDecimal totalMOney, int payType, Long merchantId, HttpServletRequest request,String outTradeNo) {
        logger.info("校验开始:");
        qrcode qrcode = null;
        for (int i = 0; i <= qrChannelList.size(); i++) {
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
            if (qrChannel != null && (qrChannel.getLastRequestTime() == null || now().after(DateUtils.addSeconds(qrChannel.getLastRequestTime(), qrChannel.getRate().intValue())))) {
                logger.info("校验的通道为:" + qrChannel.getName());
                //添加校验:控制二维码通道的访问速率
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
                    List<qrcode> qrcodeList = qrCodeDao.findByChannelIdAndStatusAndCodeTypeAndMoney(qrMerchantId, 1, payType, totalMOney);
                    logger.info("得到" + qrChannel.getName() + "符合条件的二维码有" + qrcodeList.size() + "个");
                    //创富金行的商户ID为
                    if (qrcodeList.size() == 0) {
                        logger.info("调用任意额度码");
                        qrcodeList = qrCodeDao.findByChannelIdAndStatusAndCodeTypeAndMoney(qrMerchantId, 1, payType, new BigDecimal(0));
                        logger.info("任意额度码有" + qrcodeList.size() + "个");
                        if (qrcodeList.size() != 0){
                            Random random = new Random();
                            for (int j = 0; j < qrcodeList.size(); j++) {
                                //修改为随机抽取二维码,不要轮询
                                j = random.nextInt(qrcodeList.size());
                                logger.info("j----------->" + j);
                                qrcode = qrcodeList.get(j);
                                if (qrcode != null && (qrcode.getLastRequestTime() == null || now().after(DateUtils.addSeconds(qrcode.getLastRequestTime(), qrcode.getRate().intValue())))) {
                                    logger.info("校验的二维码是" + qrcode.getName());
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
                                    /*throw new BizException(String.format("暂无可用通道,稍等一会再试!"));*/
                                }
                            }
                        }else if (qrChannelList.size()>1){
                            session.setAttribute("lunxun",lunxun + 1);
                        }else {
                            throw new BizException(String.format("暂无可用通道,稍等一会再试!"));
                        }
                    }else {
                        if (qrcodeList.size() != 0){
                            Random random = new Random();
                            for (int j = 0; j < qrcodeList.size(); j++) {
                                //修改为随机抽取二维码,不要轮询
                                j = random.nextInt(qrcodeList.size());
                                logger.info("j----------->" + j);
                                qrcode = qrcodeList.get(j);
                                if (qrcode != null && (qrcode.getLastRequestTime() == null || now().after(DateUtils.addSeconds(qrcode.getLastRequestTime(), qrcode.getRate().intValue())))) {
                                    logger.info("校验的二维码是" + qrcode.getName());
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
                                    /*throw new BizException(String.format("暂无可用通道,稍等一会再试!"));*/
                                }
                            }
                        }else {
                            session.setAttribute("lunxun",lunxun + 1);
                            throw new BizException(String.format("暂无可用通道,稍等一会再试!"));
                        }
                    }
                } else {
                    session.setAttribute("lunxun", lunxun + 1);
                    logger.info("通道:"+qrChannel.getName()+"资金池余额不足");
                    throw new BizException(String.format("暂无可用通道,稍等一会再试!"));
                }
            } else {
                session.setAttribute("lunxun", lunxun + 1);
                /*throw new BizException(String.format("暂无可用通道,稍等一会再试!"));*/
            }
        }
        return qrcode;
    }

    /**
     * 检查二维码
     */
    public synchronized qrcode getQrcode(String channelId,int codeType ,BigDecimal totalAmount){
        logger.info("----------进入通道内二维码轮询---------");
        long channelLongId = Long.parseLong(channelId);
        List<qrcode> qrcodeList = qrCodeDao.findByChannelIdAndStatusAndCodeTypeAndMoney(channelLongId, 1, codeType, totalAmount);
        qrcode qrcode=null;
        if (qrcodeList.size()==0){
            logger.info("-------------调用通道活码------------");
            List<qrcode> qrcodes = qrCodeDao.findByChannelIdAndStatusAndCodeTypeAndMoney(channelLongId, 1, codeType, new BigDecimal(0));
            if (qrcodes != null){
                 qrcode= lunXun(qrcodes);
                 return qrcode;
            }else {
                throw new BizException(String.format("通道内暂时没有适合的二维码,稍等一会再试!"));
            }
        }else {
            logger.info("-------------调用通道固码------------");
            qrcode = lunXun(qrcodeList);
            return qrcode;
        }

    }

    public qrcode lunXun(List<qrcode> qrcodeList){
        logger.info("++++++二维码轮询开始:-------->");
        Iterator<qrcode> iterator = qrcodeList.iterator();
        qrcode qrcode=null;
        while (iterator.hasNext()){
            qrcode next = iterator.next();
            if (now().after(DateUtils.addSeconds(next.getLastRequestTime(),next.getRate().intValue()))){
                qrcode=next;
                logger.info("++++++轮询到的二维码是:"+qrcode.getName());
                break;
            }
        }
        if (qrcode == null){
            throw new BizException(String.format("该通道内暂时没有适合的二维码,稍等一会再试!"));
        }
        return qrcode;
    }
}

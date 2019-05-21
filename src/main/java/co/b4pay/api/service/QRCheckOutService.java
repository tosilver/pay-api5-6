
package co.b4pay.api.service;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.model.QRChannel;
import co.b4pay.api.model.Trade;
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
import java.text.SimpleDateFormat;
import java.util.*;

import static co.b4pay.api.common.utils.DateUtil.now;


/*QR通道检测*/
@Service
public class QRCheckOutService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(QRCheckOutService.class);
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    //获取二维码的通道
    private QRChannel qrChannel;
    private Session session;
    Long merchantId;
    qrcode qrcode=null;
    /**
     * 从所属地址拿出符合请求条件的地址
     */
    public qrcode checkout(List<QRChannel> qrChannelList, BigDecimal totalMOney , int payType, HttpServletRequest request){
        logger.info("校验开始:");

        for (int i = 0; i < qrChannelList.size()-1; i++) {

            //第一个轮询开始
            Subject subject = SecurityUtils.getSubject();
            session = subject.getSession(true);
            if (session == null) {
                session.setAttribute("lunxun",0);
                session.setAttribute("qrlunxun",0);
            }

            Integer lunxun = (Integer) session.getAttribute("lunxun");

            if (lunxun == null || lunxun > qrChannelList.size()-1) {
                lunxun = 0;
            }
            i=lunxun;


            qrChannel = qrChannelList.get(i);

            System.out.println("开始校验通道了");
            logger.info("校验的通道为:" + qrChannel.getName());
            System.out.println("校验的通道为:"+qrChannel.getName());
            //添加校验:控制二维码通道的访问速率
            if (qrChannel !=null && (qrChannel.getLastRequestTime() ==null || now().after(DateUtils.addSeconds(qrChannel.getLastRequestTime(),qrChannel.getRate().intValue())))) {

                //得到二维码通道的充值金额
                BigDecimal rechargeAmount = qrChannel.getRechargeAmount();

                //得到二维码通道的冻结资金额
                BigDecimal frozenCapitalPool = qrChannel.getFrozenCapitalPool();


                //防止充值资金池溢出,要求充值资金池不能少于1000元
                //BigDecimal actualAmount = rechargeAmount.multiply(new BigDecimal(1000));
                BigDecimal actualAmount = new BigDecimal(1000.00);
                logger.info("资金池----->" + qrChannel.getName() + "限制金额为:" + actualAmount);
                /*金额的差度，判断用户是否输入金额*/
                int status = actualAmount.compareTo(rechargeAmount.subtract(totalMOney));


                if (status <= 0) {
                    logger.info("二维码通道:" + qrChannel.getName() + "校验成功!");
                    session.setAttribute("lunxun", lunxun + 1);
                    //把请求金额从充值资金池减去然后加进冻结资金池
                    BigDecimal subtract = rechargeAmount.subtract(totalMOney);
                    logger.info("充值资金池:----->" + rechargeAmount + "请求金额为:------->" + totalMOney + "相减等于:----->" + subtract);
                    BigDecimal add = frozenCapitalPool.add(totalMOney);
                    logger.info("冻结资金池:----->" + frozenCapitalPool + "请求金额为:------->" + totalMOney + "相加等于:----->" + add);
                    qrChannel.setRechargeAmount(subtract);
                    qrChannel.setFrozenCapitalPool(add);
                    qrChannel.setLastRequestTime(now());
                    qrChannelDao.save(qrChannel);
                    //得到校验成功的二维码通道的商户id
                    merchantId = qrChannel.getMerchantId();

                    /*进行第二级轮巡*/
                    this.test(qrChannelList,totalMOney,payType,session);


                }else {

                    session.setAttribute("lunxun",lunxun+1 );}
            }else {
                session.setAttribute("lunxun",lunxun+1 );
                throw new BizException(String.format("暂无可用通道,稍等一会再试!"));
            }
        }
        return qrcode;
    }



    /*用于第二次轮巡*/
    private void test(List<QRChannel> qrChannelList, BigDecimal totalMOney , int payType,Session session){
       // System.out.println("进入第二级轮巡");
        //得到对应商户id,支付类型,金额,状态为开始的支付二维码集合
        List<qrcode> qrcodeList = qrCodeDao.findBymerchantIdAndStatusAndCodeTypeAndMoney(merchantId, 1, payType, totalMOney);
        logger.info("得到"+qrChannel.getName()+"符合条件的二维码有"+qrcodeList.size()+"个");
        logger.info("调用任意额度码");
        /*如果第一次的查询是0，则再次查询*/
        if (qrcodeList.size()==0){
          //  System.out.println("list是0");
            /*根据商户ID和状态和二维码类型和收款金额来查找任意的二维码*/
            qrcodeList=qrCodeDao.findBymerchantIdAndStatusAndCodeTypeAndMoney(merchantId, 1, payType, new BigDecimal(0));
            logger.info("第二次任意额度码有"+qrcodeList.size()+"个");
            for (int a=0;a<qrcodeList.size();a++){
                Long id = qrcodeList.get(a).getId();
                if(qrcodeList.get(a).getNo()>=10) {
                    qrCodeDao.updateStatus(id, 0);//冻结该二维码
                }else {
                    qrCodeDao.updateNo(id, 0);//更新NO记录
                }
            }
        }
        else {
            /*第一次查询是非0*/
           // System.out.println("list不是0");
            logger.info("第一次查询任意额度码有"+qrcodeList.size()+"个");
            for (int a=0;a<qrcodeList.size();a++){
                Long id = qrcodeList.get(a).getId();
                if(qrcodeList.get(a).getNo()>=10) {
                    qrCodeDao.updateStatus(id, 0);//冻结该二维码
                }else {
                    qrCodeDao.updateNo(id, 0);//更新NO记录
                }
            }
        }
        /*我的，更新No之后，重新获取任意的二维码*/
        /*  qrcodeList=null;*/
        qrcodeList=qrCodeDao.findBymerchantIdAndStatusAndCodeTypeAndMoney(merchantId, 1, payType, new BigDecimal(0));
        logger.info("过滤后任意额度码有"+qrcodeList.size()+"个");
        /*第二级*/
        for (int j = 0; j < qrcodeList.size(); j++) {
            Integer qrlunxun = (Integer) session.getAttribute("qrlunxun");
            if (qrlunxun == null || qrlunxun > qrcodeList.size()-1) {
                qrlunxun = 0;
            }
            j=qrlunxun;
            qrcode = qrcodeList.get(j);
            logger.info("校验的二维码是"+qrcode.getName());
            if (qrcode !=null && (qrcode.getLastRequestTime() ==null||now().after(DateUtils.addSeconds(qrcode.getLastRequestTime(),qrcode.getRate().intValue())) )){
                String codeData = qrcode.getCodeData();
                if (codeData != null){
                    logger.info("校验通过的二维码是"+qrcode.getName());
                    qrcode.setLastRequestTime(now());
                    qrcode.setTurnover(totalMOney);
                    qrCodeDao.save(qrcode);
                    session.setAttribute("qrlunxun",qrlunxun+1 );
                    break;
                }
            }else {
                session.setAttribute("qrlunxun",qrlunxun+1 );
            }

        }

        /*第二级结束*/
        System.out.println("第二级轮巡结束");
    }





    /**
     * 订单计时器
     * (请求时触发此计时器,10分钟后执行把未支付的订单,把请求的金额从冻结资金池中减去,重新注入充值资金池)
     * @param outTradeNo
     */
    public void timer1(String outTradeNo ) {
        Timer ntimer = new Timer();
        ntimer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("计时器开始执行---------->");
                //根据客户订单号查询订单信息
                Trade trade = tradeDao.findByMerchantOrderNo(outTradeNo);
                if (trade !=null){
                    //得到订单创建时间
                    Date createTime = trade.getCreateTime();
                    logger.info("当前时间:"+format.format(new Date()));
                    logger.info("订单创建时间:"+createTime);
                    //得到订单状态
                    Integer tradeState = trade.getTradeState();
                    //如果订单为未支付,且当前时间>订单创建时间+10分钟
                    if (tradeState < 1 && now().after(DateUtils.addSeconds(createTime,600))){
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
                        logger.info("二维码通道:["+qrChannelName+"]当前的冻结资金池为:"+frozenCapitalPool);
                        //获得充值金额
                        BigDecimal rechargeAmount = qrChannel.getRechargeAmount();
                        logger.info("二维码通道:["+qrChannelName+"]当前的已充值资金池为:"+rechargeAmount);
                        //从冻结资金池减去请求金额
                        BigDecimal subtract = frozenCapitalPool.subtract(totalAmount);
                        //从资金池中加上请求金额
                        BigDecimal add = rechargeAmount.add(totalAmount);
                        //保存冻结资金池
                        qrChannel.setFrozenCapitalPool(subtract);
                        //保存资金池
                        qrChannel.setRechargeAmount(add);
                        qrChannelDao.save(qrChannel);
                        logger.info("请求金额:--["+totalAmount+"]--已从冻结资金池减去,目前还剩["+subtract+"]");
                        logger.info("请求金额:--["+totalAmount+"]--已重新加进充值资金池,目前充值资金池为["+add+"]");
                    }
                }
            }
        },600500);
    }
}

/*
package co.b4pay.api.service;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.model.QRChannel;
import co.b4pay.api.model.Trade;
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
import java.text.SimpleDateFormat;
import java.util.*;

import static co.b4pay.api.common.utils.DateUtil.now;


*/
/*QR通道检测*//*

@Service
public class QRCheckOutjiuService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(QRCheckOutjiuService.class);
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    */
/**
 * 从所属地址拿出符合请求条件的地址
 *//*

    public qrcode checkout(List<QRChannel> qrChannelList, BigDecimal totalMOney , int payType, HttpServletRequest request){
        logger.info("校验开始:");
        qrcode qrcode=null;
        for (int i = 0; i < qrChannelList.size()-1; i++) {

            //第一个轮询开始
            Subject subject = SecurityUtils.getSubject();
            Session session = subject.getSession(true);
            if (session == null) {
                session.setAttribute("lunxun",0);
                session.setAttribute("qrlunxun",0);
            }

            Integer lunxun = (Integer) session.getAttribute("lunxun");

            if (lunxun == null || lunxun > qrChannelList.size()-1) {
                lunxun = 0;
            }
            i=lunxun;


            //获取二维码的通道
            QRChannel qrChannel = qrChannelList.get(i);

            System.out.println("开始校验通道了222222222222");
            logger.info("校验的通道为:" + qrChannel.getName());
            System.out.println("校验的通道为:"+qrChannel.getName());
            //添加校验:控制二维码通道的访问速率
            if (qrChannel !=null && (qrChannel.getLastRequestTime() ==null || now().after(DateUtils.addSeconds(qrChannel.getLastRequestTime(),qrChannel.getRate().intValue())))) {

                //得到二维码通道的充值金额
                BigDecimal rechargeAmount = qrChannel.getRechargeAmount();

                //得到二维码通道的冻结资金额
                BigDecimal frozenCapitalPool = qrChannel.getFrozenCapitalPool();


                //防止充值资金池溢出,要求充值资金池不能少于1000元
                //BigDecimal actualAmount = rechargeAmount.multiply(new BigDecimal(1000));
                BigDecimal actualAmount = new BigDecimal(1000.00);
                logger.info("资金池----->" + qrChannel.getName() + "限制金额为:" + actualAmount);
                */
/*金额的差度，判断用户是否输入金额*//*

                int status = actualAmount.compareTo(rechargeAmount.subtract(totalMOney));


                if (status <= 0) {
                    logger.info("二维码通道:" + qrChannel.getName() + "校验成功!");
                    session.setAttribute("lunxun", lunxun + 1);
                    //把请求金额从充值资金池减去然后加进冻结资金池
                    BigDecimal subtract = rechargeAmount.subtract(totalMOney);
                    logger.info("充值资金池:----->" + rechargeAmount + "请求金额为:------->" + totalMOney + "相减等于:----->" + subtract);
                    BigDecimal add = frozenCapitalPool.add(totalMOney);
                    logger.info("冻结资金池:----->" + frozenCapitalPool + "请求金额为:------->" + totalMOney + "相加等于:----->" + add);
                    qrChannel.setRechargeAmount(subtract);
                    qrChannel.setFrozenCapitalPool(add);
                    qrChannel.setLastRequestTime(now());
                    qrChannelDao.save(qrChannel);


                    //得到校验成功的二维码通道的商户id
                    Long merchantId = qrChannel.getMerchantId();


                    //得到对应商户id,支付类型,金额,状态为开始的支付二维码集合
                    List<qrcode> qrcodeList = qrCodeDao.findBymerchantIdAndStatusAndCodeTypeAndMoney(merchantId, 1, payType, totalMOney);
                    logger.info("得到"+qrChannel.getName()+"符合条件的二维码有"+qrcodeList.size()+"个");
                    logger.info("调用任意额度码");


                    */
/*如果第一次的查询是0，则再次查询*//*

                    if (qrcodeList.size()==0){
                        System.out.println("list是0");
                        */
/*根据商户ID和状态和二维码类型和收款金额来查找任意的二维码*//*

                        qrcodeList=qrCodeDao.findBymerchantIdAndStatusAndCodeTypeAndMoney(merchantId, 1, payType, new BigDecimal(0));
                        logger.info("第二次任意额度码有"+qrcodeList.size()+"个");
                        for (int a=0;a<qrcodeList.size();a++){
                            Long id = qrcodeList.get(a).getId();
                            if(qrcodeList.get(a).getNo()>=10) {
                                qrCodeDao.updateStatus(id, 0);//冻结该二维码
                            }else {
                                qrCodeDao.updateNo(id, 0);//更新NO记录
                            }
                        }
                    }
                    else {
                        */
/*第一次查询是非0*//*

                        System.out.println("list不是0");
                        logger.info("第一次查询任意额度码有"+qrcodeList.size()+"个");
                        for (int a=0;a<qrcodeList.size();a++){
                            Long id = qrcodeList.get(a).getId();
                            if(qrcodeList.get(a).getNo()>=10) {
                                qrCodeDao.updateStatus(id, 0);//冻结该二维码
                            }else {
                                qrCodeDao.updateNo(id, 0);//更新NO记录
                            }
                        }
                    }
                    */
/*我的，更新No之后，重新获取任意的二维码*//*

 */
/*  qrcodeList=null;*//*

                    qrcodeList=qrCodeDao.findBymerchantIdAndStatusAndCodeTypeAndMoney(merchantId, 1, payType, new BigDecimal(0));
                    logger.info("过滤后任意额度码有"+qrcodeList.size()+"个");



                    */
/*第二级*//*

                    for (int j = 0; j < qrcodeList.size(); j++) {
                        Integer qrlunxun = (Integer) session.getAttribute("qrlunxun");
                        if (qrlunxun == null || qrlunxun > qrcodeList.size()-1) {
                            qrlunxun = 0;
                        }
                        j=qrlunxun;
                        qrcode = qrcodeList.get(j);
                        logger.info("校验的二维码是"+qrcode.getName());
                        if (qrcode !=null && (qrcode.getLastRequestTime() ==null||now().after(DateUtils.addSeconds(qrcode.getLastRequestTime(),qrcode.getRate().intValue())) )){
                            String codeData = qrcode.getCodeData();
                            if (codeData != null){
                                logger.info("校验通过的二维码是"+qrcode.getName());
                                qrcode.setLastRequestTime(now());
                                qrcode.setTurnover(totalMOney);
                                qrCodeDao.save(qrcode);
                                session.setAttribute("qrlunxun",qrlunxun+1 );
                                break;
                            }
                        }else {
                            session.setAttribute("qrlunxun",qrlunxun+1 );
                        }
                    }*/
/*第二级结束*//*






                }else {

                    session.setAttribute("lunxun",lunxun+1 );}
            }else {
                session.setAttribute("lunxun",lunxun+1 );
                throw new BizException(String.format("暂无可用通道,稍等一会再试!"));
            }
        }
        return qrcode;
    }


    */
/**
 * 订单计时器
 * (请求时触发此计时器,10分钟后执行把未支付的订单,把请求的金额从冻结资金池中减去,重新注入充值资金池)
 * @param outTradeNo
 *//*

    public void timer1(String outTradeNo ) {
        Timer ntimer = new Timer();
        ntimer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("计时器开始执行---------->");
                //根据客户订单号查询订单信息
                Trade trade = tradeDao.findByMerchantOrderNo(outTradeNo);
                if (trade !=null){
                    //得到订单创建时间
                    Date createTime = trade.getCreateTime();
                    logger.info("当前时间:"+format.format(new Date()));
                    logger.info("订单创建时间:"+createTime);
                    //得到订单状态
                    Integer tradeState = trade.getTradeState();
                    //如果订单为未支付,且当前时间>订单创建时间+10分钟
                    if (tradeState < 1 && now().after(DateUtils.addSeconds(createTime,600))){
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
                        logger.info("二维码通道:["+qrChannelName+"]当前的冻结资金池为:"+frozenCapitalPool);
                        //获得充值金额
                        BigDecimal rechargeAmount = qrChannel.getRechargeAmount();
                        logger.info("二维码通道:["+qrChannelName+"]当前的已充值资金池为:"+rechargeAmount);
                        //从冻结资金池减去请求金额
                        BigDecimal subtract = frozenCapitalPool.subtract(totalAmount);
                        //从资金池中加上请求金额
                        BigDecimal add = rechargeAmount.add(totalAmount);
                        //保存冻结资金池
                        qrChannel.setFrozenCapitalPool(subtract);
                        //保存资金池
                        qrChannel.setRechargeAmount(add);
                        qrChannelDao.save(qrChannel);
                        logger.info("请求金额:--["+totalAmount+"]--已从冻结资金池减去,目前还剩["+subtract+"]");
                        logger.info("请求金额:--["+totalAmount+"]--已重新加进充值资金池,目前充值资金池为["+add+"]");
                    }
                }
            }
        },600500);
    }
}
*/

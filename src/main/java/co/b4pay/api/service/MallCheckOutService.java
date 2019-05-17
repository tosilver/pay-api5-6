package co.b4pay.api.service;

import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.model.MallAddress;
import co.b4pay.api.model.Trade;
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

@Service
public class MallCheckOutService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(MallCheckOutService.class);
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * 从所属地址拿出符合请求条件的地址
     */
    public MallAddress checkout(String[] arr, BigDecimal totalMOney ,HttpServletRequest request){
        //把数组转list
        List<String> asList = Arrays.asList(arr);
        logger.info("asList----->:"+asList);
        //轮询开始
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession(true);
        if (session == null) {
            session.setAttribute("lunxun",0);
        }
        Integer lunxun = (Integer) session.getAttribute("lunxun");
        if (lunxun == null || lunxun > arr.length - 1 ) {
            lunxun = 0;
        }
        MallAddress mallAddress=null;
        for (int i = 0; i < asList.size(); i++) {
            i=lunxun;
            String addressId = asList.get(i);
            //String addressId = iterator.next();
            logger.info("校验的地址id为:" + addressId);
            //把id转化为long类型
            long addressLongId = Long.valueOf(addressId);
            //根据id和状态查询地址
            MallAddress _mallAddress = mallAddressDao.findByIdAndStatus(addressLongId, 1);
            //添加校验:控制商城地址访问速率
            if (_mallAddress !=null && (_mallAddress.getLastRequestTime() ==null || now().after(DateUtils.addSeconds(_mallAddress.getLastRequestTime(),_mallAddress.getRate().intValue())))) {
                //得到商城的充值金额
                BigDecimal rechargeAmount = _mallAddress.getRechargeAmount();
                //得到该商场的冻结资金额
                BigDecimal frozenCapitalPool = _mallAddress.getFrozenCapitalPool();
                //防止充值资金池溢出,取资金池的15%的比较(100%-15%)
                //BigDecimal actualAmount = rechargeAmount.multiply(new BigDecimal(85)).divide(new BigDecimal(100));
                BigDecimal actualAmount = new BigDecimal(1000);

                logger.info("资金池----->" + _mallAddress.getMallName() + "限制金额为:" + actualAmount);
                int status = rechargeAmount.compareTo(actualAmount);
                if (status > 0) {
                    logger.info("地址id:" + addressId + "校验成功!");
                    session.setAttribute("lunxun", lunxun + 1);
                    //把请求金额从充值资金池减去然后加进冻结资金池
                    BigDecimal subtract = rechargeAmount.subtract(totalMOney);
                    logger.info("充值资金池:----->" + rechargeAmount + "请求金额为:------->" + totalMOney + "相减等于:----->" + subtract);
                    BigDecimal add = frozenCapitalPool.add(totalMOney);
                    logger.info("冻结资金池:----->" + frozenCapitalPool + "请求金额为:------->" + totalMOney + "相加等于:----->" + add);
                    _mallAddress.setRechargeAmount(subtract);
                    _mallAddress.setFrozenCapitalPool(add);
                    _mallAddress.setLastRequestTime(now());
                    mallAddressDao.save(_mallAddress);
                    mallAddress = _mallAddress;
                    return mallAddress;
                }
            }else {
                session.setAttribute("lunxun",lunxun+1 );
                break;
            }
        }
        if (mallAddress == null){
            session.setAttribute("lunxun",lunxun+1 );
            throw new BizException(String.format("暂无可用通道,稍等一会再试!"));
        }
        return mallAddress;
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
                        Long addressId = trade.getAddressId();
                        //获得商城地址记录
                        MallAddress mallAddress = mallAddressDao.getOne(addressId);
                        //获得商城名称
                        String mallName = mallAddress.getMallName();
                        //获得冻结资金池
                        BigDecimal frozenCapitalPool = mallAddress.getFrozenCapitalPool();
                        logger.info("商城:["+mallName+"]当前的冻结资金池为:"+frozenCapitalPool);
                        //获得充值金额
                        BigDecimal rechargeAmount = mallAddress.getRechargeAmount();
                        logger.info("商城:["+mallName+"]当前的已充值资金池为:"+rechargeAmount);
                        //从冻结资金池减去请求金额
                        BigDecimal subtract = frozenCapitalPool.subtract(totalAmount);
                        //从资金池中加上请求金额
                        BigDecimal add = rechargeAmount.add(totalAmount);
                        //保存冻结资金池
                        mallAddress.setFrozenCapitalPool(subtract);
                        //保存资金池
                        mallAddress.setRechargeAmount(add);
                        mallAddressDao.save(mallAddress);
                        logger.info("请求金额:--["+totalAmount+"]--已从冻结资金池减去,目前还剩["+subtract+"]");
                        logger.info("请求金额:--["+totalAmount+"]--已重新加进充值资金池,目前充值资金池为["+add+"]");
                    }
                }
            }
        },600500);
    }
}

package co.b4pay.api.service;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.tenpay.PrepayIdRequestHandler;
import co.b4pay.api.common.tenpay.util.WXUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.dao.ChannelDao;
import co.b4pay.api.dao.MerchantRateDao;
import co.b4pay.api.dao.TradeDao;
import co.b4pay.api.model.Channel;
import co.b4pay.api.model.MerchantRate;
import co.b4pay.api.model.Router;
import co.b4pay.api.model.Trade;
import co.b4pay.api.model.base.AjaxResponse;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * 官方微信支付Service
 *
 * @author YK
 * @version $Id: YueManBankService.java, v 0.1 2018年6月6日 上午9:28:58 YK Exp $
 */
@Service
@Transactional
public class WeiXinPayService extends BasePayService {

    @Autowired
    private ChannelDao channelDao;

    @Autowired
    private TradeDao tradeDao;

    @Autowired
    private MerchantRateDao merchantRateDao;

    private static final Logger logger = LoggerFactory.getLogger(WeiXinPayService.class);
    private static final String GAGE_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";

    public AjaxResponse execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request, HttpServletResponse response) throws BizException, JDOMException, IOException {
        List<Channel> channelList = channelDao.findByRouterIdAndStatus(router.getId(), 1);
        if (CollectionUtils.isEmpty(channelList)) {
            throw new BizException(String.format("[%s, %s]暂无可用通道", merchantId, router.getId()));
        }
        BigDecimal totalAmount = new BigDecimal(params.getString("total_amount")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_UP);//分转元

        MerchantRate merchantRate = merchantRateDao.findByMerchantIdAndRouterId(merchantId, router.getId());
        if (merchantRate == null) {
            throw new BizException(String.format("[%s, %s]商户接口费率设置异常", merchantId, router.getId()));
        }

        Channel channel = getChannel(merchantId, router, totalAmount);

        if (channel.getUnitPrice().compareTo(totalAmount) < 0) {
            throw new BizException(String.format("单笔交易不能大于%s元", channel.getUnitPrice()));
        }
        if (channel.getMinPrice().compareTo(totalAmount) > 0) {
            throw new BizException(String.format("单笔交易不能低于%s元", channel.getMinPrice()));
        }
        String userName = BooleanUtils.toString(MainConfig.isDevMode, channel.getTestPid(), channel.getProdPid());
        String password = BooleanUtils.toString(MainConfig.isDevMode, channel.getTestPrivateKey(), channel.getProdPrivateKey());
        String appId = BooleanUtils.toString(MainConfig.isDevMode, channel.getTestAppid(), channel.getProdAppid());
        params.put("mcht_no", userName);
        String merchantNotifyUrl = params.getString("notify_url");
        params.put("notify_url", String.format("%s/notify/wxH5PayNotify.do", WebUtil.getServerUrl(request)));
        PrepayIdRequestHandler prepayReqHandler = new PrepayIdRequestHandler(request, response);// 获取prepayid的请求类
        prepayReqHandler.setKey(password);
        String noncestr = WXUtil.getNonceStr();
        String timestamp = WXUtil.getTimeStamp();
        // 设置package订单参数
        prepayReqHandler.setParameter("appid", appId);// 公众账号ID
        prepayReqHandler.setParameter("mch_id", userName);// 商户号
        prepayReqHandler.setParameter("nonce_str", noncestr);// 随机字符串
        prepayReqHandler.setParameter("body", params.getString("body")); // 商品描述
        prepayReqHandler.setParameter("out_trade_no", params.getString("trade_no")); // 商户订单号
        prepayReqHandler.setParameter("total_fee", params.getString("total_amount")); // 商品金额,以分为单位
        prepayReqHandler.setParameter("spbill_create_ip", params.getString("spbill_create_ip")); // 订单生成的机器IP，指用户浏览器端IP
        prepayReqHandler.setParameter("notify_url", params.getString("notify_url")); // 接收财付通通知的URL
        prepayReqHandler.setParameter("scene_info", params.getString("scene_info"));//场景信息
        prepayReqHandler.setParameter("trade_type", "MWEB");//交易类型
        prepayReqHandler.setParameter("attach", params.getString("attach")); // 附加数据

        // 生成获取预支付签名
        String sign = prepayReqHandler.createMD5Sign();

        // 增加非参与签名的额外参数
        prepayReqHandler.setParameter("sign", sign);// 签名
        long time = System.currentTimeMillis();
        prepayReqHandler.setGateUrl(GAGE_URL);
        String resContent = prepayReqHandler.sendPrepay();
        logger.info(resContent);
        time = System.currentTimeMillis() - time;

        String tradeNo = String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15));//交易订单号
        Trade trade = new Trade();
        trade.setMerchantId(merchantId);
        trade.setId(tradeNo);
        trade.setMerchantOrderNo(params.getString("trade_no"));//商户订单号
        trade.setChannelId(channel.getId());
        trade.setTime(time);
        trade.setNotifyUrl(merchantNotifyUrl);
        trade.setTotalAmount(totalAmount);
        BigDecimal serviceCharge = trade.getTotalAmount().multiply(merchantRate.getCostRate(), new MathContext(2, RoundingMode.HALF_UP)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_UP).add(merchantRate.getPayCost());
        trade.setAccountAmount(trade.getTotalAmount().subtract(serviceCharge));
        trade.setCostRate(merchantRate.getCostRate());
        trade.setPayCost(merchantRate.getPayCost());
        trade.setTradeState(0);
        trade.setStatus(1);
        trade.setServiceCharge(serviceCharge);
        trade.setHeader(null);
        trade.setRequest(params.toJSONString());
        trade.setRemark(null);
        trade.setCreateTime(DateUtil.getTime());
        trade.setUpdateTime(DateUtil.getTime());
        tradeDao.save(trade);

        channel.setAmountLimit(channel.getAmountLimit().subtract(totalAmount));
        channelDao.save(channel);

        return AjaxResponse.success(resContent);
    }
}

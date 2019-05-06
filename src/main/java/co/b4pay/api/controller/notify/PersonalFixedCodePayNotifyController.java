package co.b4pay.api.controller.notify;

import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.model.Channel;
import co.b4pay.api.model.Merchant;
import co.b4pay.api.model.Trade;
import co.b4pay.api.service.ChannelService;
import co.b4pay.api.service.MerchantService;
import co.b4pay.api.service.TradeService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Enumeration;

/**
 * 固额扫码支付通知
 */
@RestController
@RequestMapping({"/notify/fixedCodePayNotify.do"})
public class PersonalFixedCodePayNotifyController {
    private static final Logger logger = LoggerFactory.getLogger(PersonalFixedCodePayNotifyController.class);

    @Autowired
    private TradeService tradeService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private HmacSHA1Signature signature = new HmacSHA1Signature();

    @RequestMapping(method = {RequestMethod.POST})
    public void doPost(HttpServletRequest request) {
        //System.out.println("支付结果通用通知：" + ServletUtil.getQueryString(request));
        JSONObject jsonObject = new JSONObject();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            jsonObject.put(parameterName, request.getParameter(parameterName));
        }
        if (jsonObject.isEmpty()) {
            return;
        }

        logger.warn("fixedCodePayNotify parameters ->" + jsonObject.toJSONString());

        String merchantId = request.getParameter("merchantId");
        String amount = request.getParameter("amount");
        String phoneCode = request.getParameter("phoneCode");
        if (StringUtils.isBlank(phoneCode) || StringUtils.isBlank(amount)
                || StringUtils.isBlank(merchantId)) {
            logger.warn(String.format("手机端通知参数有误,merchantId:%s amount:%s phoneCode:%s", merchantId, amount, phoneCode));
        } else {
            Channel channel = channelService.getChannel(phoneCode);
            Trade trade = null;
            try {
                trade = tradeService.findByAmountAndChannel(new BigDecimal(amount), merchantId, channel.getId().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (trade == null) {
                logger.warn(String.format("[%s]商户号，金额为[%s]的订单信息不存在", merchantId, amount));
            } else {
                Merchant merchant = merchantService.findById(trade.getMerchantId());
                if (merchant == null) {
                    logger.warn(String.format("[%s]商户号不存在", merchantId));
                } else {
                    trade.setStatus(1);
                    trade.setTradeState(1);
                    trade.setAccountAmount(trade.getTotalAmount());
                    tradeService.save(trade);
                    try {

                        //调度参数
                        JSONObject returnData = new JSONObject();
                        returnData.put("tradeNo", trade.getMerchantOrderNo());
                        returnData.put("amount", trade.getTotalAmount().toPlainString());
                        returnData.put("tradeState", String.valueOf(trade.getTradeState()));
                        returnData.put("merchantId", merchantId);
                        returnData.put("payTime", String.valueOf(trade.getUpdateTime().getTime()));

                        String content = SignatureUtil.getSignatureContent(returnData, true);
                        String sign = signature.sign(content, merchant.getSecretKey(), Constants.CHARSET_UTF8);
                        returnData.put("signature", sign);

                        logger.warn("notify url ->" + trade.getNotifyUrl());
                        logger.warn("notify data ->" + returnData.toJSONString());
                        HttpsUtils.post(trade.getNotifyUrl(), null, returnData.toJSONString());
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        }
    }
}

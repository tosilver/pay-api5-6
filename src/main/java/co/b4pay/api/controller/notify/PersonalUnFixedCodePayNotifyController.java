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
import org.apache.commons.lang3.StringUtils;
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
 * 无固额扫码支付通知
 */
@RestController
@RequestMapping({"/notify/unFixedCodePayNotify.do"})
public class PersonalUnFixedCodePayNotifyController {
    private static final Logger logger = LoggerFactory.getLogger(PersonalUnFixedCodePayNotifyController.class);

    @Autowired
    private TradeService tradeService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private HmacSHA1Signature signature = new HmacSHA1Signature();

    @Autowired
    private MerchantService merchantService;

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

        logger.warn("unFixedCodePayNotify parameters ->" + jsonObject.toJSONString());

        String merchantId = request.getParameter("merchantId");
        Channel channel = channelService.getChannel(request.getParameter("phoneCode"));
        Trade trade = tradeService.findByAmountAndChannel(new BigDecimal(0), merchantId, channel.getId().toString());
        if (trade == null) {
            logger.warn(String.format("[%s]商户号的订单信息不存在", merchantId));
        } else {
            Merchant merchant = merchantService.findById(trade.getMerchantId());
            String amount = request.getParameter("amount") == null ? "0" : request.getParameter("amount");
            trade.setAccountAmount(new BigDecimal(amount));
            trade.setTotalAmount(new BigDecimal(amount));
            trade.setStatus(1);
            trade.setTradeState(1);
            trade.setAccountAmount(trade.getTotalAmount());
            tradeService.save(trade);
            try {
                JSONObject returnData = new JSONObject();
                returnData.put("tradeNo", trade.getMerchantOrderNo());
                returnData.put("amount", trade.getTotalAmount());
                returnData.put("result", "支付成功");
                String content = SignatureUtil.getSignatureContent(returnData, true);

                String sign = signature.sign(content, merchant.getSecretKey(), Constants.CHARSET_UTF8);
                logger.warn("notify url ->" + trade.getNotifyUrl());
                logger.warn("notify data ->" + returnData.toJSONString());
                HttpsUtils.post(trade.getNotifyUrl(), null, returnData.toJSONString());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                e.printStackTrace();
            }
        }
        //trade.setPayOrderNo(request.getParameter("trade_no"));


    }
}

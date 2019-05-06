package co.b4pay.api.controller.notify;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.tenpay.ResponseHandler;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.StringUtil;
import co.b4pay.api.model.*;
import co.b4pay.api.service.*;
import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;

/**
 * @author YK
 * @version $Id v 0.1 2018年06月04日 16:32 Exp $
 */
@RestController
@RequestMapping("/notify/wxH5PayNotify.do")
public class WxH5PayNotifyController {

    private static final Logger logger = LoggerFactory.getLogger(WxH5PayNotifyController.class);

    private static final String SUCCESS = "SUCCESS";

    private static final String ROUTER_KEY = "wxH5Pay";

    @Autowired
    private RouterService routerService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private JobTradeService jobTradeService;

    @Autowired
    ChannelService channelService;

    @RequestMapping(method = {RequestMethod.POST})
    public String doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
//            if (logger.isInfoEnabled()) {
//                logger.info("支付结果通用通知：" + ServletUtil.getQueryString(request));
//                System.out.println("支付通知：" + ServletUtil.getQueryString(request));
//            }
            Router router = routerService.findById(ROUTER_KEY);
            if (router == null || router.getStatus() == -1) {
                logger.error("路由异常");
                return StringUtil.EMPTY;
            }
            // 创建支付应答对象
            ResponseHandler resHandler = new ResponseHandler(request, response);
            String orderNo = resHandler.getParameter("out_trade_no");// 支付订单号
            Trade trade = tradeService.findByMerchantOrderNo(orderNo);
            Channel channel = channelService.findById(trade.getChannelId());
            String password = BooleanUtils.toString(MainConfig.isDevMode, channel.getTestPrivateKey(), channel.getProdPrivateKey());
            JSONObject jsonObject = JSONObject.parseObject(JSONUtils.toJSONString(resHandler.getAllParameters()));
            jsonObject.remove("sign");
            resHandler.setKey(password);
            if (logger.isInfoEnabled()) {
                logger.info("支付结果通用通知，通过流获取：" + resHandler.getAllParameters());
            }
            if (logger.isInfoEnabled()) {
                logger.info("支付应答对象 DebugInfo：" + resHandler.getDebugInfo());
            }
            // 判断签名
            if (resHandler.isTenpaySign()) {
                // 判断签名及结果
                if (resHandler.isTenpaySign() && SUCCESS.equals(resHandler.getParameter("return_code")) && SUCCESS.equals(resHandler.getParameter("result_code"))) {
                    logger.info("订单查询成功");
                    logger.info("out_trade_no:" + resHandler.getParameter("out_trade_no") + " transaction_id:" + resHandler.getParameter("transaction_id"));
                    logger.info("trade_state:" + resHandler.getParameter("trade_state") + " total_fee:" + resHandler.getParameter("total_fee"));
                    logger.info("discount:" + resHandler.getParameter("discount") + " time_end:" + resHandler.getParameter("time_end"));
                    if (trade.getTradeState() == 0) {//未支付

                        trade.setStatus(1);
                        trade.setPayOrderNo(jsonObject.getString("sysNo"));
                        trade.setTradeState(1);
                        trade.setRequest(jsonObject.toJSONString());
                        tradeService.save(trade);
                        logger.info("【" + ROUTER_KEY + "】支付处理成功：" + jsonObject);
                        Merchant merchant = merchantService.findById(trade.getMerchantId());
                        String merchantSignatureContent = SignatureUtil.getSignatureContent(jsonObject, false) + "&key=" + merchant.getSecretKey();
                        String merchantSignature = DigestUtils.md5Hex(merchantSignatureContent).toUpperCase();
                        jsonObject.put("sign", merchantSignature);

                        JobTrade jobTrade = new JobTrade();
                        jobTrade.setId(trade.getId());
                        jobTrade.setContent(jsonObject.toJSONString());
                        jobTrade.setNotifyUrl(trade.getNotifyUrl());
                        jobTrade.setStatus(0);
                        jobTrade.setCount(0);
                        jobTrade.setChannelType(ChannelType.WEIXIN);
                        jobTrade.setCreateTime(DateUtil.getTime());
                        jobTrade.setUpdateTime(DateUtil.getTime());
                        jobTrade.setExecTime(DateUtil.add(DateUtil.getTime(), Calendar.MINUTE, Integer.valueOf(MainConfig.jobIntervalTime.split(",")[0])));
                        jobTradeService.save(jobTrade);

                        String result = HttpsUtils.post(trade.getNotifyUrl(), null, jsonObject.toJSONString());
                        int status = SUCCESS.equals(result) ? 1 : 0;
                        jobTrade.setStatus(status);
                        jobTradeService.save(jobTrade);

                    } else {
                        logger.error("订单号【" + orderNo + "】已支付或错误，状态【" + trade.getStatus() + "】");
                        return StringUtil.EMPTY;
                    }
                    resHandler.sendToCFT(SUCCESS);
                } else {
                    // 错误时，返回结果未签名，记录retcode、retmsg看失败详情。
                    logger.warn("查询验证签名失败或业务错误： retcode:" + resHandler.getParameter("retcode") + " retmsg:" + resHandler.getParameter("retmsg") + " err_code:" + resHandler.getParameter("err_code")
                            + " err_code_des:" + resHandler.getParameter("err_code_des"));
                }
            } else {
                logger.warn("通知签名验证失败");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return SUCCESS;
    }
}

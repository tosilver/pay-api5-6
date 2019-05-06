package co.b4pay.api.service;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.tosdomutils.HttpClient;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.common.zengutils.HCMD5;
import co.b4pay.api.common.zengutils.HttpClientUtil;
import co.b4pay.api.model.Channel;
import co.b4pay.api.model.Goods;
import co.b4pay.api.model.MerchantRate;
import co.b4pay.api.model.Router;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.demo.trade.config.Configs;
import com.google.zxing.WriterException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static co.b4pay.api.common.utils.DateUtil.now;

/**
 * MALL支付查询
 *
 * @author zgp
 * @version
 */
@Service
//@Transactional
public class MallQueryPayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(MallQueryPayService.class);

    private static final String MALLPAYQUERY_API_DOMAIN = MainConfig.getConfig("MALLPAYQUERY_API_DOMAIN");

    private HmacSHA1Signature signature = new HmacSHA1Signature();


    public JSONObject executeReturn(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws BizException {

        logger.info("mallqueryPayService-->executeReturn:" + params);
        BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        Channel channel = getChannel(merchantId, router, totalAmount); // 预校验
        if (channel.getIp4() == null) {
            channel.setStatus(-1);
            channel.setUpdateTime(now());
            channelDao.save(channel);
            throw new BizException("渠道地址设置异常");
        }
        Map m = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            m.put(parameterName, request.getParameter(parameterName));
        }
        m.put("channelId", channel.getId().toString());
        m.remove("signature");
        try {
            String content = SignatureUtil.getSignatureContent(m, true);
            String sign = signature.sign(content, merchantDao.getOne(merchantId).getSecretKey(), Constants.CHARSET_UTF8);
            m.put("signature", sign);
            String result = HttpsUtils.post(channel.getIp4() + "/pay/mallqueryPayExecute.do", null, m);
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }

    }

    public JSONObject execute(Long merchantId, Router router, JSONObject params, HttpServletRequest request) throws Exception {
        logger.info("mallqueryPayService-->execute:" + params);

        String serverUrl = WebUtil.getServerUrl(request);
        logger.warn("server url :" + serverUrl);
        long time = System.currentTimeMillis();
        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        //BigDecimal totalAmount = new BigDecimal(params.getString("totalAmount"));
        Channel channel = null;
        if (params.containsKey("channelId")) {
            channel = channelDao.getOne(Long.parseLong(params.getString("channelId")));
        }
        if (channel == null || channel.getStatus() < 0) {
            throw new BizException("渠道异常,请稍后再试！");
        }
        if (StringUtils.isBlank(channel.getGoodsTypeId())) {
            throw new BizException("渠道商品类别为空");
        }
        List<Goods> goodsList = goodsDao.findByTypeId(Integer.valueOf(channel.getGoodsTypeId()));
        if (goodsList.size() == 0) {
            throw new BizException(String.format("渠道商品类别为空,类别序列：%s)", channel.getGoodsTypeId()));
        }

        MerchantRate merchantRate = merchantRateDao.findByMerchantIdAndRouterId(merchantId, router.getId());
        if (merchantRate == null) {
            throw new BizException(String.format("[%s, %s]商户费率设置异常", merchantId, router.getId()));
        }

        String outTradeNo = params.getString("out_trade_no");//"tradeprecreate" + System.currentTimeMillis() + (long) (Math.random() * 10000000L);

        //构建请求参数
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        //long diyOrderId = idWorker.nextId();
        sb.append("out_trade_no=").append(outTradeNo);
        logger.warn("参数:" + sb.toString());
        // 发送get请求
        HttpClient httpClient=new HttpClient(MALLPAYQUERY_API_DOMAIN+sb.toString());
        // 返回数据
        logger.warn("请求开始:");
        httpClient.get();
        logger.warn("请求结束!!!!!!!");
        String content = httpClient.getContent();
        System.out.println("[mall查询]应答报文:  " + content);
        JSONObject rspJson = JSON.parseObject(content);
        if (rspJson != null) {
            if (StringUtils.isNotBlank(rspJson.getString("code")) && "10000".equals(rspJson.getString("code"))) {
                String retmsg = rspJson.getString("msg");
                logger.info("[mall查询]返回的结果:", retmsg);
                //支付账户
                String buyerLogonId = rspJson.getString("buyer_logon_id");
                //金额
                String  outTradeNo1= rspJson.getString("out_trade_no");
                //应答码
                String retcode = rspJson.getString("code");
                //支付时间
                String payTime = rspJson.getString("send_pay_date");
                //支付金额
                String totalAmount = rspJson.getString("total_amount");
                //交易状态
                String tradeStatus = rspJson.getString("trade_status");


                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no",outTradeNo1);
                jsonObject.put("retcode",retcode);
                jsonObject.put("retmsg", retmsg);
                jsonObject.put("pay_time", payTime);
                jsonObject.put("total_amount", totalAmount);
                jsonObject.put("trade_status", tradeStatus);
                jsonObject.put("buyer_logon_id", buyerLogonId);
                return jsonObject;
            } else if (StringUtils.isNotBlank(rspJson.getString("code")) && "40004".equals(rspJson.getString("code"))){
                String retmsg = rspJson.getString("msg");
                logger.info("[快捷查询]返回的结果:", retmsg);
                //支付账户
                String buyerLogonId = rspJson.getString("buyer_logon_id");
                //金额
                String  outTradeNo1= rspJson.getString("out_trade_no");
                //应答码
                String retcode = rspJson.getString("code");
                //支付时间
                String payTime = rspJson.getString("send_pay_date");
                //支付金额
                String totalAmount = rspJson.getString("total_amount");
                //交易状态
                String tradeStatus = rspJson.getString("trade_status");


                JSONObject jsonObject = new JSONObject();
                jsonObject.put("out_trade_no",outTradeNo1);
                jsonObject.put("retcode",retcode);
                jsonObject.put("retmsg", retmsg);
                jsonObject.put("pay_time", payTime);
                jsonObject.put("total_amount", totalAmount);
                jsonObject.put("trade_status", tradeStatus);
                jsonObject.put("buyer_logon_id", buyerLogonId);
                return jsonObject;
            }else {
                String msg = rspJson.getString("msg");
                logger.warn("[mall支付查询]查询交易失败:" + msg);
                throw new BizException(msg);
            }
        } else {
            throw new BizException("服务器异常!!!");
        }
    }


}
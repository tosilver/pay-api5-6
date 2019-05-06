package co.b4pay.api.service;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.utils.DateUtil;
import co.b4pay.api.common.utils.QRCodeUtil;
import co.b4pay.api.model.*;

import java.util.Date;

import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.model.base.AjaxResponse;
import co.b4pay.api.socket.SocketUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static co.b4pay.api.common.utils.DateUtil.now;
import static co.b4pay.api.socket.SocketConstants.SOCKET_MAP;

/**
 * 个人收款码Service
 * Created with IntelliJ IDEA
 * Created By AZain
 * Date: 2018-08-22
 * Time: 16:55
 */
@Service
@Transactional
public class CodePayService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(CodePayService.class);

    private String LOCAL_FIXED_CODE_PATH = "/var/data/fixedCode/";
    //private String LOCAL_FIXED_CODE_PATH = "C:\\Users\\AZain\\Desktop\\fixedCode\\";
//    private String LOCAL_UNFIXED_CODE_PATH = "/var/data/unFixedCode/";
    private String LOCAL_UNFIXED_CODE_PATH = "/Users/azain/Desktop/unFixedCode/";

    public static Map<Integer, LinkedMap> FIXED_CODE_URL_MAP = new ConcurrentHashMap<>();

    public static Map<Integer, LinkedMap> UNFIXED_CODE_URL_MAP = new ConcurrentHashMap<>();

    @Autowired
    private HmacSHA1Signature signature = new HmacSHA1Signature();

    /**
     * 固定金额收款码
     *
     * @param merchantId
     * @param params
     * @return
     * @throws BizException
     */
    public AjaxResponse executeWithFixed(Long merchantId, JSONObject params, HttpServletRequest request, Router router) throws BizException {

        BigDecimal amount = new BigDecimal(params.getString("amount"));

        Integer payWay = params.getInteger("payWay");

        String tradeNo = params.getString("tradeNo");


        Trade trade = tradeDao.findByMerchantIdAndAmount(amount, String.valueOf(merchantId), tradeNo);

        if (trade != null) {
            throw new BizException(String.format("[%s]商户有金额为[%s]的未支付订单,订单号为：%s", merchantId, trade.getTotalAmount(), trade.getMerchantOrderNo()));
        }

        //查找该商户的个人收款码
        LinkedMap phoneUrlMap = getCodeUrlListWithFixed(payWay, true);
        if (phoneUrlMap == null || phoneUrlMap.size() < 1) {
            throw new BizException(String.format("暂无[%s]支付可用个人收款码", payWay == 1 ? "微信" : "支付宝"));
        }
        String codeUrl = null;
        String phoneCode = null;
        synchronized (this) {
            Iterator iterator = phoneUrlMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entries = (Map.Entry) iterator.next();
                String key = (String) entries.getKey();
                Channel channel = channelDao.findByRouterAndName(router.getId(), key);
                if (channel == null || channel.getStatus() == -1) {
                    changeMapIndex(payWay, true);
                    return AjaxResponse.failure(String.format("[%s]商户暂无可用[%s]渠道", merchantId, key));
                }
                Map amountUrlMap = (Map) entries.getValue();
                if (amountUrlMap.containsKey(amount.toPlainString())) {
                    Map qrCodeTimeMap = (Map) amountUrlMap.get(amount.toPlainString());
                    Long time = (Long) qrCodeTimeMap.get("time");
                    String url = (String) qrCodeTimeMap.get("url");
                    Long now = new Date().getTime();
                    if (now - time > Long.parseLong(MainConfig.EXPIRY_TIME)) {
                        phoneCode = key;
                        codeUrl = url;
                        qrCodeTimeMap.put("time", now);
                        amountUrlMap.put(amount.toPlainString(), qrCodeTimeMap);
                        phoneUrlMap.put(key, amountUrlMap);
                        FIXED_CODE_URL_MAP.put(payWay, phoneUrlMap);
                        break;
                    }

                }
            }
        }
        logger.warn("phoneCode ->" + phoneCode);
        logger.warn("codeUrl ->" + codeUrl);
        if (codeUrl == null) {
            changeMapIndex(payWay, true);
            return AjaxResponse.failure(String.format("[%s]商户暂无可用[%s]个人收款码", merchantId, amount.toPlainString()));
        }

        changeMapIndex(payWay, true);

        trade = new Trade();
        trade.setMerchantId(merchantId);
        trade.setId(String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15)));
        trade.setMerchantOrderNo(tradeNo);//商户订单号
        trade.setChannelId(getChannel(phoneCode, router.getId()).getId());
        logger.warn("Channel Id ->" + trade.getChannelId());
        trade.setTime(System.currentTimeMillis());
        trade.setNotifyUrl(params.getString("notifyUrl"));
        trade.setTotalAmount(amount);
        trade.setAccountAmount(new BigDecimal(0));
        trade.setCostRate(new BigDecimal(0));
        trade.setPayCost(new BigDecimal(0));
        trade.setTradeState(0);
        trade.setStatus(1);
        trade.setServiceCharge(new BigDecimal(0));
        trade.setHeader(null);
        trade.setRequest(params.toJSONString());
        trade.setRemark(null);
        trade.setCreateTime(DateUtil.getTime());
        trade.setUpdateTime(DateUtil.getTime());
        tradeDao.save(trade);

        JobTrade jobTrade = new JobTrade();
        jobTrade.setId(trade.getId());
        jobTrade.setStatus(-1);
        jobTrade.setCount(0);
        jobTrade.setChannelType(ChannelType.QRCODE);
        jobTrade.setNotifyUrl(params.getString("notifyUrl"));
        jobTradeDao.save(jobTrade);

        logger.warn("fixed code trade ->" + JSONObject.toJSONString(trade));
        logger.warn(String.format("通知手机端订单生成,socket个数:%s ", SOCKET_MAP.size()));
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
                    returnJson.put("code", phoneCode + "001");
                    JSONObject tradeJSON = new JSONObject();
                    tradeJSON.put("tradeNo", trade.getMerchantOrderNo());
                    tradeJSON.put("amount", trade.getTotalAmount());
                    tradeJSON.put("merchantId", trade.getMerchantId());
                    tradeJSON.put("phoneCode", phoneCode);
                    tradeJSON.put("tradeStatus", trade.getTradeState());
                    tradeJSON.put("createTime", trade.getCreateTime().getTime());
                    tradeJSON.put("payWay", params.getInteger("payWay"));
                    tradeJSON.put("qrType", router.getId());
                    returnJson.put("data", tradeJSON);
                    socketUtil.sendData(returnJson.toJSONString() + "*");
                } catch (IOException e) {
                    logger.warn("订单生成，下发给apk失败：" + e.getMessage());
                    try {
                        socketIterator.remove();
                        socket.close();
                    } catch (IOException e1) {
                        logger.warn("关闭socket失败：" + e.getMessage());
                    }
                }
            }
        } else {
            logger.warn("订单生成，无socket端口，下发给apk失败。");
        }

        return AjaxResponse.success("codeUrl", codeUrl);
    }


    /**
     * 无固定金额收款码
     *
     * @param merchantId
     * @param params
     * @return
     * @throws BizException
     */
    public AjaxResponse executeWithUnFixed(Long merchantId, JSONObject params, Router router) throws BizException {
        logger.warn("executeWithUnFixed params:" + params.toJSONString());
        BigDecimal amount = new BigDecimal(params.getString("amount"));

        Integer payWay = params.getInteger("payWay");
        String merchantNotifyUrl = params.getString("notifyUrl");
        String tradeNo = params.getString("tradeNo");

        Trade trade = tradeDao.findByMerchantIdAndAmount(new BigDecimal(0), String.valueOf(merchantId), tradeNo);

        if (trade != null) {
            throw new BizException(String.format("[%s]商户有订单号为[%s]的未支付订单", merchantId, trade));
        }

        //查找该商户的个人收款码
        LinkedMap phoneUrlMap = getCodeUrlListWithFixed(payWay, false);
        if (phoneUrlMap == null || phoneUrlMap.size() < 1) {
            throw new BizException(String.format("暂无[%s]支付可用个人收款码", payWay == 1 ? "微信" : "支付宝"));
        }
        String codeUrl = null;
        String phoneCode = null;
        synchronized (this) {
            Iterator iterator = phoneUrlMap.entrySet().iterator();
            a:
            while (iterator.hasNext()) {
                Map.Entry entries = (Map.Entry) iterator.next();
                String key = (String) entries.getKey();
                Channel channel = channelDao.findByRouterAndName(router.getId(), key);
                if (channel == null || channel.getStatus() == -1) {
                    changeMapIndex(payWay, false);
                    return AjaxResponse.failure(String.format("[%s]商户暂无可用[%s]渠道", merchantId, key));
                }
                Map urlMap = (Map) entries.getValue();
                Iterator tempIterator = urlMap.entrySet().iterator();
                while (tempIterator.hasNext()) {
                    Map.Entry tempEntries = (Map.Entry) tempIterator.next();
                    Map qrCodeTimeMap = (Map) tempEntries.getValue();
                    Long time = (Long) qrCodeTimeMap.get("time");
                    String url = (String) qrCodeTimeMap.get("url");
                    Long now = new Date().getTime();
                    if (now - time > Long.parseLong(MainConfig.EXPIRY_TIME)) {
                        phoneCode = key;
                        codeUrl = url;
                        qrCodeTimeMap.put("time", now);
                        urlMap.put(tempEntries.getKey(), qrCodeTimeMap);
                        phoneUrlMap.put(key, urlMap);
                        UNFIXED_CODE_URL_MAP.put(payWay, phoneUrlMap);
                        break a;
                    }
                }
            }
        }
        logger.warn("phoneCode ->" + phoneCode);
        logger.warn("codeUrl ->" + codeUrl);

        changeMapIndex(payWay, false);
        if (codeUrl == null) {
            return AjaxResponse.failure("暂无可用个人收款码");
        }

        trade = new Trade();
        trade.setMerchantId(merchantId);
        trade.setId(String.format("%s%s", DateUtil.dateToStr(DateUtil.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(15)));
        trade.setMerchantOrderNo(tradeNo);//商户订单号
        trade.setChannelId(getChannel(phoneCode, router.getId()).getId());
        trade.setTime(System.currentTimeMillis());
        trade.setNotifyUrl(params.getString("notifyUrl"));
        trade.setTotalAmount(amount);
        trade.setAccountAmount(new BigDecimal(0));
        trade.setCostRate(new BigDecimal(0));
        trade.setPayCost(new BigDecimal(0));
        trade.setTradeState(0);
        trade.setStatus(1);
        trade.setServiceCharge(new BigDecimal(0));
        trade.setHeader(null);
        trade.setRequest(params.toJSONString());
        trade.setRemark(null);
        trade.setCreateTime(DateUtil.getTime());
        trade.setUpdateTime(DateUtil.getTime());
        tradeDao.save(trade);

        JobTrade jobTrade = new JobTrade();
        jobTrade.setId(trade.getId());
        jobTrade.setStatus(-1);
        jobTrade.setCount(0);
        jobTrade.setChannelType(ChannelType.QRCODE);
        jobTrade.setNotifyUrl(merchantNotifyUrl);
        jobTradeDao.save(jobTrade);

        logger.warn("unfixed code trade ->" + JSONObject.toJSONString(trade));
        logger.warn(String.format("通知手机端订单生成,socket个数:%s ", SOCKET_MAP.size()));
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
                    returnJson.put("code", phoneCode + "001");
                    JSONObject tradeJSON = new JSONObject();
                    tradeJSON.put("tradeNo", trade.getMerchantOrderNo());
                    tradeJSON.put("amount", trade.getTotalAmount());
                    tradeJSON.put("merchantId", trade.getMerchantId());
                    tradeJSON.put("phoneCode", phoneCode);
                    tradeJSON.put("tradeStatus", trade.getTradeState());
                    tradeJSON.put("createTime", trade.getCreateTime().getTime());
                    tradeJSON.put("payWay", params.getInteger("payWay"));
                    tradeJSON.put("qrType", router.getId());
                    returnJson.put("data", tradeJSON);
                    socketUtil.sendData(returnJson.toJSONString() + "*");
                } catch (IOException e) {
                    logger.warn("订单生成，下发给apk失败：" + e.getMessage());
                    try {
                        socketIterator.remove();
                        socket.close();
                    } catch (IOException e1) {
                        logger.warn("关闭socket失败：" + e.getMessage());
                    }
                }
            }
        } else {
            logger.warn("订单生成，无socket端口，下发给apk失败。");
        }
        return AjaxResponse.success("codeUrl", codeUrl);
    }

    /**
     * 获取固定额度收款码url信息
     *
     * @param payWay  支付方式
     * @param isFixed 是否固定金额
     * @return
     */
    private LinkedMap getCodeUrlListWithFixed(Integer payWay, boolean isFixed) {
        LinkedMap phoneUrlMap = new LinkedMap();
        if (isFixed && FIXED_CODE_URL_MAP.containsKey(payWay)) {
            phoneUrlMap = FIXED_CODE_URL_MAP.get(payWay);
        } else if (!isFixed && UNFIXED_CODE_URL_MAP.containsKey(payWay)) {
            phoneUrlMap = UNFIXED_CODE_URL_MAP.get(payWay);
        } else if ((isFixed && FIXED_CODE_URL_MAP.size() == 0) ||
                (!isFixed && UNFIXED_CODE_URL_MAP.size() == 0)) {
            String tempPath = payWay + System.getProperty("file.separator");//支付方式
            String realPath;

            if (isFixed) {
                realPath = LOCAL_FIXED_CODE_PATH + tempPath;
            } else {
                realPath = LOCAL_UNFIXED_CODE_PATH + tempPath;
            }
            logger.warn("local code path ->" + realPath);
            File localPathFile = new File(realPath);
            File[] phoneFiles = localPathFile.listFiles();
            if (phoneFiles != null) {
                for (int i = 0; i < phoneFiles.length; i++) {
                    if (phoneFiles[i].isDirectory()) {
                        String phoneCode = phoneFiles[i].getName();
                        File[] qrFiles = phoneFiles[i].listFiles();
                        if (qrFiles != null) {
                            Map<String, Object> amountUrlMap = new HashMap<>();
                            for (int j = 0; j < qrFiles.length; j++) {
                                if (qrFiles[j].isFile()) {
                                    Map<String, Object> qrCodeTimeMap = new HashMap<>();
                                    qrCodeTimeMap.put("time", new Date().getTime() - Long.parseLong(MainConfig.EXPIRY_TIME));
                                    try {
                                        qrCodeTimeMap.put("url", QRCodeUtil.decode(qrFiles[j]));
                                        if (isFixed) {
                                            amountUrlMap.put(qrFiles[j].getName().substring(0, qrFiles[j].getName().lastIndexOf(".")),
                                                    qrCodeTimeMap);
                                        } else {
                                            amountUrlMap.put(qrFiles[j].getName(), qrCodeTimeMap);
                                        }
                                    } catch (Exception e) {
                                        logger.warn(qrFiles[j].getAbsolutePath() + "二维码文件解析错误");
                                    }
                                }
                            }
                            phoneUrlMap.put(phoneCode, amountUrlMap);
                        }
                    }
                }
            }
            if (isFixed) {
                FIXED_CODE_URL_MAP.put(payWay, phoneUrlMap);
            } else {
                UNFIXED_CODE_URL_MAP.put(payWay, phoneUrlMap);
            }

        }
        return phoneUrlMap;
    }

    private synchronized void changeMapIndex(Integer key, boolean isFixed) {
        LinkedMap linkedMap;
        if (isFixed) {
            linkedMap = FIXED_CODE_URL_MAP.get(key);
        } else {
            linkedMap = UNFIXED_CODE_URL_MAP.get(key);
        }
        Iterator iterator = linkedMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entries = (Map.Entry) iterator.next();
            String mapKey = (String) entries.getKey();
            Map mapValue = (Map) entries.getValue();
            linkedMap.remove(mapKey, mapValue);
            linkedMap.put(mapKey, mapValue);
            if (isFixed) {
                FIXED_CODE_URL_MAP.put(key, linkedMap);
            } else {
                UNFIXED_CODE_URL_MAP.put(key, linkedMap);
            }
            break;
        }
    }


    public String fixedCodePayNotify(Socket socket, String receiveData) {
        JSONObject returnJson = new JSONObject();
        returnJson.put("code", "-1");
        try {
            if (StringUtils.isNotBlank(receiveData)) {
                JSONObject receiveJson = JSONObject.parseObject(receiveData);
                logger.warn("socket fixedCodePayNotify parameters ->" + receiveJson.toJSONString());
                String code = receiveJson.getString("code");
                String data = receiveJson.getString("data");
                if (StringUtils.isNotBlank(data) && StringUtils.isNotBlank(code)) {
                    JSONObject dataJson = JSON.parseObject(data);
                    String merchantId = dataJson.getString("merchantId");
                    String amount = dataJson.getString("amount");
                    String tradeNo = dataJson.getString("tradeNo");
                    String phoneCode = dataJson.getString("phoneCode");
//                    String qrType = dataJson.getString("qrType");
                    Channel channel = channelDao.findByName(phoneCode);
                    if (channel == null) {
                        returnJson.put("msg", String.format("[%s]渠道异常", phoneCode));
                    } else {
                        Trade trade = tradeDao.findByNoAmountAndChannel(tradeNo, new BigDecimal(amount), merchantId, channel.getId().toString());
                        logger.warn(String.format("查询的参数：[%s],[%s],[%s]", tradeNo, merchantId, channel.getId().toString()));
                        logger.warn("查询到的单据：" + JSONObject.toJSONString(trade));
                        if (trade == null) {
                            returnJson.put("msg", String.format("[%s]商户号，订单号为[%s]的订单信息不存在", merchantId, tradeNo));
                        } else {
                            trade.setStatus(1);
                            if (now().getTime() - channel.getLastFailTime().getTime() < Long.valueOf(MainConfig.CHANCEL_ROTATION_TIME)) {
                                trade.setTradeState(-3);
                            } else {
                                trade.setTradeState(1);
                                trade.setTotalAmount(new BigDecimal(amount));
                                trade.setAccountAmount(new BigDecimal(amount));
                            }
                            tradeDao.save(trade);
                            Merchant merchant = merchantService.findById(Long.valueOf(merchantId));
                            JSONObject returnJsonToMerchant = new JSONObject();
                            if (merchant != null) {
                                //调度类参数
                                returnJsonToMerchant.put("tradeNo", trade.getMerchantOrderNo());
                                returnJsonToMerchant.put("amount", trade.getTotalAmount().toPlainString());
                                returnJsonToMerchant.put("tradeState", String.valueOf(trade.getTradeState()));
                                returnJsonToMerchant.put("merchantId", merchantId);
                                returnJsonToMerchant.put("payTime", String.valueOf(trade.getUpdateTime().getTime()));
                                String content = SignatureUtil.getSignatureContent(returnJsonToMerchant, true);
                                String sign = signature.sign(content, merchant.getSecretKey(), Constants.CHARSET_UTF8);
                                returnJsonToMerchant.put("signature", sign);
                            }
                            //下发给下游
                            //try {
                            //    logger.warn("notify url ->" + trade.getNotifyUrl());
                            //    logger.warn("notify data ->" + returnJsonToMerchant.toJSONString());
                            //    HttpsUtils.post(trade.getNotifyUrl(), null, returnJsonToMerchant.toJSONString());
                            //} catch (Exception e) {
                            //    logger.error("下发下游失败："+e.getMessage(),e);
                            //}
                            //更新调度类
                            try {
                                JobTrade jobTrade = jobTradeService.findById(trade.getId());
                                if (jobTrade != null) {
                                    jobTrade.setStatus(0);
                                    jobTrade.setExecTime(DateUtil.getTime());
                                    jobTrade.setContent(returnJsonToMerchant.toJSONString());
                                    jobTradeService.save(jobTrade);
                                }
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                            if (trade.getTradeState() == 1) {
                                logger.warn("支付成功！！！");
                                //释放渠道的二维码
                                LinkedMap phoneUrlMap = null;
                                if ("fixedCodePay".equals(channel.getRouter().getId())) {
                                    phoneUrlMap = FIXED_CODE_URL_MAP.get(channel.getProduct());
                                } else if ("unFixedCodePay".equals(channel.getRouter().getId())) {
                                    phoneUrlMap = UNFIXED_CODE_URL_MAP.get(channel.getProduct());
                                }
                                if (phoneUrlMap != null) {
                                    Map urlMap = (Map) phoneUrlMap.get(phoneCode);
                                    Iterator tempIterator = urlMap.entrySet().iterator();
                                    while (tempIterator.hasNext()) {
                                        Map.Entry tempEntries = (Map.Entry) tempIterator.next();
                                        Map qrCodeTimeMap = (Map) tempEntries.getValue();
                                        Long now = new Date().getTime();
                                        qrCodeTimeMap.put("time", now - Long.parseLong(MainConfig.EXPIRY_TIME));
                                        urlMap.put(tempEntries.getKey(), qrCodeTimeMap);
                                        phoneUrlMap.put(phoneCode, urlMap);
                                        UNFIXED_CODE_URL_MAP.put(2, phoneUrlMap);
                                        break;
                                    }
                                }
                            }
                            //下发给apk
                            JSONObject returnJsonToAPK = new JSONObject();
                            returnJsonToAPK.put("tradeNo", trade.getMerchantOrderNo());
                            returnJsonToAPK.put("amount", trade.getTotalAmount());
                            returnJsonToAPK.put("tradeStatus", trade.getTradeState());
                            returnJsonToAPK.put("merchantId", trade.getMerchantId());
                            returnJsonToAPK.put("phoneCode", phoneCode);
//                            returnJsonToAPK.put("qrType",qrType);
                            returnJson.put("code", phoneCode + "003");
                            returnJson.put("data", returnJsonToAPK.toJSONString());
                        }
                    }

                } else if ("000".equals(code)) {
                    returnJson.put("code", "000");
                    returnJson.put("ip", socket.getInetAddress().getHostAddress());
                } else {
                    returnJson.put("msg", String.format("编号为[%s]的Socket，携带的data数据为空", code));
                }

            } else {
                returnJson.put("msg", "Socket消息体为空");
            }
        } catch (Exception e) {
            logger.error(e.toString());
            returnJson.put("msg", String.format("系统异常，原因：[%s]", e.getMessage()));
        }
        return returnJson.toJSONString() + "*";
    }

}

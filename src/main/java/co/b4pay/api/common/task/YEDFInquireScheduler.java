package co.b4pay.api.common.task;


import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.exception.BizException;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.zengutils.HCMD5;
import co.b4pay.api.common.zengutils.HttpClientUtil;
import co.b4pay.api.dao.MerchantDao;
import co.b4pay.api.dao.YEDFPayrollDao;
import co.b4pay.api.model.Merchant;
import co.b4pay.api.model.YEDFPayroll;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.zxing.WriterException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Component
public class YEDFInquireScheduler {
    private static final Logger logger = LoggerFactory.getLogger(YEDFInquireScheduler.class);

    private static final String kJPAY_API_DOMAIN = MainConfig.getConfig("kJPAY_API_DOMAIN");

    private HmacSHA1Signature signature = new HmacSHA1Signature();


    @Autowired
    private YEDFPayrollDao yedfPayrollDao;

    @Autowired
    private MerchantDao merchantDao;


    @Scheduled(cron = "* */6 * * * ?") // 每6分钟
    public void OrderInquire() {
        List<YEDFPayroll> byTradeState = yedfPayrollDao.findByTradeState(0);
        if (byTradeState !=null){
        for (YEDFPayroll yedfPayroll : byTradeState) {
            String merchantOrderNo = yedfPayroll.getMerchantOrderNo();
            String payOrderNo = yedfPayroll.getPayOrderNo();
            Date createTime = yedfPayroll.getCreateTime();
            Long merchantId = yedfPayroll.getMerchantId();
            BigDecimal totalAmount = yedfPayroll.getTotalAmount();
            try {
                ordernquiry(merchantOrderNo, payOrderNo, createTime.toString(),merchantId,totalAmount );
            } catch (IOException e) {
                e.printStackTrace();
            } catch (WriterException e) {
                e.printStackTrace();
            }

        }
        }
    }

    public JSONObject ordernquiry(String merchantOrderNo, String payOrderNo, String createTime,long merchantId,BigDecimal totalAmount) throws IOException, WriterException {
        //商户密钥
        String secretKey = "91c9fa29a1534e4b95555cf7d265e570";

        //封装查询所需要的参数
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("version", "V001");
        paramMap.put("agre_type", "Q");
        paramMap.put("inst_no", "10000094");
        paramMap.put("merch_id", "100000941000167");
        paramMap.put("merch_order_no",payOrderNo);
        paramMap.put("platform_order_no", merchantOrderNo);
        paramMap.put("query_id", "1");
        paramMap.put("order_datetime", createTime);
        //生成签名
        String sign = this.getPerSign(paramMap, secretKey);
        paramMap.put("sign", sign);
        //转换成JSON字符串
        String jsonString = JSONObject.toJSONString(paramMap);
        logger.warn("查询JSON字符串为:" + jsonString);
        // 发送post请求
        HttpClientUtil httpClientUtil = new HttpClientUtil();
        // 返回数据
        logger.warn("定时查询请求开始:");
        String result = httpClientUtil.doPost(kJPAY_API_DOMAIN, jsonString, "utf-8");
        logger.warn("定时查询请求结束!!!!!!!");
        HashMap<String, String> resultInfo = getResultInfo(result, jsonString, secretKey, kJPAY_API_DOMAIN);
        String responseCreateOrder = resultInfo.get("responseCreateOrder");
        System.out.println("[定时余额查询]查询应答报文:  " + responseCreateOrder);
        JSONObject rspJson = JSONObject.parseObject(responseCreateOrder);
        logger.warn("[定时余额查询]查询应答报文转为json字符串" + rspJson);
        BigDecimal deductedAmount = totalAmount.add(new BigDecimal("3.00"));
        if (rspJson != null) {

            if (StringUtils.isNotBlank(rspJson.getString("retcode")) && "00".equals(rspJson.getString("retcode"))) {
                logger.warn("定时查询结果为成功");
                YEDFPayroll yedfPayroll=yedfPayrollDao.findByMerchantOrderNo(merchantOrderNo);
                //从冻结金额减去已经成功代付的金额
                Merchant merchant1 = merchantDao.getOne(merchantId);
                BigDecimal accountFrozen1 = merchant1.getAccount_frozen();
                logger.warn("现在的冻结金额为:" + accountFrozen1);
                merchant1.setAccount_frozen(accountFrozen1.subtract(deductedAmount));
                merchantDao.save(merchant1);
                logger.warn("交易成功,从冻结金额中减去代付金额!" + deductedAmount);
                yedfPayroll.setTradeState(1);
                yedfPayrollDao.save(yedfPayroll);
                return rspJson;
            } else if (StringUtils.isNotBlank(rspJson.getString("retcode")) && "99".equals(rspJson.getString("retcode"))) {
                logger.warn("定时查询结果为未支付");
                return rspJson;
            } else {
                logger.warn("定时查询结果为失败");
                YEDFPayroll yedfPayroll=yedfPayrollDao.findByMerchantOrderNo(merchantOrderNo);
                yedfPayroll.setTradeState(-1);
                yedfPayrollDao.save(yedfPayroll);
                return rspJson;
            }
        } else {
            throw new BizException("服务器异常!!!");
        }
    }

    /**
     * 生成签名前数据处理，并生成签名
     */
    public String getPerSign(HashMap<String, String> map, String secretKey) {
        // 获取非空的数据放入signMap    按字典序排序
        TreeMap<String, String> signMap = new TreeMap<String, String>();
        for (String key1 : map.keySet()) {
            String str = (String) map.get(key1);
            // 对于空的数据或者为空字符串的数据不参与签名
            if (str != null && str.length() > 0) {
                signMap.put(key1, str);
            }
        }
        return getSign(signMap, secretKey);
    }

    /**
     * 生成签名
     */
    public String getSign(Map<String, String> map, String secretKey) {
        // 移除前端多传的数据，这个根据系统的情况来定，如果没有则不需要移除，最终只保留文档上且传进来非空的数据
        map.remove("requestType");
        map.remove("secretKey");
        map.remove("tradeNo");
        map.remove("subject");
        map.remove("body");

        StringBuffer str = new StringBuffer();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            str.append(entry.getKey() + "=" + entry.getValue() + "&");
        }
        // 商户秘钥拼接
        str.append("key=" + secretKey);
        // MD5加密
        String sign = HCMD5.MD5(str.toString(), "utf-8").toLowerCase();
        logger.info("生成签名数据 :" + str.toString() + " \n生成的签名 :" + sign);
        return sign;
    }


    /***
     * 对服务器返回数据进行验签，然后生成二维码返回给前端
     *
     * @param result
     *            返回数据
     * @param jsonParam
     *            请求数据
     * @param secretKey
     *            秘钥
     * @param url
     *            请求URL
     * @return map 返回给浏览器
     * @throws WriterException
     * @throws IOException
     */
    public HashMap<String, String> getResultInfo(String result, String jsonParam, String secretKey, String url)
            throws WriterException, IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("requestCreateOrder", jsonParam + "请求Url:  " + url);
        if (result == null) {
            map.put("responseCreateOrder", "请求地址不正确");
        } else {
            // 将返回数据转换为map
            Map<?, ?> resultMap = (Map<?, ?>) JSON.parse(result);
            @SuppressWarnings("unchecked")
            Map<String, String> perMap = (Map<String, String>) resultMap;
            //封装参与签名的数据放到resignMap   按字典序排序
            TreeMap<String, String> resignMap = new TreeMap<String, String>();
            logger.info("[返回数据resultMap]:" + resultMap.toString());
            for (String key : perMap.keySet()) {
                String value = resultMap.get(key).toString();
                if (value != null && value.length() > 0)
                    resignMap.put(key, value);
            }
            resignMap.remove("sign");
            String resign = getSign(resignMap, secretKey);// 返回数据也需要验签
            if (resign.equals(resultMap.get("sign").toString())) {// 判断返回签名和本地签名是否一致
                map.put("responseCreateOrder", result);
            } else {
                map.put("responseCreateOrder", "返回数据验签失败");
            }
        }
        return map;
    }

}

package co.b4pay.api.paytest;

import co.b4pay.api.common.utils.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝扫码支付
 */
public class PayTester extends BaseTester {

    public static void main(String args[]) {
        try {
            Map<String, String> params = new HashMap<>();
            Calendar calendar = Calendar.getInstance();
            String tradeNo = String.format("%s%s", DateUtil.dateToStr(calendar.getTime(), DateUtil.YMdhmsS_noSpli), RandomStringUtils.randomNumeric(4));
            System.out.println("订单号:::" + tradeNo);

            // 业务参数
            params.put("tradeNo", tradeNo);
            params.put("subject", "xxx品牌xxx门店当面付扫码消费");
            params.put("totalAmount", "501");
            //params.put("notifyUrl", "http://112.213.118.86:9988/sHPayNotify.do");
            params.put("notifyUrl", "http://112.213.118.86:9988/sHPayNotify.do");
            params.put("body", "购买商品3件共20.00元");
            //JSONObject jsonObject = JSONObject.parseObject(execute("/pay/grPay.do", params));
            JSONObject jsonObject = JSONObject.parseObject(execute("/pay/aliSPay.do", params));
            System.out.println(jsonObject);
            System.out.println(JSON.parseObject(jsonObject.getString("data")).getString("qr_code").replace("\\", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

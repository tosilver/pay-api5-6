package co.b4pay.api.common.zengutils;

import java.util.Random;
import java.util.UUID;

/**
 * ID生成器
 * @author zhongzf
 * @date 2015年11月2日
 */
public class ESIDGenerate {

    //纯数字验证码
    public static final String VERIFY_CODES_INT = "0123456789";

    /**
     * 生成32位UUID
     * @return
     */
    public static String getUUID(){
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String nextTradeNo(){
        return "T-" + ESDate.getDateNumber() + String.valueOf(ESIDWorker.getNext()).substring(5);
    }

    /**
     * 下一个订单号
     * @return
     */
    public static String nextOrderNo(){
        return "D-" + ESDate.getDateNumber() + String.valueOf(ESIDWorker.getNext()).substring(5);
    }

    /**
     * 生成纯数字验证码
     * @param length 生成验证码的长度
     * @return
     */
    public static String getIntRandomCode(int length){
        int codesLen = VERIFY_CODES_INT.length();
        Random rand = new Random(System.currentTimeMillis());
        StringBuilder verifyCode = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            verifyCode.append(VERIFY_CODES_INT.charAt(rand.nextInt(codesLen - 1)));
        }
        return verifyCode.toString();
    }

    public static void main(String[] args) {
        System.out.println(ESIDGenerate.nextOrderNo());
        System.out.println(ESIDGenerate.nextTradeNo());
    }
}

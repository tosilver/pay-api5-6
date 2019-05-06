/**
 * Hola.YK Inc.
 * Copyright (c) 2012-2015 All Rights Reserved.
 */
package co.b4pay.api.common.signature;

import co.b4pay.api.common.constants.Constants;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SignatureException;

/**
 * HMACSHA1签名类
 * <p>
 * &nbsp;&nbsp;Hash Message Authentication Code ，利用哈希算法，以一个密钥和一个消息为输入，生成一个消息摘要作为输出
 * </p>
 *
 * @author YK
 * @version $Id: HmacSHA1Signature.java, v 0.1 2015年1月16日 下午4:28:43 YK Exp $
 */
@Component
public class HmacSHA1Signature extends BaseSignature {
    /**
     * HMACSHA1算法名
     */
    private static final String HMAC_SHA1 = "HMACSHA1"; // HmacSHA1

    /**
     * @see BaseSignature#doCheck(String, String,
     * String, String)
     */
    @Override
    boolean doCheck(String content, String signature, String publicKey, String charset) throws SignatureException {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(publicKey.getBytes(), HMAC_SHA1);
            Mac mac = Mac.getInstance(signingKey.getAlgorithm());
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(getContentBytes(content, charset));
            // 不区分大小写
            return signature.equalsIgnoreCase(bytes2HexString(rawHmac));
        } catch (Exception e) {
            throw new SignatureException("HMACSHA1验证签名[content = " + content + "; charset = " + charset + "; signature = " + signature + "]发生异常!", e);
        }
    }

    /**
     * @see BaseSignature#doSign(String, String,
     * String)
     */
    @Override
    String doSign(String content, String privateKey, String charset) throws SignatureException {
        try {
            // 还原密钥
            SecretKey secretKey = new SecretKeySpec(privateKey.getBytes(), HMAC_SHA1);
            // 实例化Mac
            Mac mac = Mac.getInstance(secretKey.getAlgorithm());
            // 初始化mac
            mac.init(secretKey);
            // 执行消息摘要
            byte[] rawHmac = mac.doFinal(getContentBytes(content, charset));
            return bytes2HexString(rawHmac);
        } catch (Exception e) {
            throw new SignatureException("HMACSHA1签名[content = " + content + "; charset = " + charset + "]发生异常!", e);
        }
    }

    /**
     * 把字节数组转换为16进制的形式
     *
     * @param b 二进制参数
     * @return xxx
     */
    private String bytes2HexString(byte[] b) {
        String ret = "";
        for (byte $b : b) {
            String hex = Integer.toHexString($b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex;
        }
        return ret;
    }

    public static void main(String[] args) {
        HmacSHA1Signature sha1Signature = new HmacSHA1Signature();
        String key = "fccce21e-15b7-4ad3-86a3-75ba66a9ceb1";
        String content1 = "brand=%E6%8A%8A%E4%BD%A0%E9%82%A3%E5%8F%A5%E5%B0%86%E8%AE%A1%E5%B0%B1%E8%AE%A1&company=%E5%85%AB%E4%BD%B0%E4%BC%B4&contacts=%E5%93%BC%E5%93%BC%E5%94%A7%E5%94%A7&idCard=http://7xsofu.com1.z0.glb.clouddn.com/authenticationIDCard4020170323174635.png&license=http://7xsofu.com1.z0.glb.clouddn.com/authenticationLicense4020170323174625.png&nature=%E4%BA%8C%E6%89%8B%E8%BD%A6%E5%95%86&tel=13099998884&timestamp=1490262399000&uid=40";
        String content2 = "brand=%E6%8A%8A%E4%BD%A0%E9%82%A3%E5%8F%A5%E5%B0%86%E8%AE%A1%E5%B0%B1%E8%AE%A1&company=%E5%85%AB%E4%BD%B0%E4%BC%B4&contacts=%E5%93%BC%E5%93%BC%E5%94%A7%E5%94%A7&idCard=http%3A%2F%2F7xsofu.com1.z0.glb.clouddn.com%2FauthenticationIDCard4020170323174635.png&license=http%3A%2F%2F7xsofu.com1.z0.glb.clouddn.com%2FauthenticationLicense4020170323174625.png&nature=%E4%BA%8C%E6%89%8B%E8%BD%A6%E5%95%86&tel=13099998884&timestamp=1490262581000&uid=40";
        try {
//           Map<String, String> param = new HashMap<>();
//           param.put("timestamp", String.valueOf(System.currentTimeMillis()));
//           param.put("uid", "550");
//           param.put("orderId","200550170321213909");
//           String content = SignatureUtil.getSignatureContent(param, true);
            String sign = sha1Signature.sign(content1, key, Constants.CHARSET_UTF8);
            System.out.println(sign);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * Hola.YK Inc.
 * Copyright (c) 2012-2015 All Rights Reserved.
 */
package co.b4pay.api.signature;

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
 * @version $Id: HmacSHA1Signature.java, v 0.1 2015年1月16日 下午4:28:43 YiKe Exp $
 */
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
}

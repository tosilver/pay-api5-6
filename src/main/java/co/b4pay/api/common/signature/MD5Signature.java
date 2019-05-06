/**
 * Hola.YK Inc.
 * Copyright (c) 2012-2015 All Rights Reserved.
 */
package co.b4pay.api.common.signature;

import java.security.MessageDigest;
import java.security.SignatureException;

/**
 * @author YK
 * @version $Id: MD5Signature.java, v 0.1 2015年1月16日 下午4:29:52 YK Exp $
 */
public class MD5Signature extends BaseSignature {
    /**
     * SHA-1算法名
     */
    private static final String MD5 = "MD5"; // MD5

    @Override
    boolean doCheck(String content, String signature, String publicKey, String charset) throws SignatureException {
        try {
            // 不区分大小写
            return signature.equalsIgnoreCase(doSign(content, publicKey, charset));
        } catch (Exception e) {
            throw new SignatureException("MD5验证签名[content = " + content + "; charset = " + charset + "; signature = " + signature + "]发生异常!", e);
        }
    }

    @Override
    String doSign(String content, String privateKey, String charset) throws SignatureException {
        try {
            MessageDigest digest = MessageDigest.getInstance(MD5);
            digest.update(getContentBytes(content, charset));
            byte messageDigest[] = digest.digest();

            return bytes2HexString(messageDigest);
        } catch (Exception e) {
            throw new SignatureException("MD5签名[content = " + content + "; charset = " + charset + "]发生异常!", e);
        }
    }

    /**
     * 字节数组转换为 十六进制 数
     *
     * @param b
     * @return
     */
    private String bytes2HexString(byte[] b) {
        // Create Hex String
        StringBuffer hexString = new StringBuffer();
        // 字节数组转换为 十六进制 数
        for (int i = 0; i < b.length; i++) {
            String shaHex = Integer.toHexString(b[i] & 0xFF);
            if (shaHex.length() < 2) {
                hexString.append(0);
            }
            hexString.append(shaHex);
        }
        return hexString.toString();
    }
}

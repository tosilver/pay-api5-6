/**
 * Hola.YK Inc.
 * Copyright (c) 2012-2015 All Rights Reserved.
 */
package co.b4pay.api.signature;

import java.security.SignatureException;

/**
 * 签名工具
 *
 * @author YK
 * @version $Id: Signature.java, v 0.1 2015年1月16日 下午4:17:37 YiKe Exp $
 */
public interface Signature {

    /**
     * 校验签名
     *
     * @param content   签名内容
     * @param signature 签名字符串
     * @param publicKey 签名公钥
     * @param charset   签名编码
     * @return
     * @throws SignatureException
     */
    boolean check(String content, String signature, String publicKey, String charset) throws SignatureException;

    /**
     * 签名
     *
     * @param content    签名内容
     * @param privateKey 签名私钥
     * @param charset    签名编码
     * @return
     * @throws SignatureException
     */
    String sign(String content, String privateKey, String charset) throws SignatureException;

}

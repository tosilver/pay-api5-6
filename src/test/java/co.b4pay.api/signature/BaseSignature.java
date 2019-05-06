/**
 * Hola.YK Inc.
 * Copyright (c) 2012-2015 All Rights Reserved.
 */
package co.b4pay.api.signature;


import co.b4pay.api.utils.StrKit;

import java.io.UnsupportedEncodingException;
import java.security.SignatureException;

/**
 * 签名基类
 *
 * @author YK
 * @version $Id: BaseSignature.java, v 0.1 2015年1月16日 下午4:29:02 YiKe Exp $
 */
abstract class BaseSignature implements Signature {

    /**
     * @see Signature#check(String, String, String,
     * String)
     */
    @Override
    public boolean check(String content, String signature, String publicKey, String charset) throws SignatureException {
        if (StrKit.isBlank(content)) {
            throw new SignatureException("内容为空!");
        }

        if (publicKey == null) {
            throw new SignatureException("公钥为空!");
        }

        if (StrKit.isBlank(signature)) {
            throw new SignatureException("签名为空!");
        }

        return doCheck(content, signature, publicKey, charset);
    }

    /**
     * @see Signature#sign(String, String, String)
     */
    @Override
    public String sign(String content, String privateKey, String charset) throws SignatureException {
        if (StrKit.isBlank(content)) {
            throw new SignatureException("内容为空!");
        }

        if (privateKey == null) {
            throw new SignatureException("私钥为空!");
        }

        return doSign(content, privateKey, charset);
    }

    /**
     * 验证签名
     *
     * @param content   原始数据
     * @param signature 签名数据
     * @param publicKey 公钥
     * @param charset   编码集
     * @return True 签名验证通过 False 签名验证失败
     */
    abstract boolean doCheck(String content, String signature, String publicKey, String charset) throws SignatureException;

    /**
     * 使用privateKey对原始数据进行签名
     *
     * @param content    原始数据
     * @param privateKey 私钥
     * @param charset    编码集
     * @return 签名数据
     */
    abstract String doSign(String content, String privateKey, String charset) throws SignatureException;

    /**
     * @param content
     * @param charset
     * @return
     * @throws SignatureException
     * @throws UnsupportedEncodingException
     */
    protected byte[] getContentBytes(String content, String charset) throws UnsupportedEncodingException {
        if (StrKit.isBlank(charset)) {
            return content.getBytes();
        }
        return content.getBytes(charset);
    }
}

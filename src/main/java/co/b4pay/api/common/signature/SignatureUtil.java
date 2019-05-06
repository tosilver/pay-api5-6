/**
 * Hola.YK Inc.
 * Copyright (c) 2012-2018 All Rights Reserved.
 */
package co.b4pay.api.common.signature;

import co.b4pay.api.common.utils.StringUtil;
import co.b4pay.api.common.utils.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 签名工具类。
 *
 * @author YK
 * @version $Id: SignatureUtil.java, v 0.1 2015年1月16日 下午4:46:42 YK Exp $
 */
public class SignatureUtil {
    /**
     * 空字符串对象
     */
    public static final String EMPTY = "";
    /**
     * UTF-8字符类型
     */
    public static final String CHARSET_UTF8 = "UTF-8";

    /**
     * <p>
     * 根据MAP获得需要签名、验签的内容。
     * </p>
     * <p>
     * 按照参数名字首母升序的顺序a->z排序，并将所有参数用&连接起来，且将所有参数值进行编码。
     * </p>
     *
     * @param params 签名参数
     * @param escape 是否URL转义
     * @return 签名内容
     */
    public static String getSignatureContent(final Map<String, Object> params, final boolean escape) throws UnsupportedEncodingException {
        if (params == null) {
            return null;
        }
        final StringBuilder content = new StringBuilder();
        final List<String> keys = new ArrayList<String>(params.keySet());
        Collections.sort(keys); // 首字母 a->z 排序
        for (int i = 0, size = keys.size(); i < size; i++) {
            final String key = keys.get(i);
            final String value = defaultString(params.get(key));
            if (StringUtil.isBlank(value)) {
                continue;
            }
            if (content.length() > 0) {
                content.append("&");
            }
            content.append(key);
            content.append("=");
            // 将参数值编码
            content.append(escape ? URLEncoder.encode(value, CHARSET_UTF8) : value);
        }
        return content.toString();
    }

    /**
     * <p>Returns either the passed in String,
     * or if the String is {@code null}, an empty String ("").</p>
     *
     * <pre>
     * StringUtils.defaultString(null)  = ""
     * StringUtils.defaultString("")    = ""
     * StringUtils.defaultString("bat") = "bat"
     * </pre>
     *
     * @param str the String to check, may be null
     * @return the passed in String, or the empty String if it
     * was {@code null}
     */
    private static String defaultString(final Object str) {
        if (str == null) {
            return EMPTY;
        }
        return (String) str;
    }
}

/**
 * Hola.YK Inc. Copyright (c) 2012-2015 All Rights Reserved.
 */
package co.b4pay.api.common.tenpay.util;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author YK
 * @version $Id: ServletUtil.java, v 0.1 2015-8-19 下午8:50:48 YK Exp $
 */
public class ServletUtil {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * 获取请求字符串
     *
     * @param request
     * @return
     */
    public static String getQueryString(HttpServletRequest request) {
        return getQueryString(request, true, EMPTY_STRING_ARRAY);
    }

    public static String getQueryString(HttpServletRequest request, boolean sort, String... ignoreParamNames) {
        StringBuffer sb = new StringBuffer();
        @SuppressWarnings("unchecked")
        List<String> paramNames = EnumerationUtils.toList(request.getParameterNames());
        if (!ArrayUtils.isEmpty(ignoreParamNames)) {
            List<String> ignoreParamNameList = Arrays.asList(ignoreParamNames);
            paramNames.removeAll(ignoreParamNameList);
        }
        if (sort) {
            Collections.sort(paramNames);// 参数名排序
        }
        for (int i = 0, size = paramNames.size(); i < size; i++) {
            String paramName = paramNames.get(i);
            String paramValue = request.getParameter(paramName);
            if (i > 0) {
                sb.append("&");
            }
            sb.append(paramName).append("=").append(paramValue);
        }

        return sb.toString();
    }

    public static String getIpAddress(HttpServletRequest request) {
        String localIP = "127.0.0.1";
        if (request == null) {
            return localIP;
        }
        String ip = request.getHeader("x-forwarded-for");
        if ((ip == null) || (ip.length() == 0) || (ip.equalsIgnoreCase(localIP)) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if ((ip == null) || (ip.length() == 0) || (ip.equalsIgnoreCase(localIP)) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if ((ip == null) || (ip.length() == 0) || (ip.equalsIgnoreCase(localIP)) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            return localIP;
        }
        return ip;
    }
}

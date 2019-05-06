package co.b4pay.api.common.utils;

import co.b4pay.api.common.constants.Constants;
import org.apache.commons.lang3.ArrayUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static co.b4pay.api.common.config.MainConfig.API_NOTIFY_URL;
import static co.b4pay.api.common.config.MainConfig.GR_ORDER_URL;
import static co.b4pay.api.common.config.MainConfig.TRANSIN;

public class WebUtil {
    private static final String METHOD_GET = "GET";

    /**
     * 判断是否GET请求
     *
     * @param request
     * @return
     */
    public static boolean isGet(HttpServletRequest request) {
        return METHOD_GET.equals(request.getMethod());
    }

    public static Map<String, Object> convertQueryStringToMap(String queryString) {
        if (queryString == null || queryString.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        String[] splits = queryString.split("&");
        if (splits == null || splits.length == 0) {
            return Collections.emptyMap();
        }

        Map<String, Object> map = new HashMap<String, Object>(splits.length);
        for (String kv : splits) {
            String[] kvs = kv.split("=");
            map.put(kvs[0], kvs[1]);
        }

        return map;
    }

    /**
     * 获取请求字符串
     *
     * @param request
     * @return
     */
    public static String getQueryString(HttpServletRequest request) {
        return getQueryString(request, null);
    }

    /**
     * 获取请求字符串
     *
     * @param request
     * @return
     */
    public static String getQueryString(HttpServletRequest request, boolean encode) {
        if (!encode) {
            return getQueryString(request);
        }
        try {
            return URLEncoder.encode(getQueryString(request), Constants.CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取请求字符串
     *
     * @param request
     * @param ignoreParamNames 忽略的参数名
     * @return
     */
    public static String getQueryString(HttpServletRequest request, String[] ignoreParamNames) {
        StringBuffer sb = new StringBuffer();
        Enumeration<String> parameterNames = request.getParameterNames();
        for (int i = 0; parameterNames.hasMoreElements(); i++) {
            String parameterName = parameterNames.nextElement();
            if (ArrayUtils.contains(ignoreParamNames, parameterName)) {
                continue;
            }
            if (i > 0) {
                sb.append("&");
            }
            sb.append(parameterName).append("=");
            String[] parameterValues = request.getParameterValues(parameterName);
            if (parameterValues == null) {
                continue;
            }
            for (int j = 0, length = parameterValues.length; j < length; j++) {
                if (j > 0) {
                    sb.append(",");
                }
                String parameterValue = parameterValues[j];
                sb.append(parameterValue);
            }
        }

        return sb.toString();
    }

    /**
     * 获取请求URL
     *
     * @param request
     * @return
     */
    public static String getRequestUrl(HttpServletRequest request) {
        StringBuffer reqUrl = new StringBuffer(request.getRequestURL());
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            reqUrl.append("?").append(request.getQueryString());
        }
        return reqUrl.toString();
    }

    /**
     * @return String
     * @Description: 获取根目录
     * @Date: Feb 9, 2012 2:07:30 AM
     */
    public static String getServerRootDirectory(HttpServletRequest request) {
        return request.getSession().getServletContext().getRealPath(File.separator);
    }

    /**
     * 获取请求地址
     *
     * @param request
     * @return
     */
    public static String getServerUrl(HttpServletRequest request) {
//        String port = request.getServerPort() == 80 ? "" : ":" + request.getServerPort();
//        String serverName = request.getServerName();
//        if ("localhost".equals(serverName)) {
//            try {
//                serverName = InetAddress.getLocalHost().getHostAddress();
//            } catch (UnknownHostException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        return request.getScheme() + "://" + serverName + port + request.getContextPath();// + "/"
        return API_NOTIFY_URL;
    }

    public static String getgrOrderUrl(HttpServletRequest request) {
//        String port = request.getServerPort() == 80 ? "" : ":" + request.getServerPort();
//        String serverName = request.getServerName();
//        if ("localhost".equals(serverName)) {
//            try {
//                serverName = InetAddress.getLocalHost().getHostAddress();
//            } catch (UnknownHostException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        return request.getScheme() + "://" + serverName + port + request.getContextPath();// + "/"
        return GR_ORDER_URL;
    }


    @Deprecated
    public static String getIpAddress(HttpServletRequest request) {
        return getClientIp(request);
    }

    /**
     * 获得用户远程地址
     */
    public static String getClientIp(HttpServletRequest request) {
        String localIP = "127.0.0.1";//
        if (request == null) {
            return localIP;
        }
        String ip = request.getHeader("X-Real-IP");
        if ((ip == null) || (ip.isEmpty()) || (ip.equalsIgnoreCase(localIP)) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if ((ip == null) || (ip.isEmpty()) || (ip.equalsIgnoreCase(localIP)) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if ((ip == null) || (ip.isEmpty()) || (ip.equalsIgnoreCase(localIP)) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if ((ip == null) || (ip.isEmpty()) || (ip.equalsIgnoreCase(localIP)) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public static String realPath(HttpServletRequest request) {
        return request.getSession().getServletContext().getRealPath("") + "\\";
    }

    /**
     * 获取当前请求的URL地址域参数
     *
     * @param request
     * @return
     */
    public static Map<String, Object> getPrams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = iter.next();
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        return params;
    }

    /**
     * 获得用户远程地址
     */
    public static String getRemoteAddr(HttpServletRequest request) {
        String remoteAddr = request.getHeader("X-Real-IP");
        if (StringUtil.isNotBlank(remoteAddr)) {
            remoteAddr = request.getHeader("X-Forwarded-For");
        } else if (StringUtil.isNotBlank(remoteAddr)) {
            remoteAddr = request.getHeader("Proxy-Client-IP");
        } else if (StringUtil.isNotBlank(remoteAddr)) {
            remoteAddr = request.getHeader("WL-Proxy-Client-IP");
        }
        return remoteAddr != null ? remoteAddr : request.getRemoteAddr();
    }

    /**
     * @param request
     * @return
     */
    public static Map<String, Object> getRequestParams(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<String, Object>();
        Enumeration<String> e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String value = request.getParameter(name);
            map.put(name, value);
        }
        return map;
    }

    /**
     * @return
     */
//    public static HttpServletRequest getRequest() {
//        // HttpServletRequest request = ((ServletRequestAttributes)
//        // RequestContextHolder.getRequestAttributes()).getRequest();
//        // return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
//        return (HttpServletRequest) ((WebSubject) SecurityUtils.getSubject()).getServletRequest();
//    }

    /**
     * @return
     */
//    public static String getReferer() {
//        return getRequest().getHeader("Referer");
//    }

    /**
     * @return
     */
    public static String getReferer(HttpServletRequest request) {
        return request.getHeader("Referer");
    }

    /**
     * 判断是否ajax请求
     *
     * @param request {@link javax.servlet.http.HttpServletRequest}
     * @return
     */
    public static boolean isAjax(HttpServletRequest request) {
        String requestType = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestType);
    }

    /**
     * 判断是否微信请求
     *
     * @param request {@link javax.servlet.http.HttpServletRequest}
     * @return
     */
    public static boolean isWeiXin(HttpServletRequest request) {
        String userAgent = request.getHeader("user-agent");
        return userAgent.toLowerCase().contains("micromessenger");
    }

    public static String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session != null ? session.getId() : null);
    }

    public static void writeJson(HttpServletResponse response, String json) throws IOException {
        response.reset();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(Constants.CHARSET_UTF8);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter printWriter = response.getWriter();
        printWriter.write(json);
        printWriter.flush();
        printWriter.close();
    }

    public static void writeHtml(HttpServletResponse response, String html) throws IOException {
        response.reset();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(Constants.CHARSET_UTF8);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter printWriter = response.getWriter();
        printWriter.write(html);
        printWriter.flush();
        printWriter.close();
    }


    public static String getTransIn() {
        return TRANSIN;
    }
}

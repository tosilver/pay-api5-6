package co.b4pay.api.common.shiro;

import co.b4pay.api.common.utils.WebUtil;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * 鉴权过滤器
 *
 * @author YK
 * @version $Id: StatelessAuthcFilter.java, v 0.1 2016年5月8日 下午11:22:23 YK Exp $
 */
public class StatelessAuthcFilter extends AuthenticatingFilter {
    private static final Logger logger = LoggerFactory.getLogger(StatelessAuthcFilter.class);

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        return new StatelessToken(request);
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
//        HttpServletRequest httpRequest = WebUtils.toHttp(request);
//        System.out.println("请求头: ");
//        Enumeration<String> headerNames = httpRequest.getHeaderNames();
//        while (headerNames.hasMoreElements()) {
//            String headerName =  headerNames.nextElement();
//            System.out.println(headerName + ": " + httpRequest.getHeader(headerName));
//        }
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        return super.executeLogin(request, response);
    }

    @Override
    public void afterCompletion(ServletRequest request, ServletResponse response, Exception exception) throws Exception {
        if (exception != null) {
            logger.error(exception.getMessage(), exception);
        }
        super.afterCompletion(request, response, exception);
    }

    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
        //String userId = subject.getPrincipal().toString();
        //org.springframework.web.util.WebUtils.setSessionAttribute(WebUtils.toHttp(request), Constants.LOGIN_ID_KEY, userId);
        return super.onLoginSuccess(token, subject, request, response);
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        try {
//            HttpServletRequest httpRequest = WebUtils.toHttp(request);
            HttpServletResponse httpResponse = WebUtils.toHttp(response);
//            StatelessToken statelessToken = (StatelessToken) token;

            String detailMessage = "校验签名失败";
            if (e != null) {
//                if (e.getClass().isAssignableFrom(org.apache.shiro.authc.pam.UnsupportedTokenException.class)) {
//                    logger.warn(e.getMessage());
//                } else
                if (!e.getClass().isAssignableFrom(org.apache.shiro.authc.IncorrectCredentialsException.class)) {
                    logger.warn(e.getMessage());
                    detailMessage = e.getLocalizedMessage();
                }
            }
            WebUtil.writeJson(httpResponse, "{\"code\":-1,\"msg\":\"" + detailMessage + "\"}");

//            if (WebUtil.isAjax(httpRequest)) {// ajax请求
//                writeUnauthorizedMsg(httpResponse);
//            } else {
            // 客户端没有uid&token的Cookie的时候
//            if (statelessToken.getUserId() != null) {
//                new SimpleCookie("uid").removeFrom(httpRequest, httpResponse);// 删除客户端uid
//            }
//            if (statelessToken.getClientDigest() != null) {
//                new SimpleCookie("token").removeFrom(httpRequest, httpResponse);// 删除客户端token
//            }
//                httpResponse.sendRedirect(ViewConstants.LOGIN);
//            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return super.onLoginFailure(token, e, request, response);
    }
}
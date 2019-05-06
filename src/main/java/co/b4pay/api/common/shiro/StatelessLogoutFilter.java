package co.b4pay.api.common.shiro;

import co.b4pay.api.common.constants.Constants;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 退出登陆过滤器
 *
 * @author YK
 * @version $Id: StatelessLogoutFilter.java, v 0.1 2016年7月4日 下午2:50:51 YK Exp $
 */
public class StatelessLogoutFilter extends org.apache.shiro.web.filter.authc.LogoutFilter {

    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        System.out.println("登出：" + getSubject(request, response).getPrincipal());
        return super.preHandle(request, response);
    }

    @Override
    protected void issueRedirect(ServletRequest request, ServletResponse response, String redirectUrl) throws Exception {
        HttpServletRequest httpRequest = WebUtils.toHttp(request);
        HttpServletResponse httpResponse = WebUtils.toHttp(response);

        new SimpleCookie("uid").removeFrom(httpRequest, httpResponse);
        new SimpleCookie("token").removeFrom(httpRequest, httpResponse);
        // new SimpleCookie("mobile").removeFrom(httpRequest, httpResponse);

        org.springframework.web.util.WebUtils.setSessionAttribute(httpRequest, Constants.LOGIN_ID_KEY, null);
        writeUnauthorizedMsg(httpRequest, httpResponse);
    }

    private void writeUnauthorizedMsg(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.reset();
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().print("{\"status\":0,\"url\":\"" + getRedirectUrl() + "\"}");
    }
}

package co.b4pay.api.common.interceptor;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.service.IpWhitelistService;
import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.utils.WebUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * IP白名单拦截器
 *
 * @author YK
 * @version $Id: IpWhitelistInterceptor.java, v 0.1 2018年4月24日 上午01:35:09 YK Exp $
 */
public class IpWhitelistInterceptor extends HandlerInterceptorAdapter {
    private static final Logger logger = LoggerFactory.getLogger(IpWhitelistInterceptor.class);

    @Autowired
    private IpWhitelistService ipWhitelistService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String merchantId = request.getParameter(Constants.MERCHANT_ID);
        String clientIp = WebUtil.getClientIp(request);
        if (MainConfig.isDevMode || ipWhitelistService.isAccessAllowed(merchantId, clientIp)) {
            return true;
        }
        String detailMessage = "[" + merchantId + ", " + clientIp + "]IP无访问权限";
        logger.warn(detailMessage);
        WebUtil.writeJson(response, "{\"code\":-1,\"msg\":\"" + detailMessage + "\"}");
        return false;
    }
}

package co.b4pay.api.common.shiro;

import co.b4pay.api.common.utils.StringUtil;
import co.b4pay.api.common.utils.WebUtil;
import co.b4pay.api.common.utils.StringUtil;
import co.b4pay.api.common.utils.WebUtil;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 手机应用令牌
 *
 * @author YK
 * @version $Id: StatelessToken.java, v 0.1 2016年5月2日 下午3:30:16 YK Exp $
 */
public class StatelessToken implements AuthenticationToken {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 2986322741678349503L;

    private static final String KEY_MERCHANTID = "merchantId";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_SIGNATURE = "signature";
    private static final String KEY_SERVICE = "service";
    private static final String KEY_VERSION = "version";

    private String merchantId;
    private String timestamp;
    private String signature;
    private String service;
    private String version;
    private String clientIp;
    private Map<String, Object> signatureParams;

    StatelessToken(ServletRequest request) {
        this.merchantId = request.getParameter(KEY_MERCHANTID);
        this.timestamp = request.getParameter(KEY_TIMESTAMP);
        this.signature = request.getParameter(KEY_SIGNATURE);
        this.service = request.getParameter(KEY_SERVICE);
        this.version = request.getParameter(KEY_VERSION);
        this.clientIp = WebUtil.getClientIp(WebUtils.toHttp(request));

        // 参与签名的参数
        Map<String, Object> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String key = parameterNames.nextElement();
            if (StringUtil.equals(KEY_SIGNATURE, key)) {
                continue;//排除签名本身
            }
            params.put(key, request.getParameter(key));
        }
        this.signatureParams = params;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getService() {
        return service;
    }

    public String getVersion() {
        return version;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSignature() {
        return signature;
    }

    public String getClientIp() {
        return clientIp;
    }

    public Map<String, Object> getSignatureParams() {
        return signatureParams;
    }

    @Override
    public Object getPrincipal() {
        return this.merchantId;
    }

    @Override
    public Object getCredentials() {
        return this.signature;
    }
}

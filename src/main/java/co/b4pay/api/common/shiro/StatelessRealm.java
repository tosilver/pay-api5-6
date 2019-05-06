package co.b4pay.api.common.shiro;

import co.b4pay.api.common.config.MainConfig;
import co.b4pay.api.common.utils.StringUtil;
import co.b4pay.api.model.Merchant;
import co.b4pay.api.service.MerchantService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;

/**
 * 手机应用认证
 *
 * @author YK
 * @version $Id: StatelessRealm.java, v 0.1 2016年5月2日 下午3:22:18 YK Exp $
 */
public class StatelessRealm extends AuthorizingRealm {
    private Calendar calendar = Calendar.getInstance();

    @Autowired
    private MerchantService merchantService;

    @Override
    public boolean supports(AuthenticationToken token) {
        // 仅支持StatelessToken类型的Token
        return token instanceof StatelessToken;
    }

    /**
     * 权限验证
     *
     * @see org.apache.shiro.realm.AuthorizingRealm#doGetAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // 根据用户名查找角色，请根据需求实现；手机端不做复杂权限控制，只校验登录状态
        // SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        // String username = (String) principals.getPrimaryPrincipal();
        // authorizationInfo.setRoles(userService.findRoles(username));
        // authorizationInfo.setStringPermissions(userService.findPermissions(username));
        //return new SimpleAuthorizationInfo();
        return null;
    }

    /**
     * 登陆验证
     *
     * @see org.apache.shiro.realm.AuthenticatingRealm#doGetAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken)
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        try {
            final StatelessToken statelessToken = (StatelessToken) token;

            String merchantId = statelessToken.getMerchantId();
            if (StringUtil.isBlank(merchantId)) {
                throw new UnsupportedTokenException("缺少参数[merchantId]");
            }

            String timestamp = statelessToken.getTimestamp();
            if (StringUtil.isBlank(timestamp)) {
                throw new UnsupportedTokenException("缺少参数[timestamp]");
            }

            String signature = statelessToken.getSignature();
            if (StringUtil.isBlank(signature)) {
                throw new UnsupportedTokenException("缺少参数[signature]");
            }

            if (!MainConfig.isDevMode && calendar.getTimeInMillis() > Long.parseLong(timestamp) + 5000) {// 请求有效期 5秒钟
                throw new UnsupportedTokenException("已失效的请求");
            }

            Merchant merchant = merchantService.findById(Long.parseLong(merchantId));
            if (merchant == null) {
                throw new UnknownAccountException("[" + merchantId + "]商户信息不存在");
            }
            if (merchant.getStatus() != 1) {
                throw new LockedAccountException("[" + merchantId + "]商户被冻结或未启用");
            }

            return new StatelessAuthenticationInfo(merchantId, statelessToken.getCredentials(), merchant.getSecretKey(), getName());
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("获取授权信息时出错！" + e.getMessage(), e);
        }
    }
}

package co.b4pay.api.common.shiro;

import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.signature.HmacSHA1Signature;
import co.b4pay.api.common.signature.SignatureUtil;
import co.b4pay.api.dao.MerchantDao;
import co.b4pay.api.model.Merchant;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 无状态签名匹配验证器
 *
 * @author YK
 * @version $Id: StatelessHashedCredentialsMatcher.java, v 0.1 2017年3月5日 下午3:13:06 YK Exp $
 */
public class StatelessHashedCredentialsMatcher extends HashedCredentialsMatcher {
    private static final Logger logger = LoggerFactory.getLogger(StatelessHashedCredentialsMatcher.class);

//    private static final LRUCache<String, AtomicInteger> CACHE = new LRUCache<>(100);

    @Autowired
    private HmacSHA1Signature hmacSHA1Signature;

    @Autowired
    private MerchantDao merchantDao;

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        try {
            final StatelessToken statelessToken = (StatelessToken) token;
            final StatelessAuthenticationInfo authenticationInfo = (StatelessAuthenticationInfo) info;
            final String content = SignatureUtil.getSignatureContent(statelessToken.getSignatureParams(), true);
            logger.warn("签名内容为:"+content);
            String clientDigest = statelessToken.getSignature();
            logger.warn("用户传的签名为:"+clientDigest);
            String credentialsSalt = authenticationInfo.getCredentialsSalt();
            boolean matches = hmacSHA1Signature.check(content, clientDigest, credentialsSalt, Constants.CHARSET_UTF8);
            if (!matches) {
                String merchantId = statelessToken.getMerchantId();
                Merchant merchant = merchantDao.getOne(Long.parseLong(merchantId));
                String secretKey = merchant.getSecretKey();
                String sign = hmacSHA1Signature.sign(content, secretKey, "UTF-8");
                logger.warn("无效签名[{}] 签名内容[{}] 正确签名[{}]", statelessToken.getSignature(), content, sign);
            }
            return matches;
        } catch (Exception e) {
            throw new RuntimeException("签名校验出错", e);
        }
    }
}

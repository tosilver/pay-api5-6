package co.b4pay.api.common.shiro;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

/**
 * @author YK
 * @version $Id v 0.1 2017年03月05日 01:08 Exp $
 */
class StatelessAuthenticationInfo implements AuthenticationInfo {
    /**
     * The principals identifying the account associated with this AuthenticationInfo instance.
     */
    private PrincipalCollection principals;
    /**
     * The credentials verifying the account principals.
     */
    private Object credentials;

    /**
     * Any salt used in hashing the credentials.
     *
     * @since 1.1
     */
    private String credentialsSalt;

    StatelessAuthenticationInfo(Object principal, Object hashedCredentials, String credentialsSalt, String realmName) {
        this.principals = new SimplePrincipalCollection(principal, realmName);
        this.credentials = hashedCredentials;
        this.credentialsSalt = credentialsSalt;
    }

    @Override
    public PrincipalCollection getPrincipals() {
        return principals;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    String getCredentialsSalt() {
        return credentialsSalt;
    }
}

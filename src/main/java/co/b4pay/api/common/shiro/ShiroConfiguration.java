package co.b4pay.api.common.shiro;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SubjectDAO;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSessionStorageEvaluator;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ShiroConfiguration
 */
@Configuration
public class ShiroConfiguration {

    @Bean(name = "shiroFilter")
    public ShiroFilterFactoryBean shiroFilterFactoryBean(DefaultWebSecurityManager securityManager) {
        SecurityUtils.setSecurityManager(securityManager); //
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        // 配置shiro默认登录界面地址，前后端分离中登录界面跳转应由前端路由控制，后台仅返回json数据
        shiroFilterFactoryBean.setLoginUrl("/static/user/login.html");
        shiroFilterFactoryBean.setSuccessUrl("/static/home/index.html");
        shiroFilterFactoryBean.setUnauthorizedUrl("/static/user/login.html");
        // 登录成功后要跳转的链接
//        shiroFilterFactoryBean.setSuccessUrl("/index");
        //未授权界面;
//        shiroFilterFactoryBean.setUnauthorizedUrl("/403");

        Map<String, Filter> filters = new LinkedHashMap<>();
        filters.put("statelessAuthc", new StatelessAuthcFilter());
        shiroFilterFactoryBean.setFilters(filters);

        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        // 注意过滤器配置顺序 不能颠倒
        // 配置退出 过滤器,其中的具体的退出代码Shiro已经替我们实现了，登出后跳转配置的loginUrl
//        filterChainDefinitionMap.put("/logout", "logout");
        // 配置不会被拦截的链接 顺序判断
        filterChainDefinitionMap.put("/static/**", "anon");
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/test", "anon");
        filterChainDefinitionMap.put("/notify/**", "anon"); // 异步通知
        filterChainDefinitionMap.put("/alipay/cashier.htm", "anon"); // 支付宝收银页面
        filterChainDefinitionMap.put("/**", "statelessAuthc");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

        return shiroFilterFactoryBean;
    }

    @Bean
    public DefaultAdvisorAutoProxyCreator getDefaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator daap = new DefaultAdvisorAutoProxyCreator();
        daap.setProxyTargetClass(true);
        return daap;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor getAuthorizationAttributeSourceAdvisor(DefaultWebSecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor aasa = new AuthorizationAttributeSourceAdvisor();
        aasa.setSecurityManager(securityManager);
        return aasa;
    }

    /**
     * 凭证匹配器
     *
     * @return
     */
    @Bean(name = "credentialsMatcher")
    public StatelessHashedCredentialsMatcher credentialsMatcher() {
        StatelessHashedCredentialsMatcher credentialsMatcher = new StatelessHashedCredentialsMatcher();
        credentialsMatcher.setHashAlgorithmName("HmacSHA1");
        credentialsMatcher.setHashIterations(2);
        credentialsMatcher.setStoredCredentialsHexEncoded(true);
        return credentialsMatcher;
    }

    /**
     * Realm实现
     *
     * @return
     */
    @Bean(name = "statelessRealm")
    public StatelessRealm statelessRealm(StatelessHashedCredentialsMatcher credentialsMatcher) {
        StatelessRealm statelessRealm = new StatelessRealm();
        statelessRealm.setCachingEnabled(false);
        statelessRealm.setCredentialsMatcher(credentialsMatcher);
        return statelessRealm;
    }

    @Bean("shiroEhCacheManager")
    public EhCacheManager shiroEhCacheManager() {
        EhCacheManager ehCacheManager = new EhCacheManager();
        ehCacheManager.setCacheManagerConfigFile("classpath:ehcache/ehcache-shiro.xml");
        return ehCacheManager;
    }

    @Bean("sessionIdGenerator")
    public StatelesssSessionIdGenerator sessionIdGenerator() {
        return new StatelesssSessionIdGenerator();
    }

    @Bean("sessionDAO")
    public StatelessSessionDAO sessionDAO(StatelesssSessionIdGenerator sessionIdGenerator, EhCacheManager shiroEhCacheManager) {
        StatelessSessionDAO sessionDAO = new StatelessSessionDAO();
        sessionDAO.setSessionIdGenerator(sessionIdGenerator);
        sessionDAO.setCacheManager(shiroEhCacheManager);
        return sessionDAO;
    }

    @Bean("subjectFactory")
    public StatelessDefaultSubjectFactory subjectFactory() {
        return new StatelessDefaultSubjectFactory();
    }

    /**
     * 会话管理器
     * <p>
     * //     * @param sessionDAO
     *
     * @return
     */
    @Bean("sessionManager")
    public DefaultWebSessionManager sessionManager(StatelessSessionDAO sessionDAO) {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setSessionDAO(sessionDAO);
        sessionManager.setSessionValidationSchedulerEnabled(false); // 禁用掉会话调度器
        //sessionManager.setSessionValidationInterval(3600000);
        //sessionManager.setGlobalSessionTimeout(3600000);
        //sessionManager.setDeleteInvalidSessions(true);

        SimpleCookie sessionIdCookie = new SimpleCookie("SID");
        sessionManager.setSessionIdCookie(sessionIdCookie);
        return sessionManager;
    }

    @Bean
    public SubjectDAO subjectDAO() {
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        DefaultWebSessionStorageEvaluator sessionStorageEvaluator = new DefaultWebSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        return subjectDAO;
    }

    /**
     * 安全管理器
     */
    @Bean(name = "securityManager")
    public DefaultWebSecurityManager securityManager(StatelessRealm statelessRealm, SubjectDAO subjectDAO, StatelessDefaultSubjectFactory subjectFactory, DefaultWebSessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(statelessRealm);
        securityManager.setSubjectDAO(subjectDAO);
        securityManager.setSubjectFactory(subjectFactory);
        securityManager.setSessionManager(sessionManager);
        return securityManager;
    }

    /**
     * Shiro生命周期处理器
     *
     * @return
     */
    @Bean(name = "lifecycleBeanPostProcessor")
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }
}

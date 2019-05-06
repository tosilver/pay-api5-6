package co.b4pay.api.common.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;

import java.io.Serializable;

public class StatelesssSessionIdGenerator extends JavaUuidSessionIdGenerator {
    /**
     * logger
     */
    // private static final Logger logger = LoggerFactory.getLogger(StatelesssSessionIdGenerator.class);
    @Override
    public Serializable generateId(Session session) {
//        Subject subject = SecurityUtils.getSubject();
//        if (subject.isAuthenticated()) {
//            return subject.getPrincipal().toString();
//        }
        return super.generateId(session);
    }
}

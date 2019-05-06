package co.b4pay.api.common.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;

import java.io.Serializable;

public class StatelessSessionDAO extends CachingSessionDAO {
    /**
     * logger
     */
    // private static final Logger logger = LoggerFactory.getLogger(StatelessSessionDAO.class);
    @Override
    protected void doUpdate(Session session) {
    }

    @Override
    protected void doDelete(Session session) {
        // if (logger.isInfoEnabled()) {
        // logger.info("******doDelete");
        // }
    }

    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = generateSessionId(session);
        assignSessionId(session, sessionId);
        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        return null;
    }

}

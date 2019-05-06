package co.b4pay.api.common.listener;

import co.b4pay.api.common.kit.PropKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author YK
 * @version $Id v 0.1 2017年08月09日 16:32 Exp $
 */
public class ApplicationStartupListener implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupListener.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (logger.isInfoEnabled()) {
            logger.info("服务启动...");
        }
        PropKit.use("config");
    }
}

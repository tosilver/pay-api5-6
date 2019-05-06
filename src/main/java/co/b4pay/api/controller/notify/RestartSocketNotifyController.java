package co.b4pay.api.controller.notify;

import co.b4pay.api.socket.CodeSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 重启socket
 */
@RestController
@RequestMapping({"/notify/restartSocket.do"})
public class RestartSocketNotifyController {
    private static final Logger logger = LoggerFactory.getLogger(RestartSocketNotifyController.class);

    @RequestMapping(method = {RequestMethod.GET})
    public void doPost(HttpServletRequest request) {
        logger.warn("重启socket");
        CodeSocket server = new CodeSocket();//手机端监听
        server.start();
    }
}

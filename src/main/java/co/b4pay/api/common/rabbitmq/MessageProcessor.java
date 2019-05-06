package co.b4pay.api.common.rabbitmq;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

/**
 * @author YK
 * @version $Id v 0.1 2017年08月09日 10:48 Exp $
 */
//@Component
//@RabbitListener(containerFactory = "rabbitListenerContainerFactory", queues = MessageQueues.QUEUE_SMS)
public class MessageProcessor implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    @Override
    public void onMessage(Message message) {
        try {
            JSONObject o = JSONObject.parseObject(message.getBody(), JSONObject.class);
            logger.info(o.toString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

//    @RabbitHandler
//    public void process(@Payload String message) {
//        logger.info(message);
//    }

//    @RabbitHandler
//    public void process(@Payload Map<String, Object> message) {
//        logger.info(message.toString());
//    }
}

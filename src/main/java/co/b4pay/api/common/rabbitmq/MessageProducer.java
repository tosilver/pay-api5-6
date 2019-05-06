package co.b4pay.api.common.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author YK
 * @version $Id v 0.1 2017年08月09日 10:47 Exp $
 */
@Component
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String routingKey, Map<String, Object> message) {
        rabbitTemplate.convertAndSend(routingKey, message);
    }
}
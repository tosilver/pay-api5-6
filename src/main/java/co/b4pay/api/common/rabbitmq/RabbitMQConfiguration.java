package co.b4pay.api.common.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author YK
 * @version $Id v 0.1 2017年08月09日 17:17 Exp $
 */
@Configuration
public class RabbitMQConfiguration {
    private final static String QUEUE_NAME = MessageQueues.QUEUE_PAYMENT;
    private final static String EXCHANGE_NAME = "amq.direct";

    // RabbitMQ的配置信息
    @Value("${spring.rabbitmq.host}")
    private String mqRabbitHost;
    @Value("${spring.rabbitmq.port}")
    private Integer mqRabbitPort;
    @Value("${spring.rabbitmq.username}")
    private String mqRabbitUsername;
    @Value("${spring.rabbitmq.password}")
    private String mqRabbitPassword;
    @Value("${spring.rabbitmq.virtualHost}")
    private String mqRabbitVirtualHost;

    // 建立一个连接容器，类型数据库的连接池。
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(mqRabbitHost, mqRabbitPort);
        connectionFactory.setUsername(mqRabbitUsername);
        connectionFactory.setPassword(mqRabbitPassword);
        connectionFactory.setVirtualHost(mqRabbitVirtualHost);
        return connectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    // RabbitMQ的使用入口
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(EXCHANGE_NAME);
        template.setMessageConverter(messageConverter());
        return template;
    }

    // 要求RabbitMQ建立一个队列。
    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }

//    // 声明一个监听容器
//    @Bean
//    SimpleMessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory) {
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.setQueueNames(QUEUE_NAME);
//        container.setMessageListener(messageProcessor());
//        container.setMessageConverter(messageConverter());
//        return container;
//    }
//
//    // 在spring容器中添加一个监听类
//    @Bean
//    MessageProcessor messageProcessor() {
//        return new MessageProcessor();
//    }

    /*
     * spring amqp默认的是jackson 的一个插件,目的将生产者生产的数据转换为json存入消息队列，
     * 由于fastjson的速度快于jackson,这里替换为fastjson的一个实现
     */
    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 定义一个直连交换机
    @Bean
    DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    // 要求队列和直连交换机绑定，指定ROUTING_KEY
    @Bean
    Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(MessageRoutingKeys.MEMBER_FEE);
    }
}

package co.b4pay.api.service;

import org.springframework.stereotype.Component;

/**
 * @author YK
 * @version $Id v 0.1 2017年08月09日 17:07 Exp $
 */
@Component
public class MessageService {

//    @Autowired
//    private MessageProducer messageProducer;

    //@Scheduled(cron = "0/3 * * * * ?")
    //@Scheduled(fixedRate = 60 * 1000L)
    public void send() {
//        Map<String, Object> message = new HashMap<>();
//        for (int i = 0; i < 9; i++) {
//            message.put("body", Calendar.getInstance().getTime() + " hello: " + i);
//            messageProducer.sendMessage("TEST", message);
//        }
    }
}

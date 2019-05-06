package co.b4pay.api;

import co.b4pay.api.socket.CodeSocket;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 *
 * @author YK
 * @version $Id v 0.1 2017年08月08日 15:43 Exp $
 */
@SpringBootApplication(scanBasePackages = {"co.b4pay.api"})
@EnableScheduling
@EnableJpaAuditing
public class ApplicationStartup {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationStartup.class, args);
        CodeSocket server = new CodeSocket();//手机端监听
        server.start();
    }
}

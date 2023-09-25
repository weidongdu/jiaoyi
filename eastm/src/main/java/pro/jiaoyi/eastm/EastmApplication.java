package pro.jiaoyi.eastm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"pro.jiaoyi.*"})
@EnableScheduling
@EnableAsync
public class EastmApplication {

    public static void main(String[] args) {
        SpringApplication.run(EastmApplication.class, args);
    }

}

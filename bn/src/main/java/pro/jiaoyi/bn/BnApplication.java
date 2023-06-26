package pro.jiaoyi.bn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"pro.jiaoyi.*"})
@EnableScheduling
@EnableAsync
public class BnApplication {

    public static void main(String[] args) {
        SpringApplication.run(BnApplication.class, args);
    }

}

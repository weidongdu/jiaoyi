package pro.jiaoyi.bn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {"pro.jiaoyi.*"})
public class BnApplication {

    public static void main(String[] args) {
        SpringApplication.run(BnApplication.class, args);
    }

}

package pro.jiaoyi.eastm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"pro.jiaoyi.*"})
public class EastmApplication {

    public static void main(String[] args) {
        SpringApplication.run(EastmApplication.class, args);
    }

}

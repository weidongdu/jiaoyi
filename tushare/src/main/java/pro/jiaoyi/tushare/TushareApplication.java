package pro.jiaoyi.tushare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"pro.jiaoyi.*"})
public class TushareApplication {

    public static void main(String[] args) {
        SpringApplication.run(TushareApplication.class, args);
    }

}

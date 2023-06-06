package pro.jiaoyi.tradingview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {"pro.jiaoyi.*"})
@EnableAsync
public class TradingviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingviewApplication.class, args);
    }

}

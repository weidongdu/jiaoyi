package pro.jiaoyi.tushare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import pro.jiaoyi.common.util.http.okhttp4.OkHttp4Properties;

@SpringBootApplication
@ComponentScan(basePackages = {"pro.jiaoyi.*"})
@Import({OkHttp4Properties.class})
public class TushareApplication {

    public static void main(String[] args) {
        SpringApplication.run(TushareApplication.class, args);
    }

}

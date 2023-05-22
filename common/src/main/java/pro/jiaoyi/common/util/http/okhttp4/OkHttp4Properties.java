package pro.jiaoyi.common.util.http.okhttp4;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@ConfigurationProperties(prefix = "okhttp4")
@Component
@Data
public class OkHttp4Properties {

    //使用 @Value注解 同样可以
    private int connectTimeout;
    private int writeTimeout;
    private int readTimeout;

}

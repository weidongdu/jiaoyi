package pro.jiaoyi.common.util.http.okhttp4;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(prefix = "okhttp4", name = "enabled", havingValue = "true")
@Component
@Slf4j
@DependsOn("okHttp4Properties")
public class OkHttpUtil {

    @Autowired
    private OkHttp4Properties okHttp4Properties;

    private final OkHttpClient client;

    //保证了 okHttp4Properties 的初始化在先
    public OkHttpUtil(OkHttp4Properties okHttp4Properties) {
        //  final 修饰的成员变量实际上有两种含义。一种是“只能在声明时或构造函数中被赋值”，这是我们通常所说的 final 成员变量的含义。
        // 另一种是“只能保证在赋值之后不再被修改”，这是编译器的保证，是通过在编译时执行常量折叠等优化技术实现的。
        client =  new OkHttpClient.Builder()
                .connectTimeout(okHttp4Properties.getConnectTimeout(), TimeUnit.SECONDS)
                .writeTimeout(okHttp4Properties.getWriteTimeout(), TimeUnit.SECONDS)
                .readTimeout(okHttp4Properties.getReadTimeout(), TimeUnit.SECONDS)
                .build();
    }



    public Request buildRequest(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        return builder.build();
    }

    //基本的Get 请求
    public Response get(String url, Map<String, String> headers) {
        System.out.println(okHttp4Properties);
        Request request = buildRequest(url, headers);

        //发送get请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("get请求失败, code != 2xx, url:{}", url);
            }
            return response;
        } catch (Exception e) {
            log.error("请求异常，url:{}", url, e);
            throw new RuntimeException(e);
        }
    }
}

package pro.jiaoyi.common.util.http.okhttp4;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

//@ConditionalOnProperty(prefix = "okhttp4", name = "enabled", havingValue = "true")
@Component
@Slf4j
public class OkHttpUtil {

    @Autowired
    private OkHttp4Properties okHttp4Properties;

    private final OkHttpClient client = getOkHttpClient(okHttp4Properties);

    public OkHttpClient getOkHttpClient(OkHttp4Properties okHttp4Properties) {
        return new OkHttpClient.Builder()
                .connectTimeout(okHttp4Properties.getConnectTimeout(), TimeUnit.SECONDS)
                .writeTimeout(okHttp4Properties.getWriteTimeout(), TimeUnit.SECONDS)
                .readTimeout(okHttp4Properties.getReadTimeout(), TimeUnit.SECONDS)
                .build();
    }

    //build request
    public Request buildRequest(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        return builder.build();
    }

    //基本的Get 请求
    public Response get(String url, Map<String, String> headers) {
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

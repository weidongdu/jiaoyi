package pro.jiaoyi.common.util.http.okhttp4;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(prefix = "okhttp4", name = "enabled", havingValue = "true")
@Component
@Slf4j
@DependsOn("okHttp4Properties")
public class OkHttpUtil {

    private final OkHttpClient client;

    //保证了 okHttp4Properties 的初始化在先
    public OkHttpUtil(OkHttp4Properties okHttp4Properties) {
        // final 修饰的成员变量实际上有两种含义。一种是“只能在声明时或构造函数中被赋值”，这是我们通常所说的 final 成员变量的含义。
        // 另一种是“只能保证在赋值之后不再被修改”，这是编译器的保证，是通过在编译时执行常量折叠等优化技术实现的。
        client = new OkHttpClient.Builder()
                .connectTimeout(okHttp4Properties.getConnectTimeout(), TimeUnit.SECONDS)
                .writeTimeout(okHttp4Properties.getWriteTimeout(), TimeUnit.SECONDS)
                .readTimeout(okHttp4Properties.getReadTimeout(), TimeUnit.SECONDS)
                .build();
    }


    /**
     * 构建请求 Request
     *
     * @param url
     * @param headers
     * @return
     */

    public Request buildRequest(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        return builder.build();
    }

    /**
     * 构建请求 Request 包含请求体
     *
     * @param url
     * @param headers
     * @param body
     * @return
     */
    public Request buildRequest(String url, Map<String, String> headers, RequestBody body) {
        Request.Builder reqBuilder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(reqBuilder::addHeader);
        }
        return reqBuilder.post(body).build();
    }


    /**
     * 发送请求
     *
     * @param request
     * @return
     */
    public Response send(Request request) {
        //发送请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("post请求失败, code != 2xx, url:{}", request.url());
            }
            return response;
        } catch (IOException e) {
            log.error("请求异常，url:{}", request.url(), e);
            throw new RuntimeException(e);
        }
    }

    //基本的Get 请求
    public Response get(String url, Map<String, String> headers) {
        Request request = buildRequest(url, headers);
        return send(request);
    }


    //基本的Post Form请求
    public Response postForm(String url, Map<String, String> headers, Map<String, String> params) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        params.forEach(formBuilder::add);
        RequestBody formBody = formBuilder.build();
        Request req = buildRequest(url, headers, formBody);
        return send(req);
    }

    /**
     * 基本的Post Json请求
     *
     * @param url     请求地址
     * @param headers 请求头信息
     * @param json    请求体
     * @return 响应
     */
    public Response postJson(String url, Map<String, String> headers, String json) {
        RequestBody body = RequestBody.create(json, OkHttp4Properties.JSON);
        Request req = buildRequest(url, headers, body);
        return send(req);
    }


}

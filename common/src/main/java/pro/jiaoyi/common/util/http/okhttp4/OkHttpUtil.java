package pro.jiaoyi.common.util.http.okhttp4;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(prefix = "okhttp4", name = "enabled", havingValue = "true")
@Component
@Slf4j
@DependsOn("okHttp4Properties")
public class OkHttpUtil {


    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

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
    private Response send(Request request) {
        HttpUrl url = request.url();

        //发送请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("post请求失败, code != 2xx, url:{}", url);
            }
            return response;
        } catch (IOException e) {
            log.error("请求异常，url:{}", url, e);
            throw new RuntimeException(e);
        }
    }

    private byte[] sendDefault(Request request) {
        HttpUrl url = request.url();
        //发送请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("post请求失败, code != 2xx, url:{}", url);
            }
            return response.body().bytes();
        } catch (IOException e) {
            log.error("请求异常，url:{}", url, e);
        }
        return EMPTY_BYTE_ARRAY;
    }

    //基本的Get 请求
    public Response get(String url, Map<String, String> headers) {
        Request request = buildRequest(url, headers);
        return send(request);
    }

    public byte[] getForBytes(String url, Map<String, String> headers) {
        Request request = buildRequest(url, headers);
        return sendDefault(request);
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

    public byte[] postJsonForBytes(String url, Map<String, String> headers, String json) {
        RequestBody body = RequestBody.create(json, OkHttp4Properties.JSON);
        Request req = buildRequest(url, headers, body);
        return sendDefault(req);
    }

    public boolean downloadFile(String url, Map<String, String> headers, String destPath) {
        Request request = buildRequest(url, headers);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()){
                return false;
            }

            assert response.body() != null;
            InputStream inputStream = response.body().byteStream();
            File file = new File(destPath);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
            fileOutputStream.close();
            inputStream.close();
            return true;
        } catch (Exception e) {
            log.error("下载失败,请求异常，url:{}", url, e);
        }
        return false;
    }
}

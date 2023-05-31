package pro.jiaoyi.eastm.config;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;

@Component
@Slf4j
public class WxUtil {
    @Resource
    private OkHttpUtil httpUtil;

    public void send(String content) {
        String wxUrl = "http://8.142.9.14:20808/msg/wx?content=";
        String url = wxUrl + content;

        try {
            httpUtil.get(url,null);
        } catch (Exception e) {
            log.error("send to wx error {}", e.getMessage());
        }
    }
}

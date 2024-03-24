package pro.jiaoyi.eastm;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.util.JSONObject1O;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.eastm.api.EmBaseInfoClient;

@SpringBootTest
@Slf4j
class BaseInfoTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private EmBaseInfoClient emBaseInfoClient;

    String r(String businessReview){
        businessReview = businessReview.replaceAll(",", "\n");
        businessReview = businessReview.replaceAll(";", "\n");
        businessReview = businessReview.replaceAll(":", ":\n");
        businessReview = businessReview.replaceAll("。", "\n");
        businessReview = businessReview.replaceAll("下降", "⬇\uFE0F");
        businessReview = businessReview.replaceAll("延长", "⬆\uFE0F");
        businessReview = businessReview.replaceAll("增长", "⬆\uFE0F");
        return businessReview;
    }
    @Test
    public void mainRangeTest(){
        String code = "002475";
        String s = emBaseInfoClient.businessScope(code);

        System.out.println(r(s));

        String businessReview = emBaseInfoClient.businessReview(code);
        System.out.println(r(businessReview));


    }

}

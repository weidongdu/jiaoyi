package pro.jiaoyi.search;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestBody;
import pro.jiaoyi.common.model.ApiResult;
import pro.jiaoyi.search.scraper.BaiduKeywordScraper;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Slf4j
class SearchApplicationTests1 {

    @Test
    void contextLoads() {
    }

    @Resource
    private BaiduKeywordScraper baiduKeywordScraper;

    @Test
    public void baidu() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("master","咖啡");
        ArrayList<String> list = new ArrayList<>();
        list.add("咖啡");
        jsonObject.put("keywords",list);

        String master = jsonObject.getString("master");
        List<String> keywords = jsonObject.getJSONArray("keywords").toJavaList(String.class);
        //基础词 拓展
        baiduKeywordScraper.init(master,keywords);
    }
}

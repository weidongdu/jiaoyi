package pro.jiaoyi.search.api;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pro.jiaoyi.common.model.ApiResult;
import pro.jiaoyi.search.config.CutModeEnum;
import pro.jiaoyi.search.scraper.BaiduKeywordScraper;
import pro.jiaoyi.search.util.Text2Keyword;

import java.util.List;
import java.util.Map;

/**
 * @author dwd
 */
@RestController
@RequestMapping("/keyword")
public class KeywordController {

    @Autowired
    private Text2Keyword text2Keyword;
    @GetMapping("/text")
    public ApiResult text(String text,String mode) {
        CutModeEnum cutModeEnum = CutModeEnum.getByName(mode);

        Map<String, Integer> map = text2Keyword.text2KeywordMap(text,cutModeEnum);
        return ApiResult.success(map);
    }

    @Autowired
    private BaiduKeywordScraper baiduKeywordScraper;
    @PostMapping("/baidu/init")
    public ApiResult baidu(@RequestBody String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        String master = jsonObject.getString("master");
        List<String> keywords = jsonObject.getJSONArray("keywords").toJavaList(String.class);
        //基础词 拓展
        baiduKeywordScraper.init(master,keywords);
        return ApiResult.success();
    }
}

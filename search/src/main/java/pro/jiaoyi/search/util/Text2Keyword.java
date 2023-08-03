package pro.jiaoyi.search.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/*
将一段文本转换成关键词列表
 */
@Component
@Slf4j
public class Text2Keyword {

    @Autowired
    private OkHttpUtil okHttpUtil;


    @Value("${jieba.url}")
    private String jiebaUrl;

    public List<String> text2KeywordList(String text) {
        String send = send(text);
        log.info("jieba result: {}", send);
        JSONObject json = JSONObject.parseObject(send);
        if (json.containsKey("keywords")) {
            return json.getJSONArray("keywords").toJavaList(String.class);
        }

        return Collections.emptyList();
    }

    public List<String> text2KeywordList(String text, List<String> stopWords) {

        return Collections.emptyList();
    }

    public List<String> text2KeywordList(String text, List<String> stopWords, int topN) {

        return Collections.emptyList();
    }

    public Map<String, Integer> text2KeywordMap(String text, List<String> stopWords, int topN) {

        return Collections.emptyMap();
    }

    private JSONObject textJson(String text) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("text", text);
        return jsonObject;
    }

    /*
    {
        "text": "这是一段带提取的文字"
    }

    {
    "text": "这是一段带提取的文字",
    "keywords": [
            "这是",
            "一段",
            "带",
            "提取",
            "的",
            "文字"
        ]
    }
     */
    public String send(String text) {
        JSONObject jsonObject = textJson(text);
        byte[] bytes = okHttpUtil.postJsonForBytes(jiebaUrl, null, jsonObject.toJSONString());

        return new String(bytes);
    }

}

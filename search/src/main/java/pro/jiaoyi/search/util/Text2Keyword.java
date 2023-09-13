package pro.jiaoyi.search.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.CollectionsUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.search.config.CutModeEnum;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return text2KeywordList(text, CutModeEnum.cut_for_search);
    }
    public List<String> text2KeywordList(String text, CutModeEnum mode) {
        String send = send(text, mode);
        log.info("jieba result: {}", send);
        JSONObject json = JSONObject.parseObject(send);
        if (json == null) {
            return Collections.emptyList();
        }
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


    public Map<String, Integer> text2KeywordMap(String text, CutModeEnum mode) {
        List<String> list = text2KeywordList(text, mode);
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> map = list.stream()
                .collect(Collectors.toMap(
                        str -> str,              // 键是列表中的字符串
                        str -> 1,                // 初始值为1
                        Integer::sum             // 如果键已存在，则将值加1
                ));

        return CollectionsUtil.sortByValue(map, false);

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
    public String send(String text, CutModeEnum mode) {
        JSONObject jsonObject = textJson(text);
        jsonObject.put("mode", mode == null ? CutModeEnum.cut_for_search : mode);
        byte[] bytes = okHttpUtil.postJsonForBytes(jiebaUrl, null, jsonObject.toJSONString());

        return new String(bytes);
    }
    public String send(String text) {
        return send(text, CutModeEnum.cut_for_search);
    }

    public static void main(String[] args) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("mode", CutModeEnum.cut_all_True);
        System.out.println(jsonObject.toJSONString()    );
    }
}

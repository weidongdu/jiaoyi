package pro.jiaoyi.search.scraper;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.search.dao.entity.ZhihuQuestionEntity;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 百度关键词抓取
 */
@Component
@Slf4j
public class ZhihuQuestionScraper implements Scraper {

    @Resource
    private OkHttpUtil okHttpUtil;

    public static final Map<String, String> headers = new HashMap<>();

    static {
        headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
    }

    @Override
    public SearchResult search(String keyword) {
        return null;
    }

    public ZhihuQuestionEntity initJSON(String html) {
        if (!StringUtils.hasText(html)) {
            return null;
        }


        Document doc = Jsoup.parse(html);
        Element elementById = doc.getElementById("#js-initialData");

        return null;
    }

    public JSONObject init(String qid){
        String url = "https://www.zhihu.com/question/" + qid;
        byte[] bytes = okHttpUtil.getForBytes(url, headers);
        String html = new String(bytes);
        Document doc = Jsoup.parse(html);
        String targetId = "js-initialData";
        Element js = doc.getElementById(targetId);
        if (js == null) return null;

        String text = js.text();
        return JSONObject.parse(text);
    }


}

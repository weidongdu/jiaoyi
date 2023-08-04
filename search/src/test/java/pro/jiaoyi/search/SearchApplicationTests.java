package pro.jiaoyi.search;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.search.scraper.BaiduKeywordScraper;

@SpringBootTest
class SearchApplicationTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private BaiduKeywordScraper baiduKeywordScraper;

    @Test
    public void test() {
        baiduKeywordScraper.mobile("招股书",1);
    }
}

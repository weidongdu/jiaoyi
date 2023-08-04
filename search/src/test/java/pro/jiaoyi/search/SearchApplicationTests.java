package pro.jiaoyi.search;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.search.dao.entity.SearchResultEntity;
import pro.jiaoyi.search.dao.repo.SearchResultRepo;
import pro.jiaoyi.search.scraper.BaiduKeywordScraper;
import pro.jiaoyi.search.scraper.SearchResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest
@Slf4j
class SearchApplicationTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private BaiduKeywordScraper baiduKeywordScraper;

    @Test
    public void test() {
        ArrayList<SearchResult> l1 = new ArrayList<>();

        String keyword = "招股书";
        for (int i = 0; i < 3; i++) {
            SearchResult sr = baiduKeywordScraper.mobile(keyword, i + 1);
            if (sr == null) {
                continue;
            }
            l1.add(sr);
        }


        HashSet<String> rwSet = new HashSet<>();
        for (SearchResult sr : l1) {
            rwSet.addAll(sr.getKeywordRelated());
        }

        ArrayList<SearchResult> l2 = new ArrayList<>();
        for (String word : rwSet) {
            for (int i = 0; i < 3; i++) {
                SearchResult sr = baiduKeywordScraper.mobile(word, i + 1);
                if (sr == null) {
                    continue;
                }
                l2.add(sr);
            }
        }

    }

    public void extend(String keyword, int page, int level) {

        List<SearchResult> l1 = new ArrayList<>();
        for (int i = 0; i < page; i++) {
            SearchResult sr = baiduKeywordScraper.mobile(keyword, page);
            if (sr == null) {
                continue;
            }
            l1.add(sr);
        }

        HashSet<String> rwSet = new HashSet<>();
        for (SearchResult sr : l1) {
            rwSet.addAll(sr.getKeywordRelated());
        }

        rwSet.remove(keyword);
        if (level == 1) {
            return;
        }

        List<SearchResult> l2 = new ArrayList<>();
        for (String word : rwSet) {
            for (int i = 0; i < page; i++) {
                SearchResult sr = baiduKeywordScraper.mobile(word, page);
                if (sr == null) {
                    continue;
                }
                l2.add(sr);
            }
        }

        HashSet<String> rwSet2 = new HashSet<>();
        for (SearchResult sr : l2) {
            rwSet2.addAll(sr.getKeywordRelated());
        }

        rwSet2.remove(keyword);
        rwSet2.removeAll(rwSet);

        if (level == 2) {
            return;
        }

        List<SearchResult> l3 = new ArrayList<>();
        for (String word : rwSet2) {
            for (int i = 0; i < page; i++) {
                SearchResult sr = baiduKeywordScraper.mobile(word, page);
                if (sr == null) {
                    continue;
                }
                l3.add(sr);
            }
        }

        HashSet<String> rwSet3 = new HashSet<>();
        for (SearchResult sr : l3) {
            rwSet3.addAll(sr.getKeywordRelated());
        }

        rwSet3.remove(keyword);
        rwSet3.removeAll(rwSet);
        rwSet3.removeAll(rwSet2);

        if (level == 3) {
            return;
        }


    }

    @Autowired
    private SearchResultRepo searchResultRepo;

    @Test
    public void baidu() {
        extend1("招股书","招股书", 3, 3);
    }

    public void extend1(String master, String keyword, int page, int level) {
        HashSet<String> rwSet = new HashSet<>();
        HashSet<String> previousRwSet = new HashSet<>();


        for (int currentLevel = 0; currentLevel < level; currentLevel++) {
            List<SearchResult> l = new ArrayList<>();
            // If it's the first iteration, use the specified keyword.
            // For subsequent iterations, use the words from the previous rwSet.
            Set<String> words = currentLevel == 0 ? Collections.singleton(keyword) : rwSet;
            for (String word : words) {
                for (int i = 0; i < page; i++) {
                    SearchResult sr = baiduKeywordScraper.mobile(word, i + 1);
                    if (sr != null) {
                        l.add(sr);
                        for (SearchResult.Item item : sr.getItems()) {
                            SearchResultEntity entity = new SearchResultEntity();
                            entity.setMaster(master);//主关键词
                            entity.setKeyword(word);//关键词
                            entity.setSource(sr.getSource());//baidu zhihu
                            entity.setPlat(sr.getPlat());//pc mobile
                            entity.setType(sr.getType());//dropdown related result
                            entity.setTitle(item.getTitle());
                            entity.setContent(item.getContent());
                            entity.setUrl(item.getUrl());
                            entity.setRealUrl(item.getRealUrl());//真实url
                            entity.setPage(item.getPage());//搜索结果页数
                            entity.setOrderRank(item.getRank());//搜索结果排名
                            entity.setKeywordRelated(JSON.toJSONString(sr.getKeywordRelated()));//相关搜索
                            LocalDateTime now = LocalDateTime.now();
                            entity.setCreateTime(now);//下拉框
                            entity.setUpdateTime(now);//下拉框

                            log.info("entity:{}", entity);
                            searchResultRepo.save(entity);
                        }
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        log.error("Thread.sleep() error", e);
                    }


                }
            }

            rwSet.clear();
            for (SearchResult sr : l) {
                rwSet.addAll(sr.getKeywordRelated());
                //这里要做数据存储
            }

            rwSet.remove(keyword);
            rwSet.removeAll(previousRwSet);

            // Save the current rwSet for the next iteration
            previousRwSet.addAll(rwSet);
        }
    }



    @Test
    public void eightComponents() {
        // Optional. If not specified, WebDriver searches the PATH for chromedriver.
        System.setProperty("webdriver.chrome.driver", "/Users/dwd/Downloads/search/chromedriver-mac-arm64/chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("user-data-dir=/Users/dwd/Downloads/search/user_profile");
        options.addArguments("--remote-allow-origins=*");


        WebDriver driver = new ChromeDriver(options);
        String url = "https://m.baidu.com/";
        driver.get(url);

        String title = driver.getTitle();
        log.info("title:{}", title);

        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

        WebElement textBox = driver.findElement(By.name("my-text"));
        WebElement submitButton = driver.findElement(By.cssSelector("button"));

        textBox.sendKeys("Selenium");
        submitButton.click();

        WebElement message = driver.findElement(By.id("message"));
        String value = message.getText();

        driver.quit();
    }
}

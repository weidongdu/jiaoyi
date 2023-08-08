//package pro.jiaoyi.search;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.openqa.selenium.By;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.WindowType;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.chrome.ChromeOptions;
//import org.openqa.selenium.support.ui.Wait;
//import org.openqa.selenium.support.ui.WebDriverWait;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import pro.jiaoyi.search.config.SourceEnum;
//import pro.jiaoyi.search.dao.entity.BaiduKeywordSearchFailEntity;
//import pro.jiaoyi.search.dao.entity.SearchResultEntity;
//import pro.jiaoyi.search.dao.repo.BaiduKeywordSearchFailRepo;
//import pro.jiaoyi.search.dao.repo.SearchResultRepo;
//import pro.jiaoyi.search.scraper.BaiduKeywordScraper;
//import pro.jiaoyi.search.scraper.SearchResult;
//import pro.jiaoyi.search.util.RegUtil;
//import pro.jiaoyi.search.util.SeleniumUtil;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.*;
//
//@SpringBootTest
//@Slf4j
//class SearchApplicationTests {
//
//    @Test
//    void contextLoads() {
//    }
//
//    @Resource
//    private BaiduKeywordScraper baiduKeywordScraper;
//
////    @Test
////    public void test() {
////        ArrayList<SearchResult> l1 = new ArrayList<>();
////
////        String keyword = "招股书";
////        for (int i = 0; i < 3; i++) {
////            SearchResult sr = baiduKeywordScraper.mobile(keyword, i + 1);
////            if (sr == null) {
////                continue;
////            }
////            l1.add(sr);
////        }
////
////
////        HashSet<String> rwSet = new HashSet<>();
////        for (SearchResult sr : l1) {
////            rwSet.addAll(sr.getKeywordRelated());
////        }
////
////        ArrayList<SearchResult> l2 = new ArrayList<>();
////        for (String word : rwSet) {
////            for (int i = 0; i < 3; i++) {
////                SearchResult sr = baiduKeywordScraper.mobile(word, i + 1);
////                if (sr == null) {
////                    continue;
////                }
////                l2.add(sr);
////            }
////        }
////
////    }
//
////    public void extend(String keyword, int page, int level) {
////
////        List<SearchResult> l1 = new ArrayList<>();
////        for (int i = 0; i < page; i++) {
////            SearchResult sr = baiduKeywordScraper.mobile(keyword, page);
////            if (sr == null) {
////                continue;
////            }
////            l1.add(sr);
////        }
////
////        HashSet<String> rwSet = new HashSet<>();
////        for (SearchResult sr : l1) {
////            rwSet.addAll(sr.getKeywordRelated());
////        }
////
////        rwSet.remove(keyword);
////        if (level == 1) {
////            return;
////        }
////
////        List<SearchResult> l2 = new ArrayList<>();
////        for (String word : rwSet) {
////            for (int i = 0; i < page; i++) {
////                SearchResult sr = baiduKeywordScraper.mobile(word, page);
////                if (sr == null) {
////                    continue;
////                }
////                l2.add(sr);
////            }
////        }
////
////        HashSet<String> rwSet2 = new HashSet<>();
////        for (SearchResult sr : l2) {
////            rwSet2.addAll(sr.getKeywordRelated());
////        }
////
////        rwSet2.remove(keyword);
////        rwSet2.removeAll(rwSet);
////
////        if (level == 2) {
////            return;
////        }
////
////        List<SearchResult> l3 = new ArrayList<>();
////        for (String word : rwSet2) {
////            for (int i = 0; i < page; i++) {
////                SearchResult sr = baiduKeywordScraper.mobile(word, page);
////                if (sr == null) {
////                    continue;
////                }
////                l3.add(sr);
////            }
////        }
////
////        HashSet<String> rwSet3 = new HashSet<>();
////        for (SearchResult sr : l3) {
////            rwSet3.addAll(sr.getKeywordRelated());
////        }
////
////        rwSet3.remove(keyword);
////        rwSet3.removeAll(rwSet);
////        rwSet3.removeAll(rwSet2);
////
////        if (level == 3) {
////            return;
////        }
////
////
////    }
//
//    @Autowired
//    private SearchResultRepo searchResultRepo;
//    @Autowired
//    private BaiduKeywordSearchFailRepo baiduKeywordSearchFailRepo;
//
//
//    @Test
//    public void baidu() {
//        baiduKeywordSearch("咖啡", "咖啡", 3, 3);
//    }
//
//    /*
//    继续
//     */
//    @Test
//    public void related() {
//        List<SearchResultEntity> all = searchResultRepo.findAll();
//        if (all.isEmpty()) {
//            return;
//        }
//
//        HashSet<String> keywordSet = new HashSet<>();
//        for (SearchResultEntity entity : all) {
//            keywordSet.add(entity.getKeyword());
//            try {
//                List<String> list = JSONArray.parseArray(entity.getKeywordRelated(), String.class);
//                keywordSet.addAll(list);
//            } catch (Exception e) {
//                log.error("get keyword related error {}", entity);
//            }
//        }
//
//
//        ArrayList<String> keywords = new ArrayList<>(keywordSet);
//        ArrayList<String> kr = new ArrayList<>();//包含master
//        ArrayList<String> nkr = new ArrayList<>();//不包含master
//
//        String master = "咖啡";
//        for (String keyword : keywords) {
//            if (keyword.contains(master)) {
//                kr.add(keyword);
//            } else {
//                nkr.add(keyword);
//            }
//        }
//        kr.addAll(nkr);
//
//        for (int i = 0; i < kr.size(); i++) {
//            log.info("keyword {} index = {}/{}", kr.get(i), i, keywords.size());
//            String keyword = keywords.get(i);
//            try {
//                baiduKeywordSearch("咖啡", keyword, 3, 2);
//            } catch (Exception e) {
//                log.error("baidu search exception {}", e.getMessage(), e);
//            }
//        }
//    }
//
//
//    @Test
//    public void runFail() {
//        List<BaiduKeywordSearchFailEntity> failList = baiduKeywordSearchFailRepo.findAll();
//        if (failList.isEmpty()) {
//            return;
//        }
//
//        failList.forEach(entity -> {
//            baiduKeywordSearch(entity.getKeyword(), entity.getKeyword(), 3, 2);
//
//            log.info(" delete fail id: {}", entity.getId());
//            baiduKeywordSearchFailRepo.deleteById(entity.getId());
//        });
//    }
//
//    public void baiduKeywordSearch(String master, String keyword, int page, int level) {
//        HashSet<String> rwSet = new HashSet<>();
//        HashSet<String> previousRwSet = new HashSet<>();
//
//        WebDriver driver = SeleniumUtil.getDriver();
//        driver.get("https://m.baidu.com/s?word=%E7%99%BE%E5%BA%A6%E4%B8%80%E4%B8%8B" +
////                "&ts=0" +
////                "&t_kt=0" +
//                "&ie=utf-8" +
////                "&rsv_iqid=7921659044293865617" +
////                "&rsv_t=ac96ml5NRi9bqFY4gn6MhG%252BgsIQiOjVh8BeSwfoSSnFi%252BmwOcmz9" +
////                "&sa=is_1" +
////                "&rsv_pq=7921659044293865617" +
////                "&rsv_sug4=1691159333747" +
////                "&tj=1" +
////                "&ss=110" +
////                "&inputT=1691159333882" +
////                "&sugid=166791193111814" +
//                "&rq=baidu");
//        for (int currentLevel = 0; currentLevel < level; currentLevel++) {
//            List<SearchResult> l = new ArrayList<>();
//            // If it's the first iteration, use the specified keyword.
//            // For subsequent iterations, use the words from the previous rwSet.
//            Set<String> words = currentLevel == 0 ? Collections.singleton(keyword) : rwSet;
//            for (String word : words) {
//                Integer count = searchResultRepo.countByKeywordAndSource(word, SourceEnum.BAIDU.name());
//                if (count != null && count > 0) {
//                    log.info("keyword: {} already exist", word);
//                    continue;
//                }
//
//                for (int i = 0; i < page; i++) {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        log.error("Thread.sleep error", e);
//                    }
//                    SearchResult sr = null;
//                    try {
//                        sr = baiduKeywordScraper.mobile(driver, word, i + 1);
//                    } catch (Exception e) {
//                        log.error("baiduKeywordScraper.mobile error", e);
//                        //加入 fail list
//                        BaiduKeywordSearchFailEntity dbKeyword = baiduKeywordSearchFailRepo.findByKeyword(keyword);
//                        if (dbKeyword == null) {
//                            baiduKeywordSearchFailRepo.save(new BaiduKeywordSearchFailEntity(word));
//                        }
//                    }
//
//                    if (sr == null) {
//                        //判断是否为
//                        if ("百度安全验证".equalsIgnoreCase(driver.getTitle())) {
//                            driver.getWindowHandles().forEach(s -> {
////                                if (!s.equalsIgnoreCase(lastWindow)) {
//                                driver.switchTo().window(s);
//                                driver.close();
////                                }
//                            });
//                            try {
//                                log.info("sleep 5 min");
//                                Thread.sleep(1000 * 60 * 5);
//                            } catch (InterruptedException e) {
//                                log.error("Thread.sleep error", e);
//                            }
//
//                            driver.switchTo().newWindow(WindowType.WINDOW);
////                            String lastWindow = driver.getWindowHandle();
//                            driver.navigate().to("https://m.baidu.com/");
//
////                            driver.switchTo().window(lastWindow);
//                        }
//                        break;
//                    }
//
//
//                    l.add(sr);
//                    for (SearchResult.Item item : sr.getItems()) {
//                        SearchResultEntity entity = new SearchResultEntity();
//                        entity.setMaster(master);//主关键词
//                        entity.setKeyword(word);//关键词
//                        entity.setSource(sr.getSource());//baidu zhihu
//                        entity.setPlat(sr.getPlat());//pc mobile
//                        entity.setType(sr.getType());//dropdown related result
//                        entity.setTitle(item.getTitle());
//                        entity.setContent(item.getContent());
//                        entity.setUrl(item.getUrl());
//                        entity.setRealUrl(item.getRealUrl());//真实url
//                        entity.setDomain(RegUtil.domain(item.getRealUrl()));
//                        entity.setPage(item.getPage());//搜索结果页数
//                        entity.setOrderRank(item.getRank());//搜索结果排名
//                        entity.setKeywordRelated(JSON.toJSONString(sr.getKeywordRelated()));//相关搜索
//
//                        LocalDateTime now = LocalDateTime.now();
//                        entity.setCreateTime(now);//下拉框
//                        entity.setUpdateTime(now);//下拉框
//
//                        entity.setRemark(item.getRemark());
//                        log.info("entity:{}", entity);
//                        searchResultRepo.save(entity);
//                    }
//
//                }
//            }
//
//            rwSet.clear();
//            for (SearchResult sr : l) {
//                rwSet.addAll(sr.getKeywordRelated());
//                //这里要做数据存储
//            }
//
//            rwSet.remove(keyword);
//            rwSet.removeAll(previousRwSet);
//
//            // Save the current rwSet for the next iteration
//            previousRwSet.addAll(rwSet);
//        }
//
//        driver.close();//关闭当前窗口(tab)
//        driver.quit();
//
//    }
//
//
//    @Test
//    public void eightComponents() {
//        // Optional. If not specified, WebDriver searches the PATH for chromedriver.
//        System.setProperty("webdriver.chrome.driver", "/Users/dwd/Downloads/search/chromedriver-mac-arm64/chromedriver");
//
//        ChromeOptions options = new ChromeOptions();
//        options.addArguments("user-data-dir=/Users/dwd/Downloads/search/user_profile");
//        options.addArguments("--remote-allow-origins=*");
//
//
//        WebDriver driver = new ChromeDriver(options);
//        String url = "https://m.baidu.com/";
//        driver.get(url);
//
//        String title = driver.getTitle();
//        log.info("title:{}", title);
//
////        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
//
//        //显示等待 超时
//        WebElement revealed = driver.findElement(By.id("revealed"));
//        Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(2));
//
//        driver.findElement(By.id("reveal")).click();
//        wait.until(d -> revealed.isDisplayed());
//
//        revealed.sendKeys("Displayed");
//
//
//        WebElement textBox = driver.findElement(By.name("my-text"));
//        WebElement submitButton = driver.findElement(By.cssSelector("button"));
//
//        textBox.sendKeys("Selenium");
//        submitButton.click();
//
//        WebElement message = driver.findElement(By.id("message"));
//        String value = message.getText();
//
//        driver.quit();
//    }
//
//
//    @Test
//    public void explicit() {
//        System.setProperty("webdriver.chrome.driver", "/Users/dwd/Downloads/search/chromedriver-mac-arm64/chromedriver");
//
//        ChromeOptions options = new ChromeOptions();
//        options.addArguments("user-data-dir=/Users/dwd/Downloads/search/user_profile");
//        options.addArguments("--remote-allow-origins=*");
//
//
//        WebDriver driver = new ChromeDriver(options);
//
//        driver.get("https://www.selenium.dev/selenium/web/dynamic.html");
//        WebElement revealed = driver.findElement(By.id("revealed"));
//        Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//        //最大等待时间 10s 超时 , 具体等待时间由实际情况决定
//
//        driver.findElement(By.id("reveal")).click();
//        log.info("1");
//        wait.until(d -> revealed.isDisplayed());
//        log.info("2");
//
//        revealed.sendKeys("Displayed");
//        Assertions.assertEquals("Displayed", revealed.getDomProperty("value"));
//        driver.quit();
//    }
//}

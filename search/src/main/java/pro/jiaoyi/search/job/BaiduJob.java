package pro.jiaoyi.search.job;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.search.config.SourceEnum;
import pro.jiaoyi.search.dao.entity.KeywordSearchingEntity;
import pro.jiaoyi.search.dao.entity.KeywordsWaitToSearchEntity;
import pro.jiaoyi.search.dao.entity.SafeCheckEntity;
import pro.jiaoyi.search.dao.entity.SearchResultEntity;
import pro.jiaoyi.search.dao.repo.KeywordSearchingRepo;
import pro.jiaoyi.search.dao.repo.KeywordsWaitToSearchRepo;
import pro.jiaoyi.search.dao.repo.SafeCheckRepo;
import pro.jiaoyi.search.dao.repo.SearchResultRepo;
import pro.jiaoyi.search.scraper.BaiduKeywordScraper;
import pro.jiaoyi.search.scraper.SearchResult;
import pro.jiaoyi.search.strategy.BaiduSafeCheckImpl;
import pro.jiaoyi.search.strategy.SafeCheck;
import pro.jiaoyi.search.util.RegUtil;
import pro.jiaoyi.search.util.SeleniumUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;

import static pro.jiaoyi.search.config.SourceEnum.BAIDU;

@Slf4j
@Component
public class BaiduJob {

    @Resource
    private BaiduKeywordScraper baiduKeywordScraper;
    @Resource
    private KeywordsWaitToSearchRepo keywordsWaitToSearchRepo;

    @Resource
    private SearchResultRepo searchResultRepo;

    @Resource
    private KeywordSearchingRepo keywordSearchingRepo;

    @Resource(name = "baiduSafeCheckImpl")
    private SafeCheck baiduSafeCheckImpl;
    @Resource
    private SafeCheckRepo safeCheckRepo;

    public static final int MAX_LEVEL = 3; //从0 开始

    @Scheduled(fixedRate = 1000 * 60) // 1分钟
    @Async
    public void safeJob() {
        String ipAddress = null;
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            ipAddress = localhost.getHostAddress();
        } catch (UnknownHostException e) {
            log.error("error", e);
        }

        SafeCheckEntity db = safeCheckRepo.findBySourceAndHost(BAIDU.name(), ipAddress);
        if (db != null) {
            long stopTs = System.currentTimeMillis() - DateUtil.toTimestamp(db.getCreateTime());
            if (stopTs > BaiduSafeCheckImpl.STOP_TIME_MS) {
                log.info("百度安全验证 暂停结束 {}", db);
                safeCheckRepo.delete(db);
            }
        }
    }

    @Scheduled(fixedDelay = 1000 * 60)
    public void run() {
        String ipAddress = null;
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            ipAddress = localhost.getHostAddress();
        } catch (UnknownHostException e) {
            log.error("error", e);
        }

        SafeCheckEntity db = safeCheckRepo.findBySourceAndHost(BAIDU.name(),ipAddress);
        if (db != null) {
            long stopTs = System.currentTimeMillis() - DateUtil.toTimestamp(db.getCreateTime());
            if (stopTs < BaiduSafeCheckImpl.STOP_TIME_MS) {
                log.info("百度安全验证 暂停中 {}", db);
                return;
            }
        }

        // 计划 3页 3级 主词 -> [主词]关联1 -> [[主词]关联1]关联1
        // 第一层 本地提取
        // 目标得到 1 * 20 * 20 * 20 = 8000 个关键词 也就是会搜索8000 次
        // 8000 * 20 = 160000 个相关词
        // 加上 分词提到结构  预计 一百万个关键词
        // 去重 + 意义过滤 一百万个关键词 -> 1万个关有效键词

        List<KeywordsWaitToSearchEntity> initList = baiduKeywordScraper.getInitList(MAX_LEVEL, 10);
        log.info("initList size: {}", initList.size());

        for (KeywordsWaitToSearchEntity entity : initList) { // 20 个关键词

            // 正在处理的关键词 放入redis 作为标记, 用于分布式环境下的任务调度
            // 先使用代理ip 方案 (手机ip)
            // 代理ip 方案失败后, 使用 serverless 方案

            //判断 level 是否已经达到max
            if (entity.getLevel() >= MAX_LEVEL) {
                log.info("level >= MAX_LEVEL, pass, update entity: {}", JSON.toJSONString(entity));
                // 更新状态
                entity.setSearchCount(entity.getSearchCountMax() + 2);
                keywordsWaitToSearchRepo.saveAndFlush(entity);
                continue;
            }

            KeywordSearchingEntity searchingEntity = keywordSearchingRepo.findBySourceAndMasterKeywordAndKeyword(BAIDU.name(), entity.getMasterKeyword(), entity.getKeyword());
            if (searchingEntity == null) {
                searchingEntity = new KeywordSearchingEntity(BAIDU.name(), entity.getMasterKeyword(), entity.getKeyword());
                try {
                    keywordSearchingRepo.saveAndFlush(searchingEntity);
                } catch (Exception e) {
                    //相当于获取分布式锁失败
                    log.info("keyword is searching by other job, pass, entity: {}", JSON.toJSONString(entity));
                    continue;
                }

            } else {
                log.info("keyword is searching by other job, pass, entity: {}", JSON.toJSONString(entity));
                continue;
            }

            // 20 个关键词 待处理
            // 理论上会搜索 1 + 20次(相关搜索)
            // 得到 21 * 20 = 420 个关键词
            // 这21 个keyword 有可能在任一时候搜索失败
            // 失败后记录到数据库
            try {
                // get proxy
                Proxy proxy = null;
                if (entity.getLevel() < MAX_LEVEL) {
                    baiduKeywordSearch(entity.getMasterKeyword(), entity.getKeyword(), 2, entity.getLevel(), MAX_LEVEL, proxy);
                }
            } catch (Exception e) {
                log.error("baiduKeywordSearch error, entity: {}", JSON.toJSONString(entity), e);
            }
            keywordSearchingRepo.delete(searchingEntity);
        }

        //清理服务
    }


    public void baiduKeywordSearch(String master, String keyword, int page, int cLevel, int level, Proxy proxy) {

        HashSet<String> rwSet = new HashSet<>(); // 用于多级搜索, 以及去重
        HashSet<String> previousRwSet = new HashSet<>();

        boolean headless = true;

        WebDriver driver = proxy == null ? SeleniumUtil.getDriver(headless) : SeleniumUtil.getDriver(proxy, headless);
        try {
            baiduKeywordScraper.searchIndex(driver, master);
        } catch (Exception e) {
            log.error("searchIndex error, master: {}", master, e);
            driver.quit();
            return;
        }

        if (baiduSafeCheckImpl.safeCheck(driver)) {
            driver.quit();
            return;
        }

        for (int currentLevel = cLevel; currentLevel < level; currentLevel++) {
            List<SearchResult> l = new ArrayList<>();
            // If it's the first iteration, use the specified keyword.
            // For subsequent iterations, use the words from the previous rwSet.
            Set<String> words = currentLevel == cLevel ? Collections.singleton(keyword) : rwSet;
            for (String word : words) {
                Integer count = searchResultRepo.countByKeywordAndSource(word, SourceEnum.BAIDU.name());
                if (count != null && count > 0) {
                    log.info("keyword: {} already exist", word);
                    // skip
                    KeywordsWaitToSearchEntity dbWait = keywordsWaitToSearchRepo.findBySourceAndMasterKeywordAndKeyword(BAIDU.name(), master, word);
                    if (dbWait != null) {
                        log.info("update keyword: {}", dbWait);
                        dbWait.setSearchCount(dbWait.getSearchCountMax() + 1);
                        keywordsWaitToSearchRepo.saveAndFlush(dbWait);
                    }
                    continue;
                }

                for (int i = 0; i < page; i++) {
                    SearchResult sr = null;
                    try {
                        sr = baiduKeywordScraper.mobile(driver, word, i + 1);
                    } catch (Exception e) {
                        log.error("baiduKeywordScraper.mobile error", e);
                    }

                    if (sr == null) {
                        // update db
                        KeywordsWaitToSearchEntity dbWait = keywordsWaitToSearchRepo.findBySourceAndMasterKeywordAndKeyword(BAIDU.name(), master, word);
                        if (dbWait != null) {
                            log.info("update keyword: {}", dbWait);
                            dbWait.setSearchCount(dbWait.getSearchCount() + 1);
                            keywordsWaitToSearchRepo.saveAndFlush(dbWait);
                        }

                        if (baiduSafeCheckImpl.safeCheck(driver)) {
                            driver.quit();
                            //终结本次任务
                            return;
                        }
                        continue;
                    }

                    log.info("search result, save related keyword to wait list: {}", sr);
                    // save related keyword to wait list
                    List<String> keywordRelatedList = sr.getKeywordRelated();
                    if (keywordRelatedList != null && keywordRelatedList.size() > 0) {
                        for (String kr : keywordRelatedList) {
                            //check db exist
                            KeywordsWaitToSearchEntity dbWait = keywordsWaitToSearchRepo.findBySourceAndMasterKeywordAndKeyword(BAIDU.name(), master, kr);
                            if (dbWait == null) {
                                KeywordsWaitToSearchEntity entity = new KeywordsWaitToSearchEntity(BAIDU.name(), master, kr, currentLevel + 1);
                                keywordsWaitToSearchRepo.saveAndFlush(entity);
                            }
                        }
                    }

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
                        entity.setDomain(RegUtil.domain(item.getRealUrl()));
                        entity.setPage(item.getPage());//搜索结果页数
                        entity.setOrderRank(item.getRank());//搜索结果排名
                        entity.setKeywordRelated(JSON.toJSONString(sr.getKeywordRelated()));//相关搜索

                        LocalDateTime now = LocalDateTime.now();
                        entity.setCreateTime(now);//下拉框
                        entity.setUpdateTime(now);//下拉框

                        entity.setRemark(item.getRemark());
                        log.debug("entity:{}", entity);
                        searchResultRepo.save(entity);

                    }

                    //update wait list to searched count = max + 1
                    //标记 word 已经搜索过了 , count = max + 1
                    //这一步的原因是 同步本地kwSet 与 db
                    KeywordsWaitToSearchEntity dbWait = keywordsWaitToSearchRepo.findBySourceAndMasterKeywordAndKeyword(BAIDU.name(), master, word);
                    if (dbWait != null && dbWait.getSearchCount() < dbWait.getSearchCountMax()) {
                        log.info("update wait list, keyword: {}", dbWait);
                        dbWait.setSearchCount(dbWait.getSearchCountMax() + 1);
                        keywordsWaitToSearchRepo.saveAndFlush(dbWait);
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

        driver.close();//关闭当前窗口(tab)
        driver.quit();
    }

}

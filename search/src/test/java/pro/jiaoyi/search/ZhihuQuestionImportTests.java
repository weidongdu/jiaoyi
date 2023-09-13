package pro.jiaoyi.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.util.BeanUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.Rank;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.search.dao.entity.SearchResultEntity;
import pro.jiaoyi.search.dao.entity.ZhihuQuestionEntity;
import pro.jiaoyi.search.dao.entity.ZhihuQuestionScoreEntity;
import pro.jiaoyi.search.dao.repo.SearchResultRepo;
import pro.jiaoyi.search.dao.repo.ZhihuQuestionRepo;
import pro.jiaoyi.search.dao.repo.ZhihuQuestionScoreRepo;
import pro.jiaoyi.search.scraper.BaiduKeywordScraper;
import pro.jiaoyi.search.scraper.ZhihuQuestionScraper;
import pro.jiaoyi.search.util.RegUtil;
import pro.jiaoyi.search.util.SeleniumUtil;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@SpringBootTest
@Slf4j
class ZhihuQuestionImportTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private SearchResultRepo searchResultRepo;

    @Resource
    private ZhihuQuestionRepo zhihuQuestionRepo;

    @Resource
    private ZhihuQuestionScraper zhihuQuestionScraper;

    @Resource
    private OkHttpUtil okHttpUtil;

    @Test
    public void sync() {
        String domain = "zhihu.com";
        int pageSize = 2;
        int rank = 10;
        for (int i = 0; i < pageSize; i++) {
            for (int j = 0; j < rank; j++) {
                sync(domain, i + 1, j + 1);
            }
        }

    }


    public void sync(String domain, int page, int order) {
//        String domain = "zhihu.com";
//        int page = 1;
//        int order = 1;
        List<SearchResultEntity> list = searchResultRepo.findByDomainAndPageAndOrderRank(domain, page, order);

        for (SearchResultEntity entity : list) {
            if (StringUtils.hasText(entity.getRealUrl())) {
                String qid = RegUtil.zhihuQuestionId(entity.getUrl());
                if (!StringUtils.hasText(qid)) {
                    continue;
                }

                ZhihuQuestionEntity db = zhihuQuestionRepo.findByQid(qid);
                if (db != null) {
                    continue;
                }
                ZhihuQuestionEntity z = new ZhihuQuestionEntity();
                z.setQid(qid);
                z.setFetchTime(entity.getCreateTime());
                z.setSearchCount(0);
                z.setPn(page);
                z.setOrderRank(order);

                log.info("save {}", z);
                zhihuQuestionRepo.save(z);
            }
        }
    }


    @Test
    public void test() {
        int page = 1;
        int order = 1;

        List<ZhihuQuestionEntity> list = zhihuQuestionRepo.findByPnAndOrderRank(page, order);
        for (ZhihuQuestionEntity entity : list) {
            JSONObject jsonObject = zhihuQuestionScraper.init(entity.getQid());
            System.out.println(jsonObject);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("error ", e);
            }
        }

    }


    @Test
    public void getQids() {
        int lastId = 0;

        ArrayList<Long> ids = new ArrayList<>();


        //0828-1017 = 1970
        //fetch time 2023-08-27 12:22:53.346943
        for (int i = 0; i < 1970; i++) {
            int id = lastId + i + 1;

            ids.add((long) id);
        }
        List<ZhihuQuestionEntity> list = zhihuQuestionRepo.findAllById(ids);
        ArrayList<String> qids = new ArrayList<>();
        for (ZhihuQuestionEntity entity : list) {
            qids.add(String.valueOf(entity.getQid()));
        }
        System.out.println(JSON.toJSONString(qids));
    }


    @Test
    public void parseJson() {
        String path = "/Users/dwd/Downloads/search/zhihu_qid/brave";
        List<String> paths = FileUtil.readDirectoryFilesAbsPath(path);

        for (int i = 0; i < paths.size(); i++) {
            String s = FileUtil.readFromFile(paths.get(i));

            try {
                parseJson(s);
            } catch (Exception e) {
                log.error("error {}", e.getMessage(), e);
            }
        }

    }

    public void parseJson(String s) {
//        String path = "/Users/dwd/Downloads/search/zhihu_qid/brave";
//        List<String> paths = FileUtil.readDirectoryFilesAbsPath(path);
//        String s = FileUtil.readFromFile(paths.get(0));
        JSONObject jsonObject = JSONObject.parse(s);
        System.out.println(jsonObject);

        if (jsonObject != null) {
            String qid = jsonObject.getString("qid");
            //            Optional<ZhihuQuestionEntity> db = zhihuQuestionRepo.findByQid(qid);
            ZhihuQuestionEntity entity = zhihuQuestionRepo.findByQid(qid);
            if (entity != null) {
                entity.setQid(qid);
                entity.setTitle(jsonObject.getString("title"));

                entity.setCreated(DateUtil.toLocalDateTime(new Date(jsonObject.getLong("created") * 1000L)));
                entity.setUpdatedTime(DateUtil.toLocalDateTime(new Date(jsonObject.getLong("updatedTime") * 1000L)));

                Integer answerCount = jsonObject.getInteger("answerCount");// "answerCount": 21,
                Integer visitCount = jsonObject.getInteger("visitCount");//   "visitCount": 917797,
                Integer commentCount = jsonObject.getInteger("commentCount");//   "commentCount": 4,
                Integer followerCount = jsonObject.getInteger("followerCount");//   "followerCount": 107,
                Integer collapsedAnswerCount = jsonObject.getInteger("collapsedAnswerCount");//   "collapsedAnswerCount": 3,

                entity.setAnswerCount(answerCount);
                entity.setVisitCount(visitCount);
                entity.setCommentCount(commentCount);
                entity.setFollowerCount(followerCount);
                entity.setCollapsedAnswerCount(collapsedAnswerCount);


                JSONArray answers = jsonObject.getJSONArray("answers");
                if (answers != null && answers.size() > 0) {

                    {
                        JSONObject a1 = answers.getJSONObject(0);
                        String name = a1.getString("name");
                        Integer voteupCount = a1.getInteger("voteupCount");
                        Long createdTime = a1.getLong("createdTime") * 1000L;
                        Long updatedTime = a1.getLong("updatedTime") * 1000L;

                        entity.setA1_name(name);
                        entity.setA1_voteupCount(voteupCount);
                        entity.setA1_createdTime(DateUtil.toLocalDateTime(new Date(createdTime)));
                        entity.setA1_updatedTime(DateUtil.toLocalDateTime(new Date(updatedTime)));
                    }

                    if (answers.size() >= 2) {
                        JSONObject a1 = answers.getJSONObject(0);
                        String name = a1.getString("name");
                        Integer voteupCount = a1.getInteger("voteupCount");
                        Long createdTime = a1.getLong("createdTime") * 1000L;
                        Long updatedTime = a1.getLong("updatedTime") * 1000L;

                        entity.setA2_name(name);
                        entity.setA2_voteupCount(voteupCount);
                        entity.setA2_createdTime(DateUtil.toLocalDateTime(new Date(createdTime)));
                        entity.setA2_updatedTime(DateUtil.toLocalDateTime(new Date(updatedTime)));
                    }


                    if (answers.size() >= 3) {
                        JSONObject a1 = answers.getJSONObject(0);
                        String name = a1.getString("name");
                        Integer voteupCount = a1.getInteger("voteupCount");
                        Long createdTime = a1.getLong("createdTime") * 1000L;
                        Long updatedTime = a1.getLong("updatedTime") * 1000L;

                        entity.setA3_name(name);
                        entity.setA3_voteupCount(voteupCount);
                        entity.setA3_createdTime(DateUtil.toLocalDateTime(new Date(createdTime)));
                        entity.setA3_updatedTime(DateUtil.toLocalDateTime(new Date(updatedTime)));
                    }

                    if (answers.size() >= 4) {
                        JSONObject a1 = answers.getJSONObject(0);
                        String name = a1.getString("name");
                        Integer voteupCount = a1.getInteger("voteupCount");
                        Long createdTime = a1.getLong("createdTime") * 1000L;
                        Long updatedTime = a1.getLong("updatedTime") * 1000L;

                        entity.setA4_name(name);
                        entity.setA4_voteupCount(voteupCount);
                        entity.setA4_createdTime(DateUtil.toLocalDateTime(new Date(createdTime)));
                        entity.setA4_updatedTime(DateUtil.toLocalDateTime(new Date(updatedTime)));
                    }

                    if (answers.size() >= 4) {
                        JSONObject a1 = answers.getJSONObject(0);
                        String name = a1.getString("name");
                        Integer voteupCount = a1.getInteger("voteupCount");
                        Long createdTime = a1.getLong("createdTime") * 1000L;
                        Long updatedTime = a1.getLong("updatedTime") * 1000L;

                        entity.setA5_name(name);
                        entity.setA5_voteupCount(voteupCount);
                        entity.setA5_createdTime(DateUtil.toLocalDateTime(new Date(createdTime)));
                        entity.setA5_updatedTime(DateUtil.toLocalDateTime(new Date(updatedTime)));
                    }

                }
                System.out.println(entity);
                zhihuQuestionRepo.save(entity);
            } else {
                log.info("not found qid {}", qid);
            }
        }


    }

    @Resource
    private ZhihuQuestionScoreRepo zhihuQuestionScoreRepo;
    @Test
    public void score(){
        List<ZhihuQuestionEntity> all = zhihuQuestionRepo.findAll();
        for (ZhihuQuestionEntity entity : all) {
            if (entity.getVisitCount() != null && entity.getVisitCount() > 0){
                ZhihuQuestionScoreEntity scEntity = new ZhihuQuestionScoreEntity();
                BeanUtils.copyProperties(entity,scEntity);
                scEntity.setId(null);
                scEntity.setFetchTime(LocalDateTime.now());
                int daysDifference = DateUtil.daysDiff(entity.getCreated());
                if (daysDifference <= 0) daysDifference = 1;
                scEntity.setDs(daysDifference);
                scEntity.setVisitPerDay(BigDecimal.valueOf(entity.getVisitCount()).divide(BigDecimal.valueOf(daysDifference),1, RoundingMode.HALF_UP) );

                log.info("sc {}",scEntity);
                zhihuQuestionScoreRepo.save(scEntity);
            }
        }
    }
}

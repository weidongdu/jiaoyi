package pro.jiaoyi.eastm.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.dao.entity.ThemeScoreEntity;
import pro.jiaoyi.eastm.dao.entity.WeiboEntity;
import pro.jiaoyi.eastm.dao.repo.ThemeScoreRepo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.service.WeiboService;
import pro.jiaoyi.eastm.util.TradeTimeUtil;
import pro.jiaoyi.eastm.util.sina.SinaTicktimeDataUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/data")
public class DataController {

    /*
[
[
"Income",
"Life Expectancy",
"Population",
"Country",
"Year"
],
[
815,
34.05,
351014,
"Australia",
1800
]]
     */
    @Resource
    private ThemeScoreRepo themeScoreRepo;


    /*
    [
        [
        "Income",
        "Life Expectancy",
        "Population",
        "Country",
        "Year"
        ],
        [
        815,
        34.05,
        351014,
        "Australia",
        1800
        ]
    ]
     */
    @GetMapping("/themeScore")
    @CrossOrigin
    public JSONObject themeScore() {


        LocalDateTime now = LocalDateTime.now();
        List<ThemeScoreEntity> list = themeScoreRepo.findAllByCreateTimeBetween(now.minusDays(2), now);

        BigDecimal score = BigDecimal.ZERO;

        List<ThemeScoreEntity> ids = list.stream()
                .filter(i -> i.getF1Theme().equals("昨日涨停_含一字"))
                .sorted(Comparator.comparingLong(ThemeScoreEntity::getId)).toList();
        if (!ids.isEmpty()) {
            score = ids.get(ids.size() - 1).getF2Score();
        }

        final BigDecimal finalScore = score;
        List<String> themelist = list.stream()
                .filter(i -> i.getF2Score().compareTo(finalScore) > 0)
                .map(ThemeScoreEntity::getF1Theme).distinct().toList();

        HashSet<String> themes = new HashSet<>(themelist);


        ArrayList<Object[]> array = new ArrayList<>(list.size() + 1);
        Object[] head = new Object[5];
        head[0] = "Income"; //score
        head[1] = "Life Expectancy";
        head[2] = "Population";
        head[3] = "Country";//theme
        head[4] = "Year";//creatTime
        array.add(head);

        for (ThemeScoreEntity themeScore : list) {
            if (!themes.contains(themeScore.getF1Theme())) {
                continue;
            }

            Object[] items = new Object[5];
            items[0] = themeScore.getF2Score(); //"Income",
            items[1] = themeScore.getF3Chg(); //"Life Expectancy",
            items[2] = themeScore.getId(); //"Population",
            items[3] = themeScore.getF1Theme(); //"Country",
            LocalDateTime createTime = themeScore.getCreateTime();
            //将getCreateTime 精确分钟级别 去掉秒
            items[4] = LocalDateTime.of(createTime.getYear(), createTime.getMonth(), createTime.getDayOfMonth(),
                    createTime.getHour(), createTime.getMinute(), 0);//"Year"
            array.add(items);

            themes.add(themeScore.getF1Theme());
        }


        JSONObject map = new JSONObject();
        map.put("data", array);
        map.put("themes", new ArrayList<>(themes));

        return map;
    }

    /*
        {
        "ts": 1626950400000,
        "arr" : [{

               "uid":"",
               "content":"",
               "mid":"",
               "createTime":""

        }]
        }
         */
    @Resource
    private WeiboService weiboService;

    @PostMapping("/wb")
    @CrossOrigin
    public String wb(@RequestBody JSONObject jsonObject) {
        Long ts = jsonObject.getLong("ts");
        JSONArray jsonArray = jsonObject.getJSONArray("arr");
        List<WeiboEntity> weiboEntityList = jsonArray.toJavaList(WeiboEntity.class);
        for (WeiboEntity weiboEntity : weiboEntityList) {
            // timestamp to beijing time
            weiboEntity.setCreateTime(DateUtil.toLocalDateTime(new Date(ts)));
        }
        weiboService.send(weiboEntityList);
        return "ok";
    }


    @Resource
    private SinaTicktimeDataUtil sinaTicktimeDataUtil;
    @Resource
    private EmClient emClient;

//    @GetMapping("/sina/init")
//    @Async
//    public void init() {
//        List<EmCList> list = emClient.getClistDefaultSize(true);
////        boolean flag = false;
//        for (EmCList em : list) {
//            log.info("em={}", em);
//            //时间 start 2024-02-19 -> now
//            //时间 end now
//
////            String s = "000650";
////            if (s.equals(em.getF12Code())) {
////                flag = true;
////            }
////            if (!flag) {
////                continue;
////            }
//
//            if (em.getF5Vol().compareTo(BigDecimal.ONE) <= 0
//                    || em.getF2Close().compareTo(BigDecimal.ONE) <= 0
//                    || em.getF15High().compareTo(BigDecimal.ONE) <= 0
//            ) {
//                log.info("pass code:{},name:{},vol:{},close:{}", em.getF12Code(), em.getF14Name(), em.getF5Vol(), em.getF2Close());
//                continue;
//            }
//
//
//            String symbol = em.getF12Code();
//            for (int i = 0; i < 30; i++) {
//                LocalDate start = LocalDate.of(2024, 2, 20);
//                LocalDate limit = LocalDate.of(2024, 3, 9);
//                start = start.plusDays(i);
//                if (!TradeTimeUtil.isTradeDay(start) || start.isAfter(limit)) {
//                    log.info("{} 非交易日", start);
//                    continue;
//                }
//
//                String day = start.toString();
//                try {
//                    sinaTicktimeDataUtil.getTicktimeData(symbol, day);
//                    Thread.sleep(1000);
//                } catch (Exception e) {
//                    log.error("{}", e);
//                }
//            }
//
//        }
//
//
//    }

    @GetMapping("/sina/sum")
    @Async
    public void tickSum(String day) {
        if (!TradeTimeUtil.isTradeDay() || day == null || day.isEmpty()) {
            return;
        }

        List<EmCList> list = emClient.getClistDefaultSize(true);
        for (EmCList em : list) {

            if (em.getF5Vol().compareTo(BigDecimal.ONE) <= 0
                    || em.getF2Close().compareTo(BigDecimal.ONE) <= 0
                    || em.getF15High().compareTo(BigDecimal.ONE) <= 0) {
                log.info("pass code:{},name:{},vol:{},close:{}", em.getF12Code(), em.getF14Name(), em.getF5Vol(), em.getF2Close());
                continue;
            }

            String symbol = em.getF12Code();

            try {
                sinaTicktimeDataUtil.getTicktimeData(symbol, day, true);
            } catch (Exception e) {
                log.error("{}", e);
            }

        }


    }

}



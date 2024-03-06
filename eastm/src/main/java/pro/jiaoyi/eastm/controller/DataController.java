package pro.jiaoyi.eastm.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.dao.entity.ThemeScoreEntity;
import pro.jiaoyi.eastm.dao.entity.WeiboEntity;
import pro.jiaoyi.eastm.dao.repo.ThemeScoreRepo;
import pro.jiaoyi.eastm.service.WeiboService;

import java.math.BigDecimal;
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


}



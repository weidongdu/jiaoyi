package pro.jiaoyi.eastm.controller;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.eastm.dao.entity.ThemeScoreEntity;
import pro.jiaoyi.eastm.dao.repo.ThemeScoreRepo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

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
            if (!themes.contains(themeScore.getF1Theme())){
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
}



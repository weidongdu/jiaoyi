package pro.jiaoyi.eastm.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.CollectionsUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.dao.entity.OpenEmCListEntity;
import pro.jiaoyi.eastm.dao.entity.ThemeScoreEntity;
import pro.jiaoyi.eastm.dao.repo.OpenEmCListRepo;
import pro.jiaoyi.eastm.dao.repo.ThemeScoreRepo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.service.ImgService;
import pro.jiaoyi.eastm.service.SpeedService;
import pro.jiaoyi.eastm.util.TradeTimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static pro.jiaoyi.eastm.controller.StockController.MONITOR_CODE_AMT_MAP;

@Component
@Slf4j
public class MarketJob {
    @Resource
    private EmClient emClient;
    @Resource
    private SpeedService speedService;
    @Resource
    private ThemeScoreRepo themeScoreRepo;
    @Resource
    private OpenEmCListRepo openEmCListRepo;
    @Resource
    private WxUtil wxUtil;
    @Resource
    private ImgService imgService;

    //常量

    public static final Cache<String, List<String>> CACHE_THEME_MAP = Caffeine.newBuilder()
            .expireAfterWrite(600, TimeUnit.MINUTES)
            .maximumSize(1000).build();
    private static Map<String, BigDecimal> THEME_SPEED_SCORE_MAP_PRE = new HashMap<>();
    public static final Set<String> CODE_MA_BLOCK_SET = new HashSet<>();
    public static final HashSet<String> BLOCK_THEME_SET = new HashSet<>();
    private static final Map<String, Integer> WX_SEND_COUNT_MAP = new HashMap<>();
    public static final Map<String, List<EmDailyK>> CODE_KS_CACHE_MAP = new HashMap<>();
    public static final AtomicInteger CODE_KS_CACHE_COUNT = new AtomicInteger(0);
    private static List<EmCList> EM_CLIST_PRE = null;


    {
        List<String> t = List.of(
                "融资融券", "国企改革", "机构重仓", "创业板综", "标准普尔", "预盈预增",
                "深股通", "预亏预减", "富时罗素", "沪股通", "转债标的",
                "QFII重仓", "央企改革", "MSCI中国", "破净股", "参股保险",
                "参股券商", "参股银行", "AH股", "基金重仓", "参股期货",
                "AB股", "参股新三板", "证金持股", "送转预期", "注册制次新股", "昨日涨停",
                "中证500", "深成500", "上证380", "低价股", "江苏板块",
                "广东板块", "北京板块", "浙江板块", "深圳特区", "江苏板块",
                "上海板块", "四川板块", "辽宁板块", "湖南板块", "湖北板块", "山东板块",
                "安徽板块", "新疆板块", "江西板块", "京津冀", "长江三角",
                "河南板块", "成渝特区", "河北板块", "海南板块", "贵州板块",
                "吉林板块", "宁夏板块", "福建板块", "重庆板块", "甘肃板块", "陕西板块"
        );

        BLOCK_THEME_SET.addAll(t);
    }


    //1. 获取开盘竞价
    @Scheduled(cron = "30 25 9 * * ?")
    public void runOpen() {
        open("");
    }

    @Scheduled(cron = "30 0/10 * * * ?")
    @Async
    public void runThemePct() {
        themePct();
    }

    @Scheduled(cron = "0 15 06,15 ? * MON-FRI")
    public void initMaMap() {
        CODE_MA_BLOCK_SET.clear();
        CODE_KS_CACHE_COUNT.set(0);
        if (LocalTime.now().getHour() >= 15) {
            MONITOR_CODE_AMT_MAP.clear();
        }
    }

    @Scheduled(cron = "0/5 0/1 * * * ?")
    public void runTrading() {
        tradingAlert();
    }


    public void open(String source) {
        log.info("开盘竞价");
        // 1. 获取今日是否开盘
        if (!TradeTimeUtil.isTradeDay()) {
            log.info("今日不开盘");
            return;
        }

        String tradeDate = LocalDate.now().toString().replace("-", "");
        // 今日开盘
        List<EmCList> openList = new ArrayList<>();
        if ("db".equalsIgnoreCase(source)) {
            List<OpenEmCListEntity> all = openEmCListRepo.findByTradeDate(tradeDate);
            HashMap<String, OpenEmCListEntity> map = new HashMap<>();
            for (OpenEmCListEntity entity : all) {
                map.put(entity.getF12Code(), entity);
            }

            for (OpenEmCListEntity openEmCList : map.values()) {
                String jsonString = JSONObject.toJSONString(openEmCList);
                EmCList emCList = JSON.parseObject(jsonString, EmCList.class);
                openList.add(emCList);
            }
        } else {
            openList = emClient.getClistDefaultSize(true);
        }

        if (openList == null || openList.size() == 0) {
            return;
        }

        List<EmCList> fList = openList.stream().filter(
                e -> e.getF3Pct().abs().compareTo(BDUtil.B1) < 0
                        && e.getF6Amt().compareTo(BDUtil.B5000W) > 0
        ).toList();

        ArrayList<String> names = new ArrayList<>();
        for (EmCList em : fList) {
            String code = em.getF12Code();
            List<EmDailyK> kList = emClient.getDailyKs(code, LocalDate.now(), 100, true);
            if (kList == null || kList.size() < 100) {
                continue;
            }

            List<BigDecimal> closeList = kList.stream().map(EmDailyK::getClose).toList();
            boolean high60 = MaUtil.highK(closeList, 60);
            if (!high60) {
                log.info("不满足 60日新高: {}", em.getF14Name());
                continue;
            }

            int kLast = kList.size() - 1;
            EmDailyK k = kList.get(kLast);
            log.info("{}", JSON.toJSONString(k));

            BigDecimal dayAmtTop10 = emClient.amtTop10p(kList);
            BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
            BigDecimal fAmt = hourAmt.multiply(BDUtil.b0_1);
            if (fAmt.compareTo(BDUtil.B1000W) < 0) {
                log.info("fAmt={} 成交额小于1000万: {}", fAmt, em.getF14Name());
                continue;
            }

            BigDecimal fx = em.getF6Amt().divide(fAmt, 2, RoundingMode.HALF_UP);
            if (fx.compareTo(BDUtil.B5) < 0) {
                log.info("成交额相比小于f_amt 5倍: {}", em.getF14Name());
                continue;
            }
            names.add(k.getName() + k.getCode()
                    + " amt=" + BDUtil.amtHuman(em.getF6Amt()) + "亿"
                    + " m1=" + BDUtil.amtHuman(fAmt)
                    + " fx=" + fx + "倍");
        }

        if (names.size() > 0) {
            log.info("开盘竞价: {}", names);
            StringBuilder content = new StringBuilder("开盘竞价");
            for (String name : names) {
                content.append("<br>").append(name);
            }
            String encode = URLEncoder.encode(content.toString(), StandardCharsets.UTF_8);
            wxUtil.send(encode);
        }

        for (EmCList emCList : openList) {
            OpenEmCListEntity entity = copyOpen(emCList, LocalDateTime.now());
            log.info("保存开盘信息: {}", entity);
            openEmCListRepo.saveAndFlush(entity);
            log.info("保存开盘信息成功: {}", entity.getF14Name());
        }


    }

    public OpenEmCListEntity copyOpen(EmCList emCList, LocalDateTime now) {
        OpenEmCListEntity entity = new OpenEmCListEntity();

        entity.setF2Close(emCList.getF2Close());//    private BigDecimal f2Close;//最新价//        "f2": 11.9,
        entity.setF3Pct(emCList.getF3Pct());//    private BigDecimal f3Pct;//涨跌幅//        "f3": -0.92,
        entity.setF4Chg(emCList.getF4Chg());//    private BigDecimal f4Chg;//涨跌额//        "f4": -0.11,
        entity.setF5Vol(emCList.getF5Vol());//    private BigDecimal f5Vol;//成交量(手)//        "f5": 305362,
        entity.setF6Amt(emCList.getF6Amt());//    private BigDecimal f6Amt;//成交额//        "f6": 364294854.36,
        entity.setF7Amp(emCList.getF7Amp());//    private BigDecimal f7Amp;//振幅//        "f7": 0.92,
        entity.setF8Turnover(emCList.getF8Turnover());//    private BigDecimal f8Turnover;//换手率//        "f8": 0.16,
        entity.setF9Pe(emCList.getF9Pe());//    private BigDecimal f9Pe;//市盈率(动态)//        "f9": 3.95,
        entity.setF10VolRatio(emCList.getF10VolRatio());//    private BigDecimal f10VolRatio;//量比//        "f10": 2.8,
        entity.setF12Code(emCList.getF12Code());//    private String f12Code;//代码//        "f12": "000001",
        entity.setF14Name(emCList.getF14Name());//    private String f14Name;//名称//        "f14": "平安银行",
        entity.setF15High(emCList.getF15High());//    private BigDecimal f15High;//最高//        "f15": 12.0,
        entity.setF16Low(emCList.getF16Low());//    private BigDecimal f16Low;//最低//        "f16": 11.89,
        entity.setF17Open(emCList.getF17Open());//    private BigDecimal f17Open;//今开//        "f17": 11.99,
        entity.setF18Close(emCList.getF18Close());//    private BigDecimal f18Close;//昨收//        "f18": 12.01,
        entity.setF22Speed(emCList.getF22Speed());//    private BigDecimal f22Speed;//涨速//        "f22": -0.25,
        entity.setF23Pb(emCList.getF23Pb());//    private BigDecimal f23Pb;//市净率//        "f23": 0.61
        entity.setF100bk(emCList.getF100bk());//    private String f100bk;//所属板块//        "f100": "银行"

        entity.setTradeDate(now.toLocalDate().toString().replaceAll("-", ""));//    private String tradeDate;//交易日
        entity.setF1Amt(BDUtil.BN1);//    private BigDecimal f1Amt;//1min 成交额 定量
        entity.setOpenX(BDUtil.BN1);//    private BigDecimal openX;//相比昨日成交额 倍数
        entity.setCreateTime(now);//    private LocalDateTime createTime;
        entity.setUpdateTime(now);//    private LocalDateTime updateTime;

        return entity;
    }

    public List<String> getTheme(String code) {
        List<String> list = CACHE_THEME_MAP.getIfPresent(code);
        if (list == null) {
            List<String> themeList = emClient.coreThemeDetail(code);
            if (themeList.size() > 0) {
                CACHE_THEME_MAP.put(code, themeList);
            }
        }
        return CACHE_THEME_MAP.getIfPresent(code);
    }


    public void themePct() {
        if (!EmClient.tradeTime()) {
            return;
        }

        List<EmCList> list = emClient.getClistDefaultSize(true);
        List<EmCList> pList = list.stream().filter(e ->
                e.getF3Pct().compareTo(BDUtil.B9) > 0
                        && !(e.getF14Name().contains("N") || e.getF14Name().contains("C"))
        ).toList();

        if (pList.size() == 0) return;
        Map<String, BigDecimal> themeSpeedScoreMap = new HashMap<>();
        Map<String, List<String>> themeCodesNameMap = new HashMap<>();

        for (EmCList s : pList) {
            String code = s.getF12Code();
            List<String> themes = getTheme(code);
            if (themes == null || themes.size() == 0) {
                continue;
            }
            for (String theme : themes) {
                if (BLOCK_THEME_SET.contains(theme)) {
                    continue;
                }
                BigDecimal score = themeSpeedScoreMap.get(theme);
                if (score == null) {
                    score = BigDecimal.ZERO;
                }
                score = score.add(s.getF3Pct());
                themeSpeedScoreMap.put(theme, score);

                List<String> codes = themeCodesNameMap.get(theme);
                if (codes == null) {
                    codes = new ArrayList<>();
                }
                codes.add(s.getF14Name() + "_" + s.getF12Code() + "_" + s.getF3Pct());
                themeCodesNameMap.put(theme, codes);
            }

        }


        Map<String, BigDecimal> sortMap = CollectionsUtil.sortByValue(themeSpeedScoreMap, false);
        StringBuilder top100 = new StringBuilder();

        BigDecimal scoreLimit = sortMap.getOrDefault("昨日涨停_含一字", BDUtil.B100);
        sortMap.forEach((t, c) -> {
            log.info("theme={} score={} {}", t, c, String.join(",\t", themeCodesNameMap.get(t)));
            //send wx
            if (c.compareTo(scoreLimit) <= 0) {
                return;
            }

            BigDecimal pre = THEME_SPEED_SCORE_MAP_PRE.get(t) == null ? BigDecimal.ZERO : THEME_SPEED_SCORE_MAP_PRE.get(t);
            BigDecimal chg = c.subtract(pre);
            top100.append(t).append("_").append(c).append("_[").append(chg).append("]").append("<br>");
        });

        if (top100.length() == 0) {
            //top 3
            int limit = Math.min(3, sortMap.size());
            AtomicInteger count = new AtomicInteger(0);
            sortMap.forEach((t, c) -> {
                if (count.getAndIncrement() > limit) {
                    return;
                }
                BigDecimal pre = THEME_SPEED_SCORE_MAP_PRE.get(t) == null ? BigDecimal.ZERO : THEME_SPEED_SCORE_MAP_PRE.get(t);
                BigDecimal chg = c.subtract(pre);
                top100.append(t).append("_").append(c).append("_[").append(chg).append("]").append("<br>");
            });
        }

        top100.append("<br>");
        // 涨停板行情
        ArrayList<EmCList> hsl = new ArrayList<>();
        for (EmCList em : pList) {
            if (em.getF15High().compareTo(em.getF2Close()) == 0) {
                BigDecimal f = new BigDecimal("1.1");
                BigDecimal highStop = BigDecimal.ZERO;
                if (em.getF12Code().startsWith("60") || em.getF12Code().startsWith("0")) {

                } else if (em.getF12Code().startsWith("3") || em.getF12Code().startsWith("68") || em.getF12Code().startsWith("69")) {
                    f = new BigDecimal("1.2");
                } else {
                    f = new BigDecimal("1.3");
                }

                highStop = em.getF18Close().multiply(f).setScale(2, RoundingMode.HALF_UP);
                if (em.getF2Close().compareTo(highStop) >= 0) {
                    hsl.add(em);
                }
            }
        }

        top100.append(hsl.size()).append("<br>");
        hsl.sort(Comparator.comparing(EmCList::getF8Turnover).reversed());
        //HSL 按照每5 分组
        //0-5
        //5-10
        //10-15 ...

        // 创建一个Map，键是getF8Turnover的值除以5的结果，值是EmCList对象的列表
        Map<Integer, List<EmCList>> groupedHsl = new HashMap<>();
        for (EmCList emCList : hsl) {
            int groupKey = emCList.getF8Turnover().intValue() / 5;
            // 如果Map中已经有这个groupKey的键，就把当前的emCList添加到对应的列表中
            // 否则，创建一个新的列表，把当前的emCList添加进去，然后把这个新的列表放到Map中
            if (groupedHsl.containsKey(groupKey)) {
                groupedHsl.get(groupKey).add(emCList);
            } else {
                List<EmCList> newList = new ArrayList<>();
                newList.add(emCList);
                groupedHsl.put(groupKey, newList);
            }
        }
        for (List<EmCList> ll : groupedHsl.values()) {
            AtomicInteger counter = new AtomicInteger(0);
            for (EmCList em : ll) {
                counter.incrementAndGet();
                BigDecimal mv = em.getF6Amt().multiply(BDUtil.B100).divide(em.getF8Turnover(), 0, RoundingMode.HALF_UP);
                top100.append(em.getF14Name()).append("_").append(BDUtil.amtHuman(mv)).append("_").append(BDUtil.amtHuman(em.getF6Amt())).append("_").append(em.getF8Turnover());
                if (counter.intValue() % 4 == 0 && ll.size() > 4) {
                    top100.append("<br>");
                } else {
                    top100.append("  ");
                }
            }

            top100.append("<br>");
            top100.append("<br>");
        }

        String img = "";
        //随机取 hsl 中的数据
        if (hsl.size() > 0) {
            Random rand = new Random();
            int randomNum = rand.nextInt(hsl.size()); // This will generate a random number between 0 (inclusive) and 101 (exclusive), so effectively 0-100.
            EmCList em = hsl.get(randomNum);
            List<String> themes = getTheme(em.getF12Code());
            BigDecimal mv = em.getF6Amt().multiply(BDUtil.B100).divide(em.getF8Turnover(), 0, RoundingMode.HALF_UP);
            top100.append(em.getF14Name()).append("_").append(BDUtil.amtHuman(mv)).append("_").append(BDUtil.amtHuman(em.getF6Amt())).append("_").append(em.getF8Turnover()).append(" ");
            for (String theme : themes) {
                top100.append(theme).append(" ");
            }
            img = (em.getF12Code());
            String url = EmClient.getEastUrl(em.getF12Code());
            top100.append("<br>").append(url);
        }

        String encodeTop100 = URLEncoder.encode(top100.toString(), StandardCharsets.UTF_8);
        wxUtil.send(encodeTop100);
        if (img.length() > 0) {
            imgService.sendImg(img);
        }

        ArrayList<ThemeScoreEntity> themeScoreEntities = new ArrayList<>(sortMap.size());
        LocalDateTime now = LocalDateTime.now();
        sortMap.forEach((t, c) -> {
            BigDecimal pre = THEME_SPEED_SCORE_MAP_PRE.get(t) == null ? BigDecimal.ZERO : THEME_SPEED_SCORE_MAP_PRE.get(t);
            BigDecimal chg = c.subtract(pre);

            ThemeScoreEntity themeScore = new ThemeScoreEntity();
            themeScore.setId(null);
            themeScore.setF1Theme(t);
            themeScore.setF2Score(c);
            themeScore.setF3Chg(chg);
            themeScore.setCreateTime(now);
            themeScoreEntities.add(themeScore);
        });
        themeScoreRepo.saveAllAndFlush(themeScoreEntities);
        //保存pre
        THEME_SPEED_SCORE_MAP_PRE = themeSpeedScoreMap;

        sortMap.forEach((t, c) -> {
            log.info("theme={} score={} {}", t, c, String.join(",\t", themeCodesNameMap.get(t)));
            //send wx
            if (c.compareTo(BDUtil.B100) <= 0) {
                return;
            }
            StringBuilder content = new StringBuilder();
            content.append("theme=").append(t).append(" score=").append(c);
            for (String s : themeCodesNameMap.get(t)) {
                content.append("<br>").append(s);
            }
            String encode = URLEncoder.encode(content.toString(), StandardCharsets.UTF_8);
            wxUtil.send(encode);
        });

    }


    public void runSpeedMonitor(List<EmCList> list) {
        log.debug("runSpeedMonitor map : {}", JSON.toJSONString(MONITOR_CODE_AMT_MAP.keySet()));
        if (MONITOR_CODE_AMT_MAP.size() == 0) return;
        //sort by speed
        List<EmCList> listIn = list.stream().filter(em ->
                MONITOR_CODE_AMT_MAP.containsKey(em.getF12Code())
                        && em.getF3Pct().compareTo(BigDecimal.ZERO) > 0
                        && em.getF22Speed().compareTo(BigDecimal.ZERO) >= 0
        ).sorted(Comparator.comparing(EmCList::getF22Speed).reversed()).toList();


        log.info("\n\n");

        for (EmCList emCList : listIn) {
            if (emCList.getF22Speed().compareTo(BigDecimal.ZERO) > 0) {
                log.info("{},ohlc {} {} {} {},pct={},amt={},fAmt={},speed={}",
                        emCList.getF12Code() + emCList.getF14Name(),
                        emCList.getF17Open(),
                        emCList.getF15High(),
                        emCList.getF16Low(),
                        emCList.getF2Close(),
                        emCList.getF3Pct(),
                        BDUtil.amtHuman(emCList.getF6Amt()),
                        BDUtil.amtHuman(BDUtil.b0_1.multiply(MONITOR_CODE_AMT_MAP.get(emCList.getF12Code()))),
                        emCList.getF22Speed());
            }
        }

        List<EmCList> codes = listIn.stream().filter(em ->

                em.getF3Pct().compareTo(BDUtil.B5) < 0
                        && em.getF22Speed().compareTo(BDUtil.B1) >= 0
        ).toList();


        if (codes.size() == 0) {
            return;
        }

        HashMap<String, Integer> map = new HashMap<>();
        map.put("m1", 65);
//        map.put("m3", 185);
//        map.put("m5", 305);
        for (EmCList em : codes) {
            String key = "[speed]" + em.getF12Code();
            Integer count = WX_SEND_COUNT_MAP.get(key);
            if (count != null && count > 2) {
                log.info("wx send count >= 3: {},移除监控", em.getF14Name());
                continue;
            }

            BigDecimal hourAmt = MONITOR_CODE_AMT_MAP.get(em.getF12Code());
            BigDecimal fAmt = hourAmt.multiply(BDUtil.b0_1);

            if (fAmt.compareTo(BDUtil.B500W) < 0) {
                log.info("fAmt={} 成交额小于500万: {}", fAmt, em.getF14Name());
                continue;
            }

            Map<String, BigDecimal> amtMap = speedService.getWindowAmt(em.getF12Code(), em.getF14Name(), map);
            if (amtMap.size() == 0) {
                continue;
            }

            BigDecimal m1 = amtMap.get("m1");
            log.info("{} fAmt={} m1={}", em.getF14Name(), BDUtil.amtHuman(fAmt), BDUtil.amtHuman(m1));
            if (m1 == null || m1.compareTo(BigDecimal.ZERO) == 0 || m1.compareTo(fAmt) < 0) {
                log.info("{} fAmt={} m1={} m1成交额不够", em.getF14Name(), BDUtil.amtHuman(fAmt), BDUtil.amtHuman(m1));
                continue;
            }

            BigDecimal fx = m1.divide(fAmt, 2, RoundingMode.HALF_UP);
            /*
            2024/1/15 09:32:09
            price=8.06 pct=1.13 speed=1.13
            name=中国核电
            m1=0.13 vol=2869.50万
            20240115_093210
             */
            String content = "[speed]" + em.getF14Name() + em.getF12Code() +
                    "<br>" + "price=" + em.getF2Close() + " pct=" + em.getF3Pct() + " speed=" + em.getF22Speed() +
                    "<br>" + "m1=" + fx + " amt=" + BDUtil.amtHuman(m1);
            String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
            wxUtil.send(encode);
            if (count == null) {
                WX_SEND_COUNT_MAP.put(key, 1);
                imgService.sendImg(em.getF12Code());
            } else {
                WX_SEND_COUNT_MAP.put(key, count + 1);
            }
        }

    }


    public void tradingAlert() {
        if (!EmClient.tradeTime()) {
            return;
        }
        List<EmCList> list = emClient.getClistDefaultSize(true);
        runSpeedMonitor(list);
//        runCrossMa(list);
        tick(EM_CLIST_PRE, list);
        EM_CLIST_PRE = list;
    }

    public void runCrossMa(List<EmCList> list) {

        List<EmCList> lowList = list.stream().filter(
                em -> em.getF17Open().compareTo(em.getF2Close()) < 0
                        && em.getF17Open().compareTo(em.getF16Low()) == 0
                        && em.getF17Open().compareTo(BDUtil.B5) > 0
                        && em.getF3Pct().compareTo(BigDecimal.ZERO) > 0
        ).toList();


        for (EmCList emCList : lowList) {
            String code = emCList.getF12Code();
            if (CODE_MA_BLOCK_SET.contains(code)) {
                continue;
            }

            List<EmDailyK> dailyKs = getKsCache(code);
            if (dailyKs == null || dailyKs.size() < 65) {
                CODE_MA_BLOCK_SET.add(code);
                continue;
            }

            int last = dailyKs.size() - 1;

            EmDailyK k = dailyKs.get(last);
            k.setOpen(emCList.getF17Open());
            k.setClose(emCList.getF2Close());
            k.setLow(emCList.getF16Low());
            k.setHigh(emCList.getF15High());

            Map<String, BigDecimal[]> maMap = MaUtil.ma(dailyKs);

            BigDecimal[] ma5 = maMap.get("ma5");
            BigDecimal[] ma10 = maMap.get("ma10");
            BigDecimal[] ma20 = maMap.get("ma20");
            BigDecimal[] ma30 = maMap.get("ma30");
            BigDecimal[] ma60 = maMap.get("ma60");

            if (ma5 == null || ma10 == null || ma20 == null || ma30 == null || ma60 == null) {
                CODE_MA_BLOCK_SET.add(code);
                continue;
            }

            BigDecimal close = emCList.getF2Close();
            BigDecimal open = emCList.getF17Open();

            if (open.compareTo(ma5[last]) > 0
                    || open.compareTo(ma10[last]) > 0
                    || open.compareTo(ma20[last]) > 0
                    || open.compareTo(ma30[last]) > 0
                    || open.compareTo(ma60[last]) > 0
            ) {
                continue;
            }

            if (close.compareTo(ma5[last]) < 0
                    || close.compareTo(ma10[last]) < 0
                    || close.compareTo(ma20[last]) < 0
                    || close.compareTo(ma30[last]) < 0
                    || close.compareTo(ma60[last]) < 0
            ) {
                continue;
            }


            CODE_MA_BLOCK_SET.add(code);
            String content = "[crossMa]" + emCList.getF14Name() + emCList.getF12Code() + "_" + emCList.getF3Pct();
            String url = EmClient.getEastUrl(emCList.getF12Code());
            content += "<br>" + url;
            String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
            if (emCList.getF22Speed().compareTo(BDUtil.B1) >= 1) {
                wxUtil.send(encode);
                imgService.sendImg(code);
            }
            log.info("cross ma: {}", emCList.getF14Name() + emCList.getF12Code());
        }
    }


    /*
    获取K线, 排除今日
     */
    public List<EmDailyK> getKsCache(String code) {
        // 1, 从缓存拿ks
        List<EmDailyK> list = CODE_KS_CACHE_MAP.get(code);
        if (list == null) {
            //2, 新数据 从接口拿
            list = emClient.getDailyKs(code, LocalDate.now(), 100, true);
            if (list == null || list.size() < 65) {
                //放入Block
                CODE_MA_BLOCK_SET.add(code);
                return Collections.emptyList();
            }
            CODE_KS_CACHE_MAP.put(code, list);
        }

        if (list.size() < 65) {
            //放入Block
            CODE_MA_BLOCK_SET.add(code);
            return Collections.emptyList();
        }

        return list;
    }


    public void tick(List<EmCList> preList, List<EmCList> now) {
        if (preList == null || preList.size() == 0
                || now == null || now.size() == 0) {
            return;
        }

        HashMap<String, EmCList> p = new HashMap<>(preList.size());
        for (EmCList em : preList) {
            p.put(em.getF12Code(), em);
        }

        int w = 10000;
        int up = 0;
        int dn = 0;
        BigDecimal upAmt = BigDecimal.ONE;
        BigDecimal dnAmt = BigDecimal.ONE;

        for (EmCList em : now) {
            EmCList pre = p.get(em.getF12Code());
            BigDecimal amtDiff = em.getF6Amt().subtract(pre.getF6Amt());
            if (em.getF2Close().compareTo(pre.getF2Close()) > 0) {
                upAmt = upAmt.add(amtDiff);
                up++;
            }
            if (em.getF2Close().compareTo(pre.getF2Close()) < 0) {
                dnAmt = dnAmt.add(amtDiff);
                dn++;
            }
        }


        //           (上漲的股票數量/下跌的股票數量)
        //  TRIN= --------------------------------
        //         （上漲的股票的成交量/下跌的股票成交量）

        // 假设 单位资金推动股票数量是一致

        // 那么 r < 1 ,
        // 1 股数相同 ==>当相同数据的股票 ,但是上涨占用的资金更多,说明资金更愿意往上涨的股票上面去
        // 2 amt相同 ==>说明太容易跌了,可能会反转上涨

        // 那么 r > 1 ,
        // 1 股数相同 ==>当相同数据的股票 , 但是下跌占用的资金更多, (抱团, 资金偏好) 说明资金更愿意往下跌的股票上面去
        // 2 amt相同 ==>说明 太容易涨了


        //含意就是 单位资金 推动股票上涨的比例
        BigDecimal upAmtRatio = new BigDecimal(up * w).divide(upAmt, 4, RoundingMode.HALF_UP);
        //含意就是 单位资金 推动股票下跌的比例
        BigDecimal dnAmtRatio = new BigDecimal(dn * w).divide(dnAmt, 4, RoundingMode.HALF_UP);

        BigDecimal r = dnAmtRatio.compareTo(BigDecimal.ZERO) > 0 ? upAmtRatio.divide(dnAmtRatio, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        String type = "----";
        int updn = up - dn;
        BigDecimal dr = BigDecimal.ONE.subtract(r).multiply(BDUtil.B100).abs();
        BigDecimal udr = BigDecimal.ZERO;
        if (updn != 0) {
            //含义是 updn 打出了dr 的幅度
            udr = dr.divide(new BigDecimal(updn).abs(), 4, RoundingMode.HALF_UP);
        }

        if (updn > 100) {
            if (r.compareTo(new BigDecimal("0.95")) <= 0) {
                type = "up配合[抱涨]";
            }
            if (r.compareTo(new BigDecimal("1.05")) >= 0) {
                type = "up背离[抱跌]";
            }
        }

        if (updn < -100) {
            if (r.compareTo(new BigDecimal("0.95")) <= 0) {
                type = "dn背离[抱涨]";
            }
            if (r.compareTo(new BigDecimal("1.05")) >= 0) {
                type = "dn配合[抱跌]";
            }
        }

        log.info("up={}dn={}upA={}dnA={}amt={}TICKP={}TICK={}TRIN={}type={}udr={}",

                String.format("%-8s", up),
                String.format("%-8s", dn),
                String.format("%-8s", BDUtil.amtHuman(upAmt)),
                String.format("%-8s", BDUtil.amtHuman(dnAmt)),
                String.format("%-8s", BDUtil.amtHuman(upAmt.add(dnAmt))),
                String.format("%-8s", BDUtil.p100(new BigDecimal(updn).divide(new BigDecimal(now.size()), 4, RoundingMode.HALF_UP))),
                String.format("%-8s", updn),
                String.format("%-8s", r),
                String.format("%-10s", type),
                udr
        );


    }

}

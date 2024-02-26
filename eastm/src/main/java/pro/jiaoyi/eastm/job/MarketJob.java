package pro.jiaoyi.eastm.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.CollectionsUtil;
import pro.jiaoyi.common.util.EmojiUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.dao.entity.CloseEmCListEntity;
import pro.jiaoyi.eastm.dao.entity.OpenEmCListEntity;
import pro.jiaoyi.eastm.dao.entity.TickEmCListEntity;
import pro.jiaoyi.eastm.dao.repo.CloseEmCListRepo;
import pro.jiaoyi.eastm.dao.repo.OpenEmCListRepo;
import pro.jiaoyi.eastm.dao.repo.ThemeScoreRepo;
import pro.jiaoyi.eastm.dao.repo.TickEmCListRepo;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private CloseEmCListRepo closeEmCListRepo;
    @Resource
    private TickEmCListRepo tickEmCListRepo;

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
    private static final Map<String, BigDecimal> TRIN_MAP = new HashMap<>();
    //    public static final Map<String, List<EmDailyK>> CODE_KS_CACHE_MAP = new HashMap<>();
    public static final AtomicInteger CODE_KS_CACHE_COUNT = new AtomicInteger(0);
    private static List<EmCList> EM_CLIST_PRE = null;
    public static final Set<String> ALLSPEED_BLOCK_SET = new HashSet<>();

    static {
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

    public MarketJob(ThemeScoreRepo themeScoreRepo) {
        this.themeScoreRepo = themeScoreRepo;
    }


    //1. 获取开盘竞价
    @Scheduled(cron = "30 25 9 * * ?")
    public void runOpen() {
        open("");
    }

    @Scheduled(cron = "30 5 15 * * ?")
    public void runClose() {

        if (!TradeTimeUtil.isTradeDay()) {
            return;
        }

        List<EmCList> list = emClient.getClistDefaultSize(true);
        if (list == null || list.isEmpty()) {
            return;
        }
        //过滤 一字板, hsl < 3%
        List<EmCList> yi = list.stream().filter(em ->
                em.getF2Close().compareTo(BigDecimal.ONE) > 0
                        && em.getF3Pct().compareTo(BigDecimal.ONE) > 0
                        && em.getF8Turnover().compareTo(BDUtil.B3) < 0
                        && em.getF2Close().compareTo(em.getF17Open()) == 0
                        && em.getF2Close().compareTo(em.getF15High()) == 0
                        && em.getF2Close().compareTo(em.getF16Low()) == 0
        ).toList();

        if (!yi.isEmpty()) {
            log.info("一字板: {}", yi);
            StringBuilder content = new StringBuilder("一字板");
            for (EmCList emCList : yi) {
                content.append("<br>").append(emCList.getF14Name()).append("_").append(emCList.getF12Code());
            }
            String encode = URLEncoder.encode(content.toString(), StandardCharsets.UTF_8);
            wxUtil.send(encode);
        }

        closeEmCListRepo.deleteAll();
        for (EmCList emCList : list) {
            CloseEmCListEntity entity = copyClose(emCList, LocalDateTime.now());
            log.info("保存close信息: {}", entity);
            closeEmCListRepo.saveAndFlush(entity);
            log.info("保存close成功: {}", entity.getF14Name());
        }
    }

//    @Scheduled(cron = "30 0/10 * * * ?")
//    @Async
//    public void runThemePct() {
//        themePct();
//    }

    @Scheduled(cron = "0 15 06,15 ? * MON-FRI")
    public void initMaMap() {
        ALLSPEED_BLOCK_SET.clear();
        CODE_MA_BLOCK_SET.clear();
        CODE_KS_CACHE_COUNT.set(0);
        WX_SEND_COUNT_MAP.clear();
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

            List<BigDecimal> closeList = kList.stream().map(EmDailyK::getHigh).toList();
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
//            log.info("保存开盘信息: {}", entity);
            openEmCListRepo.saveAndFlush(entity);
//            log.info("保存开盘信息成功: {}", entity.getF14Name());
        }

        //删除昨日数据
        speedService.deleteFenshiM1Pre();
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

    public CloseEmCListEntity copyClose(EmCList emCList, LocalDateTime now) {
        CloseEmCListEntity entity = new CloseEmCListEntity();

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
//        if (img.length() > 0) {
//            imgService.sendImg(img);
//        }

//        ArrayList<ThemeScoreEntity> themeScoreEntities = new ArrayList<>(sortMap.size());
//        LocalDateTime now = LocalDateTime.now();
//        sortMap.forEach((t, c) -> {
//            BigDecimal pre = THEME_SPEED_SCORE_MAP_PRE.get(t) == null ? BigDecimal.ZERO : THEME_SPEED_SCORE_MAP_PRE.get(t);
//            BigDecimal chg = c.subtract(pre);
//
//            ThemeScoreEntity themeScore = new ThemeScoreEntity();
//            themeScore.setId(null);
//            themeScore.setF1Theme(t);
//            themeScore.setF2Score(c);
//            themeScore.setF3Chg(chg);
//            themeScore.setCreateTime(now);
//            themeScoreEntities.add(themeScore);
//        });
//        themeScoreRepo.saveAllAndFlush(themeScoreEntities);
        //保存pre
        THEME_SPEED_SCORE_MAP_PRE = themeSpeedScoreMap;
//
//        sortMap.forEach((t, c) -> {
//            log.info("theme={} score={} {}", t, c, String.join(",\t", themeCodesNameMap.get(t)));
//            //send wx
//            if (c.compareTo(BDUtil.B100) <= 0) {
//                return;
//            }
//            StringBuilder content = new StringBuilder();
//            content.append("theme=").append(t).append(" score=").append(c);
//            for (String s : themeCodesNameMap.get(t)) {
//                content.append("<br>").append(s);
//            }
//            String encode = URLEncoder.encode(content.toString(), StandardCharsets.UTF_8);
//            wxUtil.send(encode);
//        });

    }


    public void runSpeedMonitor(List<EmCList> list) {
        log.debug("runSpeedMonitor map : {}", JSON.toJSONString(MONITOR_CODE_AMT_MAP.keySet()));
        if (MONITOR_CODE_AMT_MAP.isEmpty()) return;
        //sort by speed
        List<EmCList> listIn = list.stream().filter(em ->
                MONITOR_CODE_AMT_MAP.containsKey(em.getF12Code())
                        && em.getF3Pct().compareTo(BigDecimal.ZERO) > 0
                        && em.getF22Speed().compareTo(BigDecimal.ZERO) >= 0
        ).sorted(Comparator.comparing(EmCList::getF22Speed).reversed()).toList();

        for (EmCList emCList : listIn) {
            if (emCList.getF22Speed().compareTo(BigDecimal.ZERO) > 0) {


                BigDecimal m1 = speedService.getFenshiAmtSimple(emCList.getF12Code(), 65);
                BigDecimal fx = m1.divide(BDUtil.b0_1.multiply(MONITOR_CODE_AMT_MAP.get(emCList.getF12Code())), 2, RoundingMode.HALF_UP);
                log.info("{}[{}]  {}[{}] ,m1={}[{}],amt={},fAmt={}",
                        emCList.getF12Code() + emCList.getF14Name(),
                        emCList.getF3Pct(),
                        emCList.getF2Close(),
                        emCList.getF22Speed(),
                        BDUtil.amtHuman(m1),
                        BDUtil.amtHuman(fx),
                        BDUtil.amtHuman(emCList.getF6Amt()),
                        BDUtil.amtHuman(BDUtil.b0_1.multiply(MONITOR_CODE_AMT_MAP.get(emCList.getF12Code())))
                );
            }
        }

        List<EmCList> codes = listIn.stream().filter(em ->
                em.getF3Pct().compareTo(BDUtil.B5) < 0
                        && em.getF22Speed().compareTo(BDUtil.B1) >= 0
        ).toList();


        if (codes.isEmpty()) {
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
            //要求昨天 分时 > 1000 个
            int fc = speedService.countByCode(em.getF12Code(), LocalDate.now());
            String fcs = "";
            if (fc < 1000) {
                log.error("分时amt数据不足1000个: {}", em.getF14Name());
                fcs = "分时amt不足" + fc + "个";
            } else {
                fcs = "分时amt_" + fc + "个";
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
                    "<br>" + "m1=" + fx + " amt=" + BDUtil.amtHuman(m1) +
                    "<br>" + fcs;
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
        if (EM_CLIST_PRE != null && EM_CLIST_PRE.size() > 0) {
            tickByMarket(EM_CLIST_PRE, list);
        }
        EM_CLIST_PRE = list;
        //全市场涨速
        speedService.addAll(list);
//        runAllSpeed(list);
        runFenshiM1(list);
    }
//

    ExecutorService executor = Executors.newFixedThreadPool(10);
    public void runFenshiM1(List<EmCList> list) {
        CompletableFuture.runAsync(() -> {
            try {
                speedService.runFenshiM1(list);
            } catch (Exception e) {
                // 处理异常，例如记录日志或采取其他适当的处理措施
                log.info("runFenshiM1 error: {}", e.getMessage());
            }
        }, executor);
    }


    private void runAllSpeed(List<EmCList> list) {

        BigDecimal b001 = new BigDecimal("0.01");
        List<EmCList> speedList = list.stream().filter(em ->
                em.getF16Low().compareTo(em.getF18Close()) > 0
                        && em.getF2Close().compareTo(em.getF17Open()) > 0
                        && em.getF3Pct().compareTo(BDUtil.B5) < 0

                        && em.getF22Speed().compareTo(BigDecimal.ONE) >= 0

                        && em.getF2Close().compareTo(BDUtil.B5) > 0
                        && em.getF2Close().compareTo(BDUtil.B50) < 0
                        // 最高点 不超过 1%
                        && (em.getF15High().subtract(em.getF2Close()))
                        .divide(em.getF2Close(), 4, RoundingMode.HALF_UP)
                        .compareTo(b001) < 0
        ).toList();

        log.debug("speed size: {}", speedList.size());

        StringBuilder content = new StringBuilder();

        for (EmCList em : speedList) {
            String code = em.getF12Code();
            String key = "[speed]" + em.getF12Code();
            Integer count = WX_SEND_COUNT_MAP.get(key);
            if (count != null && count > 3) {
                continue;
            }

            if (ALLSPEED_BLOCK_SET.contains(code)) {
                continue;
            }

            //要求昨天 分时 > 1000 个
//            int fc = speedService.countByCode(code, LocalDate.now());
//            if (fc < 1000) {
//                ALLSPEED_BLOCK_SET.add(code);
//                continue;
//            }

            BigDecimal amtHour = emClient.getAmtHour(code);
            BigDecimal fAmt = amtHour.multiply(BDUtil.b0_1);
            BigDecimal m1 = speedService.getFenshiAmtSimple(em.getF12Code(), 65);
            if (m1 == null || m1.compareTo(BigDecimal.ZERO) == 0 || m1.compareTo(BDUtil.B500W) < 0 || m1.compareTo(fAmt) < 0) {
                continue;
            }

            List<EmDailyK> kList = emClient.getDailyKs(code, LocalDate.now(), 100, false);
            if (kList == null || kList.size() < 100) {
                ALLSPEED_BLOCK_SET.add(code);
                continue;
            }
            //排除昨日涨停
//            EmDailyK pre = kList.get(kList.size() - 1 - 1);
//            if (pre.getPct().compareTo(BDUtil.B9) > 0 || pre.getHsl().compareTo(BDUtil.B5) > 0) {
//                ALLSPEED_BLOCK_SET.add(code);
//                continue;
//            }
            List<BigDecimal> closeList = kList.stream().map(EmDailyK::getClose).toList();
            boolean high60 = MaUtil.highK(closeList, 95);
            if (!high60) {
                ALLSPEED_BLOCK_SET.add(code);
                log.debug("不满足 60日新高: {}", em.getF14Name());
                continue;
            }

            // 检验 分时成交额是否过于稀疏

            BigDecimal fx = m1.divide(fAmt, 2, RoundingMode.HALF_UP);

            String wx = "[speed]" + em.getF14Name() + em.getF12Code() +
                    "<br>" + "price=" + em.getF2Close() + " pct=" + em.getF3Pct() + " speed=" + em.getF22Speed() +
                    "<br>" + "m1=" + fx + " amt=" + BDUtil.amtHuman(m1);

            content.append("[speed]").append(em.getF12Code()).append(em.getF14Name())
                    .append("_").append(em.getF2Close()).append("[").append(em.getF3Pct()).append("]")
                    .append("_").append("m1=").append(BDUtil.amtHuman(m1)).append("_").append("fx=").append(fx)
                    .append("<br>");

            if (count == null) {
                WX_SEND_COUNT_MAP.put(key, 1);
                imgService.sendImg(em.getF12Code());
            } else {
                WX_SEND_COUNT_MAP.put(key, count + 1);
            }
            log.info("{}", wx.replaceAll("<br>", " "));
        }
        if (content.length() > 0) {
            String encode = URLEncoder.encode(content.toString(), StandardCharsets.UTF_8);
            wxUtil.send(encode);
        }

    }


//    public void runCrossMa(List<EmCList> list) {
//
//        List<EmCList> lowList = list.stream().filter(
//                em -> em.getF17Open().compareTo(em.getF2Close()) < 0
//                        && em.getF17Open().compareTo(em.getF16Low()) == 0
//                        && em.getF17Open().compareTo(BDUtil.B5) > 0
//                        && em.getF3Pct().compareTo(BigDecimal.ZERO) > 0
//        ).toList();
//
//
//        for (EmCList emCList : lowList) {
//            String code = emCList.getF12Code();
//            if (CODE_MA_BLOCK_SET.contains(code)) {
//                continue;
//            }
//
//            List<EmDailyK> dailyKs = getKsCache(code);
//            if (dailyKs == null || dailyKs.size() < 65) {
//                CODE_MA_BLOCK_SET.add(code);
//                continue;
//            }
//
//            int last = dailyKs.size() - 1;
//
//            EmDailyK k = dailyKs.get(last);
//            k.setOpen(emCList.getF17Open());
//            k.setClose(emCList.getF2Close());
//            k.setLow(emCList.getF16Low());
//            k.setHigh(emCList.getF15High());
//
//            Map<String, BigDecimal[]> maMap = MaUtil.ma(dailyKs);
//
//            BigDecimal[] ma5 = maMap.get("ma5");
//            BigDecimal[] ma10 = maMap.get("ma10");
//            BigDecimal[] ma20 = maMap.get("ma20");
//            BigDecimal[] ma30 = maMap.get("ma30");
//            BigDecimal[] ma60 = maMap.get("ma60");
//
//            if (ma5 == null || ma10 == null || ma20 == null || ma30 == null || ma60 == null) {
//                CODE_MA_BLOCK_SET.add(code);
//                continue;
//            }
//
//            BigDecimal close = emCList.getF2Close();
//            BigDecimal open = emCList.getF17Open();
//
//            if (open.compareTo(ma5[last]) > 0
//                    || open.compareTo(ma10[last]) > 0
//                    || open.compareTo(ma20[last]) > 0
//                    || open.compareTo(ma30[last]) > 0
//                    || open.compareTo(ma60[last]) > 0
//            ) {
//                continue;
//            }
//
//            if (close.compareTo(ma5[last]) < 0
//                    || close.compareTo(ma10[last]) < 0
//                    || close.compareTo(ma20[last]) < 0
//                    || close.compareTo(ma30[last]) < 0
//                    || close.compareTo(ma60[last]) < 0
//            ) {
//                continue;
//            }
//
//
//            CODE_MA_BLOCK_SET.add(code);
//            String content = "[crossMa]" + emCList.getF14Name() + emCList.getF12Code() + "_" + emCList.getF3Pct();
//            String url = EmClient.getEastUrl(emCList.getF12Code());
//            content += "<br>" + url;
//            String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
//            if (emCList.getF22Speed().compareTo(BDUtil.B1) >= 1) {
//                wxUtil.send(encode);
//                imgService.sendImg(code);
//            }
//            log.info("cross ma: {}", emCList.getF14Name() + emCList.getF12Code());
//        }
//    }


    /*
    获取K线, 排除今日
     */
//    public List<EmDailyK> getKsCache(String code) {
//        // 1, 从缓存拿ks
//        List<EmDailyK> list = CODE_KS_CACHE_MAP.get(code);
//        if (list == null) {
//            //2, 新数据 从接口拿
//            list = emClient.getDailyKs(code, LocalDate.now(), 100, true);
//            if (list == null || list.size() < 65) {
//                //放入Block
//                CODE_MA_BLOCK_SET.add(code);
//                return Collections.emptyList();
//            }
//            CODE_KS_CACHE_MAP.put(code, list);
//        }
//
//        if (list.size() < 65) {
//            //放入Block
//            CODE_MA_BLOCK_SET.add(code);
//            return Collections.emptyList();
//        }
//
//        return list;
//    }


    private static String TRIN_SIDE_PRE = "";
    private static Integer TRIN_SIDE_COUNTER = 0;

    public void tickByMarket(List<EmCList> preList, List<EmCList> now) {

        if (LocalTime.now().isAfter(LocalTime.of(14, 57)) ||
                LocalTime.now().isBefore(LocalTime.of(9, 30))) {
            return;
        }

        log.info("");
        tick(preList, now, "all");//1 全市
        tick(preList, now, "sh");//2 上证
        tick(preList, now, "sz");//3 深证
        tick(preList, now, "cf");//4 创业板
//        tick(preList, now, "kc");
//        tick(preList, now, "bj");

    }

    public void tick(List<EmCList> preList, List<EmCList> now, String market) {
        //要区分不同板块

        if (preList == null || preList.size() == 0
                || now == null || now.size() == 0) {
            return;
        }

        boolean wxSendFlag = false;
        if (market.equals("sh")) {
            log.debug("上海 market: {}", market);
            preList = preList.stream().filter(e -> e.getF12Code().startsWith("6")).toList();
            now = now.stream().filter(e -> e.getF12Code().startsWith("6")).toList();
            wxSendFlag = true;
        } else if (market.equals("sz")) {
            log.debug("深圳 market: {}", market);
            preList = preList.stream().filter(e -> e.getF12Code().startsWith("0") || e.getF12Code().startsWith("3")).toList();
            now = now.stream().filter(e -> e.getF12Code().startsWith("0") || e.getF12Code().startsWith("3")).toList();
            wxSendFlag = true;
        } else if (market.equals("cf")) {
            log.debug("创业板 market: {}", market);
            preList = preList.stream().filter(e -> e.getF12Code().startsWith("3")).toList();
            now = now.stream().filter(e -> e.getF12Code().startsWith("3")).toList();
        } else if (market.equals("kc")) {
            log.debug("科创板 market: {}", market);
            preList = preList.stream().filter(e -> e.getF12Code().startsWith("68") || e.getF12Code().startsWith("69")).toList();
            now = now.stream().filter(e -> e.getF12Code().startsWith("68") || e.getF12Code().startsWith("69")).toList();
        } else if (market.equals("bj")) {
            log.debug("北郊所 market: {}", market);
            preList = preList.stream().filter(e -> e.getF12Code().startsWith("8") || e.getF12Code().startsWith("4")).toList();
            now = now.stream().filter(e -> e.getF12Code().startsWith("8") || e.getF12Code().startsWith("4")).toList();
        } else {
            log.debug("market: {}", market);
            wxSendFlag = true;
        }

        //全市场 绝对涨跌数量
        int absUp = 0;//pct
        int absDn = 0;//pct
        int absZ = 0;//pct

        //全市场 按实体涨跌数量
        int openUp = 0;//tick up
        int openDn = 0;//tick down
        int openZ = 0;//tick zero


        //tick级 涨跌数量
        int tickUp = 0;
        int tickDn = 0;
        int tickZ = 0;


        BigDecimal tickUpAmt = BigDecimal.ZERO;
        BigDecimal tickDnAmt = BigDecimal.ZERO;
        BigDecimal tickZAmt = BigDecimal.ZERO;


        Map<String, EmCList> p = new HashMap<>(preList.size());
        for (EmCList em : preList) {
            p.put(em.getF12Code(), em);
            if (em.getF3Pct().compareTo(BDUtil.B1) > 0) {
                absUp++;
            } else if (em.getF3Pct().compareTo(BDUtil.BN1) < 0) {
                absDn++;
            } else {
                absZ++;
            }

            if (em.getF2Close().compareTo(em.getF17Open()) > 0) {
                openUp++;
            } else if (em.getF2Close().compareTo(em.getF17Open()) < 0) {
                openDn++;
            } else {
                openZ++;
            }
        }

        for (EmCList em : now) {
            EmCList pre = p.get(em.getF12Code());
            if (pre == null) continue;

            BigDecimal amtDiff = em.getF6Amt().subtract(pre.getF6Amt());
            if (em.getF2Close().compareTo(pre.getF2Close()) > 0) {
                tickUpAmt = tickUpAmt.add(amtDiff);
                tickUp++;
                continue;
            }
            if (em.getF2Close().compareTo(pre.getF2Close()) < 0) {
                tickDnAmt = tickDnAmt.add(amtDiff);
                tickDn++;
                continue;
            }
            tickZ++;
            tickZAmt = tickZAmt.add(amtDiff);
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

        int tick = tickUp - tickDn;// up - down
        BigDecimal tickPct = new BigDecimal(tick).divide(new BigDecimal(now.size()), 4, RoundingMode.HALF_UP);

        BigDecimal amt = tickUpAmt.add(tickDnAmt).add(tickZAmt);
        BigDecimal upAmtRatio = amt.compareTo(BigDecimal.ZERO) == 0 ? amt : tickUpAmt.divide(amt, 4, RoundingMode.HALF_UP);
        BigDecimal dnAmtRatio = amt.compareTo(BigDecimal.ZERO) == 0 ? amt : tickDnAmt.divide(amt, 4, RoundingMode.HALF_UP);
        BigDecimal zAmtRatio = amt.compareTo(BigDecimal.ZERO) == 0 ? amt : tickZAmt.divide(amt, 4, RoundingMode.HALF_UP);

        BigDecimal sudr = tickDn == 0 ? BigDecimal.ZERO : new BigDecimal(tickUp).divide(new BigDecimal(tickDn), 2, RoundingMode.HALF_UP);
        BigDecimal audr = dnAmtRatio.compareTo(BigDecimal.ZERO) == 0 ? dnAmtRatio : upAmtRatio.divide(dnAmtRatio, 2, RoundingMode.HALF_UP);
        BigDecimal nudr = audr.subtract(sudr);


        BigDecimal trin = ((tickDnAmt.compareTo(BigDecimal.ZERO) == 0 || tickUpAmt.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO : sudr.divide(tickUpAmt.divide(tickDnAmt, 4, RoundingMode.HALF_UP), 2, RoundingMode.HALF_UP));

        TickEmCListEntity t = new TickEmCListEntity();
        t.setId(null);
        t.setAbsUp(absUp);//pct
        t.setAbsDn(absDn);//pct
        t.setAbsZ(absZ);//pct

        t.setOpenUp(openUp);
        t.setOpenDn(openDn);
        t.setOpenZ(openZ);

        t.setTickUp(tickUp);
        t.setTickDn(tickDn);
        t.setTickZ(tickZ);

        t.setTick(tick);// up - down
        t.setTickPct(tickPct);// (up - down) / total

        t.setTickUpAmt(tickUpAmt);
        t.setTickDnAmt(tickDnAmt);
        t.setTickZAmt(tickZAmt);

        t.setUpAmtRatio(upAmtRatio);// up - down
        t.setDnAmtRatio(dnAmtRatio);// (up - down) / total
        t.setZAmtRatio(zAmtRatio);// (up - down) / total

        t.setSudr(sudr);
        t.setAudr(audr);
        t.setNudr(nudr);

        t.setTrin(trin);//(tup/aup) / (tdn/adn)
        t.setCreateTime(LocalDateTime.now());
        t.setMarket(market);

        tickEmCListRepo.saveAndFlush(t);

        if (trin.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        String side = (nudr.compareTo(BigDecimal.ZERO) > 0 ? EmojiUtil.UPs : EmojiUtil.DOWNs)
                + (trin.compareTo(new BigDecimal("1.05")) < 0
                && trin.compareTo(new BigDecimal("0.95")) > 0 ? ("-") : (nudr.multiply(new BigDecimal(tick)).compareTo(BigDecimal.ZERO) > 0 ? EmojiUtil.Right : "x"));

        if (side.equals(TRIN_SIDE_PRE)) {
            TRIN_SIDE_COUNTER++;
        } else {
            TRIN_SIDE_COUNTER = 1;
        }
        TRIN_SIDE_PRE = side;

        BigDecimal h = TRIN_MAP.get("H");
        BigDecimal l = TRIN_MAP.get("L");
        if (h == null) {
            h = BigDecimal.ONE;
        }
        if (l == null) {
            l = BigDecimal.ONE;
        }

        //h > 1.2 开始,
        //l < 0.8 开始

        String name = getMarketName(market);
        if (trin.compareTo(h) > 0) {
            h = trin;
            TRIN_MAP.put("H", h);
            if (h.compareTo(new BigDecimal("2")) > 0) {
                String content = "trin=[" + trin + "]日内新高,超卖" + "[" + name + "]";
//                String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
//                wxUtil.send(encode);
                log.info("{}", content);
//                trinPctRecent(market);
            }
        }

        if (trin.compareTo(l) < 0) {
            l = trin;
            TRIN_MAP.put("L", l);
            if (l.compareTo(new BigDecimal("0.5")) < 0) {
                String content = "trin=[" + trin + "]日内新低,超买" + "[" + name + "]";
//                String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
//                wxUtil.send(encode);
                log.info("{}", content);
//                trinPctRecent(market);
            }
        }

        if (trin.compareTo(BDUtil.B3) > 0 && wxSendFlag) {
            String content = "[底部有效]trin=[" + trin + "]超卖" + "[" + name + "]";
            String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
            wxUtil.send(encode);
            trinPctRecent(market);
        }

        if (trin.compareTo(new BigDecimal("0.3")) < 0 && wxSendFlag) {
            String content = "[顶部有效]trin=[" + trin + "]超买" + "[" + name + "]";
            String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
            wxUtil.send(encode);
            trinPctRecent(market);
        }

        log.info("{} 绝对={} 实体={} 秒={} amt={} tick={} sudr={} audr={} trin={}",
                String.format("%-4s", "[" + market + "]"),
                String.format("%-10s", absUp + ":" + absDn),
                String.format("%-10s", openUp + ":" + openDn),
                String.format("%-10s", tickUp + ":" + tickDn),
                String.format("%-15s", BDUtil.amtHuman(tickUpAmt) + ":" + BDUtil.amtHuman(tickDnAmt)),
                String.format("%-15s", tick + "[" + BDUtil.p100(tickPct) + "%]"),
                String.format("%-8s", sudr),
                String.format("%-15s", (audr) + "[" + (nudr) + "]"),
                String.format("%-8s", trin + "[" + side + "][" + TRIN_SIDE_COUNTER + "]")
        );
    }

    private String st(int width, String s) {
        return String.format("%-" + width + "s", s);
    }

    //查询trin 最近 10分钟 5钟 3钟 的比例
    public void trinPctRecent(String market) {


        if (LocalTime.now().isAfter(LocalTime.of(14, 57)) ||
                LocalTime.now().isBefore(LocalTime.of(9, 30))) {
            return;
        }
//        List<TickEmCListEntity> m10 = tickEmCListRepo.findByCreateTimeAfter(LocalDateTime.now().minusMinutes(10));
        List<TickEmCListEntity> m10 = tickEmCListRepo.findByCreateTimeAfterAndMarketEquals(LocalDateTime.now().minusMinutes(10), market);
        if (m10.size() < 50) {
            return;
        }
        List<TickEmCListEntity> m5 = m10.stream().filter(e -> e.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(5))).toList();
        List<TickEmCListEntity> m1 = m5.stream().filter(e -> e.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(1))).toList();

        //trin > 1 的数量
        long trinUp = m10.stream().filter(e -> e.getTrin().compareTo(BigDecimal.ONE) > 0).count();
        long trinDn = m10.size() - trinUp;

        long trinUp5 = m5.stream().filter(e -> e.getTrin().compareTo(BigDecimal.ONE) > 0).count();
        long trinDn5 = m5.size() - trinUp5;

        long trinUp1 = m1.stream().filter(e -> e.getTrin().compareTo(BigDecimal.ONE) > 0).count();
        long trinDn1 = m1.size() - trinUp1;

        // dn up 比例 如果up是0 , 给一个 -1
        BigDecimal trinPct = new BigDecimal(trinDn).divide(new BigDecimal(m10.size()), 2, RoundingMode.HALF_UP);
        BigDecimal trinPct5 = new BigDecimal(trinDn5).divide(new BigDecimal(m5.size()), 2, RoundingMode.HALF_UP);
        BigDecimal trinPct1 = new BigDecimal(trinDn1).divide(new BigDecimal(m1.size()), 2, RoundingMode.HALF_UP);

        String name = getMarketName(market);
        String content = "t10_t5_t1=[" + trinPct + "]_[" + trinPct5 + "]_[" + trinPct1 + "]" + "[" + name + "]";
        String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
        wxUtil.send(encode);
    }

    String getMarketName(String market) {

        String name = "全市场";
        if (market.equals("sh")) {
            name = "上证";
        } else if (market.equals("sz")) {
            name = "深证";
        } else if (market.equals("cf")) {
            name = "创业板";
        } else if (market.equals("kc")) {
            name = "科创板";
        } else if (market.equals("bj")) {
            name = "北郊所";
        }
        return name;
    }
}

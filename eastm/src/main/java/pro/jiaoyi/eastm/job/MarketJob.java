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
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.config.VipIndexEnum;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.dao.entity.OpenEmCListEntity;
import pro.jiaoyi.eastm.dao.entity.ThemeScoreEntity;
import pro.jiaoyi.eastm.dao.repo.OpenEmCListRepo;
import pro.jiaoyi.eastm.dao.repo.ThemeScoreRepo;
import pro.jiaoyi.eastm.model.EastSpeedInfo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.model.fenshi.DetailTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class MarketJob {
    @Resource
    private EmClient emClient;

    @Resource
    private OpenEmCListRepo openEmCListRepo;
    @Resource
    private WxUtil wxUtil;

    //1. 获取开盘竞价
    @Scheduled(cron = "30 25 9 * * ?")
    public void open() {
        open("");
    }

    /**
     * 穿越均线
     * 1, ma5 之上
     * 2, 穿越碰个均线
     */
    public void crossMa() {
        String[] days = {"BREAKUP_MA_5DAYS"};
        emClient.crossMa(days);
    }

    public void open(String source) {
        log.info("开盘竞价");

        // 1. 获取今日是否开盘
        List<EmDailyK> ks = emClient.getDailyKs(VipIndexEnum.index_000001.getCode(), LocalDate.now(), 100, true);
        if (ks == null || ks.size() == 0) {
            return;
        }
        int last = ks.size() - 1;
        String tradeDate = ks.get(last).getTradeDate();
        if (LocalDate.now().toString().replace("-", "").equals(tradeDate)) {
            // 今日开盘
            log.info("今日开盘");
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
                int kLast = kList.size() - 1;
                EmDailyK k = kList.get(kLast);
                log.info("{}", JSON.toJSONString(k));
                if (!tradeDate.equalsIgnoreCase(k.getTradeDate())) {
                    log.info("非今日数据: {}", em.getF14Name());
                    continue;
                }

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


    //获取概念
    public void con() {

        List<EmCList> emCLists = emClient.getClistDefaultSize(true);
        if (emCLists == null || emCLists.size() == 0) {
            return;
        }


//        List<EmCList> b9List = emCLists.stream()
//                .filter(em -> em.getF3Pct().compareTo(BDUtil.B9) >= 0).toList();
//
//        map(b9List);

        List<EmCList> stopList = emCLists.stream().filter(em -> {
            BigDecimal preClose = em.getF18Close();
            BigDecimal close = em.getF2Close();
            if (close.compareTo(preClose) <= 0) {
                return false;
            }

            BigDecimal f = new BigDecimal("1.2");
            if (em.getF12Code().startsWith("60") || em.getF12Code().startsWith("0")) {
                f = new BigDecimal("1.1");
            }

            BigDecimal highStop = preClose.multiply(f).setScale(2, RoundingMode.HALF_UP);
            return close.compareTo(highStop) >= 0;
        }).toList();

        map(stopList);

    }

    public List<String> getTheme(String code) {
        List<String> list = THEME_MAP.getIfPresent(code);
        if (list == null) {
            List<String> themeList = emClient.coreThemeDetail(code);
            if (themeList.size() > 0) {
                THEME_MAP.put(code, themeList);
            }
        }

        return THEME_MAP.getIfPresent(code);
    }

    public static final Cache<String, List<String>> THEME_MAP = Caffeine.newBuilder()
            .expireAfterWrite(600, TimeUnit.MINUTES)
            .maximumSize(5000).build();

    public void map(List<EmCList> list) {
        //数据处理
        //统计 概念, 按 数量排序
        //概念-> 数量
        Map<String, Integer> themeCountMap = new ConcurrentHashMap<>();
        Map<String, List<String>> themeCodeMap = new ConcurrentHashMap<>();
        for (EmCList em : list) {
            List<String> theme = getTheme(em.getF12Code());
            if (theme == null || theme.size() == 0) continue;


            for (String t : theme) {
                //统计数量
                themeCountMap.merge(t, 1, Integer::sum);

                //主题股本映射
                List<String> codes = themeCodeMap.get(t);
                if (codes == null) {
                    codes = new ArrayList<>();
                }
                codes.add(em.getF12Code() + "_" + em.getF14Name() + "_" + em.getF3Pct());
                themeCodeMap.put(t, codes);
            }
        }

        //count map sort by value
        Map<String, Integer> sortMap = CollectionsUtil.sortByValue(themeCountMap, false);
        log.info("板块维度");
        sortMap.forEach((t, c) -> {
            log.debug("theme={} count={} names={}", t, c, JSON.toJSONString(themeCodeMap.get(t)));
        });

        log.info("股票维度");
        list.forEach(em -> {
            List<String> theme = getTheme(em.getF12Code());
            if (theme == null || theme.size() == 0) return;


            HashMap<String, Integer> tmap = new HashMap<>();
            for (String t : theme) {
                tmap.put(t, themeCountMap.get(t));
            }
            Map<String, Integer> sortTMap = CollectionsUtil.sortByValue(tmap, false);

            StringBuilder ts = new StringBuilder();
            sortTMap.forEach((t, c) -> {
                ts.append(t).append("[").append(c).append("] ");
            });
            log.debug("code={} name={} theme={}", em.getF12Code(), em.getF14Name() + "_" + em.getF3Pct() + "_" + BDUtil.amtHuman(em.getF6Amt()), ts);
        });

        StringBuilder csv = new StringBuilder();
        ArrayList<EmCList> sortList = new ArrayList<>(list);
        sortList.sort(Comparator.comparing(EmCList::getF12Code).reversed());

        csv.append("概念").append(",").append("数量").append(",");
        for (EmCList emCList : sortList) {
            csv.append(emCList.getF12Code()).append(emCList.getF14Name()).append(",");
        }
        csv.append("\n");


        sortMap.forEach((t, c) -> {
            StringBuilder row = new StringBuilder();
            row.append(t).append(",").append(c).append(",");
            for (EmCList em : sortList) {
                List<String> codes = getTheme(em.getF12Code());
                if (!codes.contains(t)) {
                    row.append("-,");
                } else {
                    row.append(em.getF14Name()).append("_").append(t).append("_").append(c).append(",");
                }
            }
            log.debug("{}", row);
            csv.append(row).append("\n");
        });

        log.info("\n{}", csv);
        String time = DateUtil.tsToStr(System.currentTimeMillis(), DateUtil.PATTERN_yyyyMMdd_HHmmss);
        FileUtil.writeToFile("csv/概念_" + time + ".csv", csv.toString());
        FileUtil.writeToFile("csv/概念" + ".csv", csv.toString());
    }

    public static final HashSet<String> BLOCK_THEME_SET = new HashSet<>();

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

    @Resource
    private EmRealTimeClient emRealTimeClient;
    Cache<String, BigDecimal> AMT_M1_MAP = Caffeine.newBuilder()
            .expireAfterWrite(600, TimeUnit.MINUTES)
            .maximumSize(1000).build();

    Cache<String, String> BLOCK_CODE_MAP = Caffeine.newBuilder()
            .expireAfterWrite(600, TimeUnit.MINUTES)
            .maximumSize(5000).build();
    Cache<String, Integer> WX_SEND_MAP = Caffeine.newBuilder()
            .expireAfterWrite(600, TimeUnit.MINUTES)
            .maximumSize(5000).build();

    Cache<LocalDate, Integer> TRADE_DAY_MAP = Caffeine.newBuilder()
            .expireAfterWrite(600, TimeUnit.MINUTES)
            .maximumSize(2).build();


    //    @Scheduled(fixedRate = 5000L)
//    @Async
    public void speedUp() {
        //排除 周六 周日
        if (LocalDate.now().getDayOfWeek().getValue() > 5) {
            return;
        }

        //要求 9:30 - 11:30 , 13:00 - 15:00
        LocalTime time = LocalTime.now();
        boolean am = time.isAfter(LocalTime.of(9, 30)) && time.isBefore(LocalTime.of(11, 30));
        boolean pm = time.isAfter(LocalTime.of(13, 0)) && time.isBefore(LocalTime.of(14, 58));
        if (am || pm) {
            log.info("speedUp time: {}", time);
        } else {
            return;
        }

        //判断是否为交易日
        //1, 拿缓存判断
        //2, 拿接口判断
        // -1 休息日 1 交易日
        Integer tradeDay = TRADE_DAY_MAP.getIfPresent(LocalDate.now());
        if (tradeDay == null) {
            log.info("未获取上证指数: {}", LocalDate.now());
            //获取上证指数
            List<EmDailyK> ks = emClient.getDailyKs(VipIndexEnum.index_000001.getCode(), LocalDate.now(), 100, true);
            if (ks == null || ks.size() == 0) {
                log.error("获取上证指数失败: {}", LocalDate.now());
                TRADE_DAY_MAP.put(LocalDate.now(), -1);
                return;
            }

            String tradeDate = ks.get(ks.size() - 1).getTradeDate();
            if (!LocalDate.now().toString().replace("-", "").equals(tradeDate)) {
                log.info("非交易日: {}", tradeDate);
                TRADE_DAY_MAP.put(LocalDate.now(), -1);
                return;
            } else {
                TRADE_DAY_MAP.put(LocalDate.now(), 1);
            }

        } else if (tradeDay == -1) {
            log.info("not交易日: {}", LocalDate.now());
            return;
        }


        List<EastSpeedInfo> speedTop = emRealTimeClient.getSpeedTop(100, false);
        if (speedTop == null || speedTop.size() == 0) {
            return;
        }

        List<EastSpeedInfo> top1 = speedTop.stream().filter(e -> e.getSpeed_f22().compareTo(BDUtil.B1) > 0
                && e.getPct_f3().compareTo(BigDecimal.ZERO) > 0).toList();
        log.info("speedTop 1% size: {}", top1.size());

        for (EastSpeedInfo eastSpeedInfo : top1) {
            check(eastSpeedInfo);
        }


    }

    private static Map<String, BigDecimal> themeSpeedScoreMapPre = new HashMap<>();


    @Resource
    private ThemeScoreRepo themeScoreRepo;

    @Scheduled(cron = "30 0/5 * * * ?")
    @Async
    public void themePct() {
        if (!EmClient.tradeTime()) {
            return;
        }


        List<EmCList> list = emClient.getClistDefaultSize(true);
        List<EmCList> pList = list.stream().filter(e ->
                e.getF3Pct().compareTo(BDUtil.B9) > 0
                        && !(e.getF14Name().contains("N") || e.getF14Name().contains("C"))
        ).toList();
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
                ;
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

            BigDecimal pre = themeSpeedScoreMapPre.get(t) == null ? BigDecimal.ZERO : themeSpeedScoreMapPre.get(t);
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
                BigDecimal pre = themeSpeedScoreMapPre.get(t) == null ? BigDecimal.ZERO : themeSpeedScoreMapPre.get(t);
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
            String url = EmClient.getEastUrl(em.getF12Code());
            top100.append("<br>").append(url);
        }


        String encodeTop100 = URLEncoder.encode(top100.toString(), StandardCharsets.UTF_8);

        wxUtil.send(encodeTop100);

        ArrayList<ThemeScoreEntity> themeScoreEntities = new ArrayList<>(sortMap.size());
        LocalDateTime now = LocalDateTime.now();
        sortMap.forEach((t, c) -> {
            BigDecimal pre = themeSpeedScoreMapPre.get(t) == null ? BigDecimal.ZERO : themeSpeedScoreMapPre.get(t);
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
        themeSpeedScoreMapPre = themeSpeedScoreMap;


        if (LocalTime.now().isBefore(LocalTime.of(14, 59))) {
            return;
        }
        sortMap.forEach((t, c) -> {
            log.info("theme={} score={} {}", t, c, String.join(",\t", themeCodesNameMap.get(t)));
            //send wx
            if (c.compareTo(BDUtil.B100) <= 0) {
                return;
            }
            StringBuilder content = new StringBuilder();// + "<br>" + String.join(",\t", themeCodesNameMap.get(t));
            content.append("theme=").append(t).append(" score=").append(c);
            for (String s : themeCodesNameMap.get(t)) {
                content.append("<br>").append(s);
            }
            String encode = URLEncoder.encode(content.toString(), StandardCharsets.UTF_8);
            wxUtil.send(encode);
        });

    }

    @Async
    public void theme(List<EastSpeedInfo> speedTop) {

        Map<String, BigDecimal> themeSpeedScoreMap = new HashMap<>();
        Map<String, List<String>> themeCodesNameMap = new HashMap<>();

        for (EastSpeedInfo s : speedTop) {

            String code = s.getCode_f12();

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
                score = score.add(s.getSpeed_f22());
                themeSpeedScoreMap.put(theme, score);

                List<String> codes = themeCodesNameMap.get(theme);
                if (codes == null) {
                    codes = new ArrayList<>();
                }
                codes.add(s.getName_f14() + "_" + s.getCode_f12());
                themeCodesNameMap.put(theme, codes);
            }
        }


        Map<String, BigDecimal> themeSpeedScoreMapDiff = new HashMap<>();
        //计算差值 与 pre 相比 , 增幅最大的 排序
        themeSpeedScoreMap.forEach((t, c) -> {
            BigDecimal pre = themeSpeedScoreMapPre.get(t);
            if (pre == null) {
                pre = BigDecimal.ZERO;
            }
            themeSpeedScoreMapDiff.put(t, c.subtract(pre));
        });

        Map<String, BigDecimal> sortMap = CollectionsUtil.sortByValue(themeSpeedScoreMapDiff, false);
        AtomicInteger count = new AtomicInteger(0);
        int limit = Math.min(10, sortMap.size());
        sortMap.forEach((t, c) -> {
            if (count.getAndIncrement() > limit) {
                return;
            }
            //string 占用8个字符
            String st = String.format("%8s", t);


            log.info("theme={} score={} {}", st, c, String.join(",\t", themeCodesNameMap.get(t)));
        });

        themeSpeedScoreMapPre = themeSpeedScoreMap;
    }

    public void check(EastSpeedInfo eastSpeedInfo) {
        String code = eastSpeedInfo.getCode_f12();
        if ("".equals(BLOCK_CODE_MAP.getIfPresent(code))) {
            log.debug("block code: {}", code + eastSpeedInfo.getName_f14());
            return;
        }

        //1, 判断当前30s 是否包含当日最高点
        //2, 判断是否为成交量最大
        //3, speed > 1
        if (eastSpeedInfo.getSpeed_f22().compareTo(BDUtil.B1) <= 0) {
            return;
        }
        if (eastSpeedInfo.getPct_f3().compareTo(BigDecimal.ZERO) <= 0
                || eastSpeedInfo.getPct_f3().compareTo(BDUtil.B2) > 0) {
            return;
        }
        if (eastSpeedInfo.getName_f14().contains("ST")) {
            return;
        }

        //获取成交额m1
        List<EmDailyK> dailyKs = emClient.getDailyKs(code, LocalDate.now(), 100, true);
        if (dailyKs.size() < 100) {
            log.info("k size {} < 100", dailyKs.size());
            BLOCK_CODE_MAP.put(code, "");
            return;
        }

        int last = dailyKs.size() - 1;
        EmDailyK lk1 = dailyKs.get(last);
        if (lk1.getPct().compareTo(BDUtil.BN1) < 0) {
            log.info("涨幅小于1%: {}", eastSpeedInfo.getName_f14());
            BLOCK_CODE_MAP.put(code, "");
            return;
        }

        EmDailyK lk = dailyKs.get(last);
        BigDecimal fAmt = BigDecimal.ZERO;
        if (AMT_M1_MAP.getIfPresent(code) != null) {
            fAmt = AMT_M1_MAP.getIfPresent(code);
        } else {
            BigDecimal dayAmtTop10 = emClient.amtTop10p(dailyKs);
            BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
            fAmt = hourAmt.multiply(BDUtil.b0_1);
            AMT_M1_MAP.put(code, fAmt);
        }

        if (fAmt != null && fAmt.compareTo(BDUtil.B5000W) < 0) {
            log.info("fenshi m1 定量 小于500w: {} {} ", eastSpeedInfo.getName_f14(), BDUtil.amtHuman(BDUtil.b0_1.multiply(fAmt)));
            BLOCK_CODE_MAP.put(code, "");
            return;
        }


        //判断 最近30s 是否包含当日最高点
        EastGetStockFenShiVo fEastGetStockFenShiVo = emRealTimeClient.getFenshiByCode(code);
        if (fEastGetStockFenShiVo == null) {
            log.info("fenshi is null: {}", eastSpeedInfo.getName_f14());
            BLOCK_CODE_MAP.put(code, "");
            return;
        }

        EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(fEastGetStockFenShiVo);
        if (trans == null) {
            log.info("trans is null: {}", eastSpeedInfo.getName_f14());
            BLOCK_CODE_MAP.put(code, "");
            return;
        }

        List<DetailTrans> DetailTransList = trans.getData();
        if (DetailTransList == null || DetailTransList.isEmpty()) {
            log.info("DetailTransList is null: {}", eastSpeedInfo.getName_f14());
            BLOCK_CODE_MAP.put(code, "");
            return;
        }

        //判断70s 内 是否大于 0.1 fAmt
        BigDecimal fenshiAmtLast70 = emRealTimeClient.getFenshiAmt(DetailTransList, 70);
        if (fenshiAmtLast70.compareTo(BDUtil.b0_1.multiply(fAmt)) < 0 ||
                fenshiAmtLast70.compareTo(BDUtil.B5000W) < 0) {
            log.info("70s内成交额{} < 定量 {} or 5000W: {}", BDUtil.amtHuman(fenshiAmtLast70), BDUtil.amtHuman(BDUtil.b0_1.multiply(fAmt)), eastSpeedInfo.getName_f14());
            return;
        }

        BigDecimal fenshi30sHigh = emRealTimeClient.getFenshiAmt(DetailTransList, 30);
        if (lk.getHigh().compareTo(fenshi30sHigh) > 0) {
            log.info("30s内达到最高价 high={} {} {}", lk.getHigh(), fenshi30sHigh, eastSpeedInfo.getName_f14());
            return;
        }

        // 比较开盘60s 内的成交额
        BigDecimal fenshiAmtOpenM1 = emRealTimeClient.getFenshiAmtOpenM1(DetailTransList);
        if (fenshiAmtLast70.compareTo(fenshiAmtOpenM1) < 0) {
            log.info("70s内成交额{} < 开盘60s内成交额{}: {}", BDUtil.amtHuman(fenshiAmtLast70), BDUtil.amtHuman(fenshiAmtOpenM1), eastSpeedInfo.getName_f14());
            return;
        }

        log.warn("分时新高 量价满足: {}", eastSpeedInfo.getName_f14());

        //发送微信 WX_SEND_MAP check < 3
        Integer count = WX_SEND_MAP.getIfPresent(code);
        if (count == null) {
            count = 0;
        }

        if (count > 2) {
            log.info("wx send count >= 3: {}", eastSpeedInfo.getName_f14());
            BLOCK_CODE_MAP.put(code, "");
            return;
        }


        if (code.startsWith("60") || code.startsWith("00") || code.startsWith("30")) {
            if (eastSpeedInfo.getPrice_f2().compareTo(BDUtil.B50) > 0) {
                log.info("price > 50: {}, stop", eastSpeedInfo.getName_f14());
                return;
            }
        } else {
            if (eastSpeedInfo.getPrice_f2().multiply(BDUtil.B2).compareTo(BDUtil.B50) > 0) {
                log.info("price x2 > 50: {}, stop", eastSpeedInfo.getName_f14());
                return;
            }
        }


        String url = EmClient.getEastUrl(code);
        String content = "[speed_up]" + eastSpeedInfo.getName_f14() + code +
                "<br>" + "涨速: " + eastSpeedInfo.getSpeed_f22() +
                "<br>" + "涨幅: " + eastSpeedInfo.getPct_f3() +
                "<br>" + "成交额: " + BDUtil.amtHuman(lk.getAmt()) +
                "<br>" + "fAmt: " + BDUtil.amtHuman(BDUtil.b0_1.multiply(fAmt).setScale(2, RoundingMode.HALF_UP)) + ",m1: " + BDUtil.amtHuman(fenshiAmtLast70) + ",open: " + BDUtil.amtHuman(fenshiAmtOpenM1) +
                "<br>" + url;

        String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
        wxUtil.send(encode);
        WX_SEND_MAP.put(code, count + 1);

    }


    public void check(EastSpeedInfo eastSpeedInfo, List<DetailTrans> DetailTransList, List<EmDailyK> dailyKs) {
        String code = eastSpeedInfo.getCode_f12();
        if ("".equals(BLOCK_CODE_MAP.getIfPresent(code))) {
            log.debug("block code: {}", code + eastSpeedInfo.getName_f14());
            return;
        }

        //1, 判断当前30s 是否包含当日最高点
        //2, 判断是否为成交量最大
        //3, speed > 1
        if (eastSpeedInfo.getSpeed_f22().compareTo(BDUtil.B1) <= 0) {
            return;
        }
        if (eastSpeedInfo.getPct_f3().compareTo(BigDecimal.ZERO) <= 0
                || eastSpeedInfo.getPct_f3().compareTo(BDUtil.B2) > 0) {
            return;
        }
        if (eastSpeedInfo.getName_f14().contains("ST")) {
            return;
        }

        //获取成交额m1
        if (dailyKs.size() < 100) {
            log.info("k size {} < 100", dailyKs.size());
            BLOCK_CODE_MAP.put(code, "");
            return;
        }

        int last = dailyKs.size() - 1;
        EmDailyK lk = dailyKs.get(last);
        BigDecimal fAmt = BigDecimal.ZERO;
        if (AMT_M1_MAP.getIfPresent(code) != null) {
            fAmt = AMT_M1_MAP.getIfPresent(code);
        } else {
            BigDecimal dayAmtTop10 = emClient.amtTop10p(dailyKs);
            BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
            fAmt = hourAmt.multiply(BDUtil.b0_1);
            AMT_M1_MAP.put(code, fAmt);
        }

        if (fAmt != null && fAmt.compareTo(BDUtil.B5000W) < 0) {
            log.info("fenshi m1 定量 小于500w: {} {} ", eastSpeedInfo.getName_f14(), BDUtil.amtHuman(BDUtil.b0_1.multiply(fAmt)));
            BLOCK_CODE_MAP.put(code, "");
            return;
        }


        if (DetailTransList == null || DetailTransList.isEmpty()) {
            log.info("DetailTransList is null: {}", eastSpeedInfo.getName_f14());
            BLOCK_CODE_MAP.put(code, "");
            return;
        }

        //判断70s 内 是否大于 0.1 fAmt
        BigDecimal fenshiAmtLast70 = emRealTimeClient.getFenshiAmt(DetailTransList, 70);
        if (fenshiAmtLast70.compareTo(BDUtil.b0_1.multiply(fAmt)) < 0 ||
                fenshiAmtLast70.compareTo(BDUtil.B5000W) < 0) {
            log.info("70s内成交额{} < 定量 {} or 5000W: {}", BDUtil.amtHuman(fenshiAmtLast70), BDUtil.amtHuman(BDUtil.b0_1.multiply(fAmt)), eastSpeedInfo.getName_f14());
            return;
        }

        BigDecimal fenshi30sHigh = emRealTimeClient.getFenshiAmt(DetailTransList, 30);
        if (lk.getHigh().compareTo(fenshi30sHigh) > 0) {
            log.info("30s内达到最高价 high={} {} {}", lk.getHigh(), fenshi30sHigh, eastSpeedInfo.getName_f14());
            return;
        }

        // 比较开盘60s 内的成交额
        BigDecimal fenshiAmtOpenM1 = emRealTimeClient.getFenshiAmtOpenM1(DetailTransList);
        if (fenshiAmtLast70.compareTo(fenshiAmtOpenM1) < 0) {
            log.info("70s内成交额{} < 开盘60s内成交额{}: {}", BDUtil.amtHuman(fenshiAmtLast70), BDUtil.amtHuman(fenshiAmtOpenM1), eastSpeedInfo.getName_f14());
            return;
        }

        log.warn("分时新高 量价满足: {}", eastSpeedInfo.getName_f14());

        //发送微信 WX_SEND_MAP check < 3
        Integer count = WX_SEND_MAP.getIfPresent(code);
        if (count == null) {
            count = 0;
        }

        if (count > 2) {
            log.info("wx send count >= 3: {}", eastSpeedInfo.getName_f14());
            BLOCK_CODE_MAP.put(code, "");
            return;
        }


        String url = EmClient.getEastUrl(code);
        String content = "[speed_up]" + eastSpeedInfo.getName_f14() + code +
                "<br>" + "涨速: " + eastSpeedInfo.getSpeed_f22() +
                "<br>" + "涨幅: " + eastSpeedInfo.getPct_f3() +
                "<br>" + "成交额: " + BDUtil.amtHuman(lk.getAmt()) +
                "<br>" + "fAmt: " + BDUtil.amtHuman(BDUtil.b0_1.multiply(fAmt).setScale(2, RoundingMode.HALF_UP))
                + ",m1: <bold>" + BDUtil.amtHuman(fenshiAmtLast70) + "</bold>,open: " + BDUtil.amtHuman(fenshiAmtOpenM1) +
                "<br>" + url;

        String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
        wxUtil.send(encode);
        WX_SEND_MAP.put(code, count + 1);

    }

    /**
     * 获取全部均线
     * 每个周1 - 周5 30:08:00 执行
     */
    public static final Set<String> CODE_MA_BLOCK_SET = new HashSet<>();

    @Scheduled(cron = "0 15 08 ? * MON-FRI")
    public void initMaMap() {
        CODE_MA_BLOCK_SET.clear();
        CODE_KS_CACHE_COUNT.set(0);
    }

    //9:30 - 11:30 , 13:00 - 15:00
    @Scheduled(cron = "0/15 0/1 * * * ?")
    public void runCrossMa() {

        if (!EmClient.tradeTime()) {
            return;
        }

        List<EmCList> list = emClient.getClistDefaultSize(true);
        List<EmCList> lowList = list.stream().filter(
                em -> em.getF17Open().compareTo(em.getF2Close()) < 0
                        && em.getF17Open().compareTo(em.getF16Low()) == 0
                        && em.getF17Open().compareTo(BDUtil.B5) > 0
                        && em.getF3Pct().compareTo(BigDecimal.ZERO) > 0
        ).toList();


        for (EmCList emCList : lowList) {
            log.info("initMaMap: {}", emCList.getF12Code());
            String code = emCList.getF12Code();
            if (CODE_MA_BLOCK_SET.contains(code)) continue;

            List<EmDailyK> dailyKs = getKsCache(code);
            if (dailyKs == null || dailyKs.size() < 65) {
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


            String content = "[crossMa]" + emCList.getF14Name() + emCList.getF12Code() + "_" + emCList.getF3Pct();
            String url = EmClient.getEastUrl(emCList.getF12Code());
            content += "<br>" + url;

            String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
            wxUtil.send(encode);
            CODE_MA_BLOCK_SET.add(code);
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

    public static final Map<String, List<EmDailyK>> CODE_KS_CACHE_MAP = new HashMap<>();
    public static final AtomicInteger CODE_KS_CACHE_COUNT = new AtomicInteger(0);

}

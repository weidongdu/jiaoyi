package pro.jiaoyi.eastm.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.CollectionsUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.config.VipIndexEnum;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.dao.entity.OpenEmCListEntity;
import pro.jiaoyi.eastm.dao.repo.OpenEmCListRepo;
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
        THEME_MAP.clear();
        open("");
    }

    /**
     * 穿越均线
     * 1, ma5 之上
     * 2, 穿越碰个均线
     */
    public void crossMa() {
        List<String> codes = emClient.crossMa();
        if (codes == null || codes.size() == 0) {
            return;
        }
        for (String code : codes) {
            log.info("穿越均线: {}", code);
        }
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
                    log.info("成交额小于1000万: {}", em.getF14Name());
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
        List<String> list = THEME_MAP.get(code);
        if (list == null) {
            List<String> themeList = emClient.coreThemeDetail(code);
            if (themeList.size() > 0) {
                THEME_MAP.put(code, themeList);
            }
        }

        return THEME_MAP.get(code);
    }

    public static final Map<String, List<String>> THEME_MAP = new ConcurrentHashMap<>();

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
            csv.append(emCList.getF12Code() + emCList.getF14Name()).append(",");
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


    @Scheduled(fixedRate = 5000L)
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


        List<EastSpeedInfo> speedTop = emRealTimeClient.getSpeedTop(50, false);
        if (speedTop == null || speedTop.size() == 0) {
            return;
        }

        for (EastSpeedInfo eastSpeedInfo : speedTop) {
            String code = eastSpeedInfo.getCode_f12();
            if ("".equals(BLOCK_CODE_MAP.getIfPresent(code))) {
                log.debug("block code: {}", code + eastSpeedInfo.getName_f14());
                continue;
            }

            //1, 判断当前30s 是否包含当日最高点
            //2, 判断是否为成交量最大
            //3, speed > 1
            if (eastSpeedInfo.getSpeed_f22().compareTo(BDUtil.B1) <= 0) {
                continue;
            }
            if (eastSpeedInfo.getPct_f3().compareTo(BigDecimal.ZERO) <= 0
                    || eastSpeedInfo.getPct_f3().compareTo(BDUtil.B2) > 0) {
                continue;
            }
            if (eastSpeedInfo.getName_f14().contains("ST")) {
                continue;
            }

            //获取成交额m1
            List<EmDailyK> dailyKs = emClient.getDailyKs(code, LocalDate.now(), 100, true);
            if (dailyKs.size() < 100) {
                log.info("k size {} < 100", dailyKs.size());
                BLOCK_CODE_MAP.put(code, "");
                continue;
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
                continue;
            }


            //判断 最近30s 是否包含当日最高点
            EastGetStockFenShiVo fEastGetStockFenShiVo = emRealTimeClient.getFenshiByCode(code);
            if (fEastGetStockFenShiVo == null) {
                log.info("fenshi is null: {}", eastSpeedInfo.getName_f14());
                BLOCK_CODE_MAP.put(code, "");
                continue;
            }

            EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(fEastGetStockFenShiVo);
            if (trans == null) {
                log.info("trans is null: {}", eastSpeedInfo.getName_f14());
                BLOCK_CODE_MAP.put(code, "");
                continue;
            }

            List<DetailTrans> DetailTransList = trans.getData();
            if (DetailTransList == null || DetailTransList.isEmpty()) {
                log.info("DetailTransList is null: {}", eastSpeedInfo.getName_f14());
                BLOCK_CODE_MAP.put(code, "");
                continue;
            }

            //判断70s 内 是否大于 0.1 fAmt
            BigDecimal fenshiAmtLast70 = emRealTimeClient.getFenshiAmt(DetailTransList, 70);
            if (fenshiAmtLast70.compareTo(BDUtil.b0_1.multiply(fAmt)) < 0 ||
                    fenshiAmtLast70.compareTo(BDUtil.B5000W) < 0) {
                log.info("70s内成交额{} < 定量 {} or 5000W: {}", BDUtil.amtHuman(fenshiAmtLast70), BDUtil.amtHuman(BDUtil.b0_1.multiply(fAmt)), eastSpeedInfo.getName_f14());
                continue;
            }

            BigDecimal fenshi30sHigh = emRealTimeClient.getFenshiAmt(DetailTransList, 30);
            if (lk.getHigh().compareTo(fenshi30sHigh) > 0) {
                log.info("30s内达到最高价 high={} {} {}", lk.getHigh(), fenshi30sHigh, eastSpeedInfo.getName_f14());
                continue;
            }

            // 比较开盘60s 内的成交额
            BigDecimal fenshiAmtOpenM1 = emRealTimeClient.getFenshiAmtOpenM1(DetailTransList);
            if (fenshiAmtLast70.compareTo(fenshiAmtOpenM1) < 0) {
                log.info("70s内成交额{} < 开盘60s内成交额{}: {}", BDUtil.amtHuman(fenshiAmtLast70), BDUtil.amtHuman(fenshiAmtOpenM1), eastSpeedInfo.getName_f14());
                continue;
            }

            log.info("分时新高 量价满足: {}", eastSpeedInfo.getName_f14());

            //发送微信 WX_SEND_MAP check < 3
            Integer count = WX_SEND_MAP.getIfPresent(code);
            if (count == null) {
                count = 0;
            }

            if (count > 2) {
                log.info("wx send count >= 3: {}", eastSpeedInfo.getName_f14());
                BLOCK_CODE_MAP.put(code, "");
                continue;
            }

            String symbol = "";
            if (code.startsWith("6")) {
                symbol = "sh" + code;
            } else if (code.startsWith("0") || code.startsWith("3")) {
                symbol = "sz" + code;
            } else {
                symbol = "bj/" + code;

            }
            String url = "https://quote.eastmoney.com/" + symbol + ".html";
            String content = "[speed_up]" + eastSpeedInfo.getName_f14() + code +
                    "<br>" + "涨速: " + eastSpeedInfo.getSpeed_f22() +
                    "<br>" + "涨幅: " + eastSpeedInfo.getPct_f3() +
                    "<br>" + "成交额: " + BDUtil.amtHuman(lk.getAmt()) +
                    "<br>" + "fAmt: " + BDUtil.amtHuman(BDUtil.b0_1.multiply(fAmt).setScale(2, RoundingMode.HALF_UP)) + ",m1: " + BDUtil.amtHuman(fenshiAmtLast70) + ",open: " + BDUtil.amtHuman(fenshiAmtOpenM1) +
                    "<br>" + "ts: " + LocalDateTime.now() +
                    "<br>" + url;

            String encode = URLEncoder.encode(content, StandardCharsets.UTF_8);
            wxUtil.send(encode);
            WX_SEND_MAP.put(code, count + 1);

        }
    }

}

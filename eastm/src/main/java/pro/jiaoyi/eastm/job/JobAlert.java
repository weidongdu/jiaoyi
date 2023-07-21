package pro.jiaoyi.eastm.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.EmojiUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.config.IndexEnum;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.model.EastSpeedInfo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.model.fenshi.DetailTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiVo;
import pro.jiaoyi.eastm.util.EmMaUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static pro.jiaoyi.eastm.api.EmClient.*;

//@Component
@Slf4j
public class JobAlert {
    //监控 放量有涨速


    public static String TIP = "";


    static {
        TIP += "<br>" + EmojiUtil.DOWN + "卖出: 成本价附近,昨日低点";
        TIP += "<br>" + EmojiUtil.DOWN + "卖出: 小幅整理期,震荡底部";
        TIP += "<br>" + EmojiUtil.DOWN + "卖出: 趋势未结束,底仓持有";
        TIP += "<br>" + EmojiUtil.DOWN + "卖出: 交易有盈利,浮动止盈";
        TIP += "<br>" + EmojiUtil.UP + "买入: 计划外的票,谨慎开仓";
        TIP += "<br>" + EmojiUtil.UP + "买入: 高开再新高,量同步新高";
        TIP += "<br>" + EmojiUtil.UP + "买入: 仓位要控制,不要贪多";
    }

    @Autowired
    private EmRealTimeClient emRealTimeClient;
    @Autowired
    private EmClient emClient;

    @Autowired
    private WxUtil wxUtil;

    public static final BigDecimal B_Y = new BigDecimal("100000000");
    public static final BigDecimal B_W = new BigDecimal("10000");

    public static final Map<String, Integer> DAY_COUNT_MAP = new HashMap<>();
    public static final String AM = "AM";
    public static final String PM = "PM";

    public static final Map<LocalDate, List<String>> DAY_BLOCKLIST_MAP = new HashMap<>();
    public static final Set<String> INDEXSET = new HashSet<>();

    //cron 每天上午8点 清空
    @Scheduled(cron = "0 0 9 * * ?")
    public void clear() {
        DATE_CODE_NAME_MAP.clear();
        DATE_NAME_CODE_MAP.clear();
        DATE_STOCK_CODE_BK_MAP.clear();
        BK_MAP.clear();
        DATE_INDEX_ALL_MAP.clear();
        DATE_KLINE_MAP.clear();
        DAY_BLOCKLIST_MAP.clear();
        updateIndex();
    }

    public void updateIndex() {
        INDEXSET.clear();
        INDEXSET.addAll(emClient.getIndex(IndexEnum.HS300.getUrl()).stream().map(EmCList::getF12Code).collect(Collectors.toSet()));
        INDEXSET.addAll(emClient.getIndex(IndexEnum.CYCF.getUrl()).stream().map(EmCList::getF12Code).collect(Collectors.toSet()));
        INDEXSET.addAll(emClient.getIndex(IndexEnum.ZZ500.getUrl()).stream().map(EmCList::getF12Code).collect(Collectors.toSet()));
        INDEXSET.addAll(emClient.getIndex1000().stream().map(EmCList::getF12Code).collect(Collectors.toSet()));

    }

    @Scheduled(fixedRate = 1000 * 10L)
    public void run() {
        if (!EmRealTimeClient.tradeTime()) return;

        if (INDEXSET.isEmpty()) {
            updateIndex();
        }

        List<EmCList> all = emClient.getClistDefaultSize(false);
        List<EmCList> openHighList = all.stream().filter(em -> {
            if (!INDEXSET.contains(em.getF12Code())) {
                return false;
            }

            BigDecimal pre = em.getF18Close();
            BigDecimal open = em.getF17Open();
            if (pre.compareTo(BigDecimal.ZERO) == 0) {
                return false;
            }

            BigDecimal openPct = open.divide(pre, 4, RoundingMode.HALF_UP);
            //高开 0.5% - 3%
            if (openPct.compareTo(new BigDecimal("1.005")) > 0
                    && openPct.compareTo(new BigDecimal("1.03")) < 0) {
                return true;
            }
            // 高开 0 - 0.5% 且开盘价等于最低价
            return openPct.compareTo(new BigDecimal("1.005")) <= 0
                    && openPct.compareTo(BigDecimal.ONE) >= 0
                    && open.compareTo(em.getF16Low()) == 0;

        }).toList();
        Set<String> openHighCodeSet = openHighList.stream().map(EmCList::getF12Code).collect(Collectors.toSet());


        Integer am = DAY_COUNT_MAP.get(LocalDate.now() + AM);
        if (am == null) {
            String content = "监控启动" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateUtil.PATTERN_yyyy_MM_dd + "_" + DateUtil.PATTERN_HH_mm_ss));
            content += TIP;
            wxUtil.send(content);
            DAY_COUNT_MAP.put(LocalDate.now() + AM, 1);
        }


        List<EastSpeedInfo> tops = emRealTimeClient.getSpeedTop(50);
        log.info("speed list {}", tops.size());
        if (tops.size() > 0) {
            for (EastSpeedInfo top : tops) {
                String code = top.getCode_f12();
                String name = top.getName_f14();
                if (!INDEXSET.contains(code)) {
                    log.info("not index code {}", code + name);
                    continue;
                }

                if (code.startsWith("8")) {
                    log.info("北交所排除 {}", code + name);
                    continue;
                }

                //过滤 假设涨停也无法满足条件
                List<String> blockList = DAY_BLOCKLIST_MAP.computeIfAbsent(LocalDate.now(), k -> new ArrayList<>());
                if (blockList.contains(code)) {
                    log.info("block code {}", code + name);
                    continue;
                }

                log.info("run speed {} {} {}", code, name, top.getSpeed_f22());
                List<EmDailyK> dailyKs = emClient.getDailyKs(code, LocalDate.now(), 300, true);
                if (dailyKs.size() < 260) {
                    log.info("k size {} < 250", dailyKs.size());
                    blockList.add(code);
                    continue;
                }

                BigDecimal dayAmtTop10 = emClient.amtTop10p(dailyKs);
                BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
                if (hourAmt.compareTo(BDUtil.B5000W) < 0) {
                    log.info("日成交额 {} 不满足条件(约成交额 ma60<1亿)", amtStr(dayAmtTop10));
                    blockList.add(code);
                    continue;
                }

                EmDailyK k = dailyKs.get(dailyKs.size() - 1);
                Map<String, BigDecimal[]> ma = EmMaUtil.ma(dailyKs);

                BigDecimal[] ma5 = ma.get("ma5");
                BigDecimal[] ma10 = ma.get("ma10");
                BigDecimal[] ma20 = ma.get("ma20");
                BigDecimal[] ma30 = ma.get("ma30");
                BigDecimal[] ma60 = ma.get("ma60");
                BigDecimal[] ma120 = ma.get("ma120");
                BigDecimal[] ma250 = ma.get("ma250");

                int last = dailyKs.size() - 1;
                BigDecimal ma5_value = ma5[last];
                BigDecimal ma10_value = ma10[last];
                BigDecimal ma20_value = ma20[last];
                BigDecimal ma30_value = ma30[last];
                BigDecimal ma60_value = ma60[last];
                BigDecimal ma120_value = ma120[last];
                BigDecimal ma250_value = ma250[last];

                if (k.getClose().compareTo(ma5_value) < 0
                        || k.getClose().compareTo(ma10_value) < 0
                        || k.getClose().compareTo(ma20_value) < 0
                        || k.getClose().compareTo(ma30_value) < 0
                        || k.getClose().compareTo(ma60_value) < 0
                        || ma5_value.compareTo(ma250_value) < 0
                ) {
                    log.info("不满足均线之上");
                    if (ma5[last - 1].compareTo(ma250[last - 1]) < 0){
                        blockList.add(code);
                    }
                    continue;
                }

                BigDecimal fAmt = new BigDecimal("0.1").multiply(hourAmt);

                EastGetStockFenShiVo fEastGetStockFenShiVo = emRealTimeClient.getFenshiByCode(code);
                if (fEastGetStockFenShiVo == null) continue;
                EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(fEastGetStockFenShiVo);
                if (trans == null) continue;
                List<DetailTrans> DetailTransList = trans.getData();
                if (DetailTransList == null || DetailTransList.isEmpty()) continue;

                //判断70s 内 是否大于 0.1 fAmt
                BigDecimal fenshiAmtLast70 = emRealTimeClient.getFenshiAmt(DetailTransList, 70);
                //成交量放大倍数
                BigDecimal fx = fenshiAmtLast70.divide(hourAmt, 4, RoundingMode.HALF_UP);

                String amtStr = amtStr(fAmt);
                String fenshiAmtStr = amtStr(fenshiAmtLast70);

                if (fx.compareTo(new BigDecimal("0.1")) < 0) {
                    log.info("分时成交量{} 不满足条件{}", fenshiAmtStr, amtStr);
                    continue;
                }


                if (openHighCodeSet.contains(code)) {
                    log.info("开盘高开 {}", code);
                    //当前家是否为最高价
                    LocalDateTime openTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 30));
                    List<DetailTrans> open60s = DetailTransList.stream()
                            .filter(d -> d.getTs() >= DateUtil.toTimestamp(openTime)
                                    && d.getTs() <= DateUtil.toTimestamp(openTime.plusSeconds(60)))
                            .toList();

                    BigDecimal open60sAmt = BigDecimal.ZERO;
                    for (DetailTrans open60 : open60s) {
                        open60sAmt = open60sAmt.add(open60.amt());
                    }

                    if (open60sAmt.compareTo(fenshiAmtLast70) < 0) {
                        //最近量满足
                        log.info("最近amt > open 1m");
                        //判断 价格是否最高
                        List<BigDecimal> priceList = DetailTransList.stream()
                                .filter(detailTrans ->
                                        detailTrans.getTs() >= DateUtil.toTimestamp(openTime))
                                .map(DetailTrans::getPrice).toList();
                        BigDecimal max = priceList.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

                        for (int i = 0; i < 5; i++) {
                            //最后5个价格 有就一个是最高价
                            if (priceList.get(priceList.size() - 1 - i).compareTo(max) == 0) {
                                log.info("满足分时 二次突破 最高价 {}", max);
                                String content = "高开分时突破" + code + "_" + name + "_" + k.getBk()
                                        + "<br>" + "价格=" + k.getClose() + ",涨幅=" + k.getPct()
                                        + "<br>" + "标准量=" + amtStr + ",M1=" + fx + "_" + fenshiAmtStr
                                        + "<br>" + LocalDateTime.now().toString().substring(0, 16);
                                log.info("价格突破成功 {}", content.replaceAll("<br>", "\n"));
                                boolean push = checkSendWxCount(code);
                                if (push) {
                                    wxUtil.send(content);
                                    blockList.add(code);
                                }
                                break;
                            }
                        }
                    }
                }


//                int tu = emRealTimeClient.tu(dailyKs, 60, 60, 0.4d);
                boolean tu = emRealTimeClient.tu_old(dailyKs, 60, 60, 0.4d);
                if (tu) {

                    log.info("run {} {}", code, name);
                    if (k.getClose().compareTo(ma120_value) < 0
                            || k.getClose().compareTo(ma250_value) < 0) {
                        log.info("不满足均线之上");
                        blockList.add(code);
                        continue;
                    }

                    String content = code + "_" + name + "_" + k.getBk()
                            + "<br>" + "价格=" + k.getClose() + ",涨幅=" + k.getPct()
                            + "<br>" + "标准量=" + amtStr + ",M1=" + fx + "_" + fenshiAmtStr
                            + "<br>" + LocalDateTime.now().toString().substring(0, 16);
                    log.info("价格突破成功 {}", content.replaceAll("<br>", "\n"));
                    boolean push = checkSendWxCount(code);
                    if (push) {
                        wxUtil.send(content);
                        blockList.add(code);
                    }

                } else {
                    //设置 最高价, 如果还不满足 , 加入block list
//                    EmDailyK k = dailyKs.get(dailyKs.size() - 1);
                    BigDecimal f = new BigDecimal("1.2");
                    if (k.getCode().startsWith("60") || k.getCode().startsWith("0")) {
                        f = new BigDecimal("1.1");
                    }
                    k.setHigh(f.multiply(k.getPreClose()).setScale(2, RoundingMode.HALF_UP));
//                    int tu2 = emRealTimeClient.tu(dailyKs, 60, 60, 0.4d);
                    boolean tu2 = emRealTimeClient.tu_old(dailyKs, 60, 60, 0.4d);
                    if (tu2) {
                        log.info("涨停价还不满足,加入block list,{}", k);
                        blockList.add(code);
                    }
                }

            }
        }

    }

    public static final Map<String, Integer> SEND_WX_MAP = new ConcurrentHashMap<String, Integer>();

    public boolean checkSendWxCount(String code) {
        String key = DateUtil.today() + "-" + code;
        Integer count = SEND_WX_MAP.get(key);
        if (count == null) {
            count = 0;
        }
        if (count > 2) {
            return false;
        }

        if (SEND_WX_MAP.size() > 1000) {
            //清空 非today 开头在key
            SEND_WX_MAP.entrySet().removeIf(entry -> !entry.getKey().startsWith(DateUtil.today()));
        }

        SEND_WX_MAP.put(key, count + 1);
        return true;
    }


    public String amtStr(BigDecimal fAmt) {
        String str = "";

        if (fAmt.compareTo(B_Y) > 0) {
            str = fAmt.divide(B_Y, 2, RoundingMode.HALF_UP) + "亿";
        } else {
            str = fAmt.divide(B_W, 0, RoundingMode.HALF_UP) + "万";
        }

        return str;
    }

    public static void main(String[] args) {
        HashSet<String> set = new HashSet<>();
        set.add("apple");
        set.add("banana");
        set.add("orange");

        // 打乱 HashSet 中元素的顺序
        ArrayList<String> list = new ArrayList<>(set);
        Collections.shuffle(list);

        // 遍历 HashSet
        for (String element : list) {
            System.out.println(element);
        }

        System.out.println();


        for (int i = 0; i < 5; i++) {

            // 打乱 HashSet 中元素的顺序
            Collections.shuffle(list);

            // 遍历 HashSet
            for (String element : list) {
                System.out.println(element);
            }

            System.out.println();
        }

    }

}

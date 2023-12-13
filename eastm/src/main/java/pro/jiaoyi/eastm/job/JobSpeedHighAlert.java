package pro.jiaoyi.eastm.job;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.EmojiUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.dao.entity.FenshiSimpleEntity;
import pro.jiaoyi.eastm.dao.repo.FenshiSimpleRepo;
import pro.jiaoyi.eastm.model.EastSpeedInfo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.model.fenshi.DetailTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiVo;
import pro.jiaoyi.eastm.util.EmMaUtil;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
public class JobSpeedHighAlert {
    //监控 放量有涨速


    public static String TIP = "";


    static {
        TIP += "<br>" + EmojiUtil.DOWN + "卖出: 成本价附近,昨日低点";
        TIP += "<br>" + EmojiUtil.DOWN + "卖出: 小幅整理期,震荡底部";
        TIP += "<br>" + EmojiUtil.DOWN + "卖出: 趋势未结束,底仓持有";
        TIP += "<br>" + EmojiUtil.DOWN + "卖出: 交易有盈利,浮动止盈";
        TIP += "<br>" + EmojiUtil.UP + "买入: 计划外的,谨慎开仓";
        TIP += "<br>" + EmojiUtil.UP + "买入: 高开新高,量也要高";
        TIP += "<br>" + EmojiUtil.UP + "买入: 仓位控制,不要贪多";
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


    @Scheduled(fixedRate = 1 * 60 * 1000L)
    @Async
    public void updateIndex() {
//        INDEXSET.clear();
//        INDEXSET.addAll(emClient.getIndex(IndexEnum.HS300.getUrl()).stream().map(EmCList::getF12Code).collect(Collectors.toSet()));
//        INDEXSET.addAll(emClient.getIndex(IndexEnum.CYCF.getUrl()).stream().map(EmCList::getF12Code).collect(Collectors.toSet()));
//        INDEXSET.addAll(emClient.getIndex(IndexEnum.ZZ500.getUrl()).stream().map(EmCList::getF12Code).collect(Collectors.toSet()));
//        INDEXSET.addAll(emClient.getIndex1000().stream().map(EmCList::getF12Code).collect(Collectors.toSet()));

        if (!EmRealTimeClient.tradeTime()) {
            return;
        }
        List<EmCList> emCLists = emClient.xuanguCList();
        log.info("xuanguCList size {}", emCLists.size());
        INDEXSET.clear();
        INDEXSET.addAll(emCLists.stream().map(EmCList::getF12Code).collect(Collectors.toSet()));
    }


    @Scheduled(fixedRate = 1000 * 10L)
    public void run() {
        if (!EmRealTimeClient.tradeTime()) {
            return;
        }

        if (INDEXSET.isEmpty()) {
            updateIndex();
        }

        if (INDEXSET.isEmpty()) {
            return;
        }


        Integer am = DAY_COUNT_MAP.get(LocalDate.now() + AM);
        if (am == null) {
            String content = "监控启动" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateUtil.PATTERN_yyyy_MM_dd + "_" + DateUtil.PATTERN_HH_mm_ss));
            content += TIP;
            wxUtil.send(content);
            DAY_COUNT_MAP.put(LocalDate.now() + AM, 1);
        }

        //601088

        List<EastSpeedInfo> tops = emRealTimeClient.getSpeedTop(50);
        log.info("speed list {}", tops.size());
        if (tops.size() > 0) {
            List<EastSpeedInfo> list = tops.stream().filter(t -> INDEXSET.contains(t.getCode_f12())).toList();
            if (list.size() > 0) {
                tops = list;
            } else {
                return;
            }

            //过滤条件:
            //hls ma60 > 1
            //hls 30k < 10

            for (EastSpeedInfo top : tops) {
                String code = top.getCode_f12();
                String name = top.getName_f14();

                //过滤 假设涨停也无法满足条件
                List<String> blockList = DAY_BLOCKLIST_MAP.computeIfAbsent(LocalDate.now(), k -> new ArrayList<>());
                if (blockList.contains(code)) {
                    log.info("block code {}", code + name);
                    continue;
                }

                log.info("run speed {} {} {}", code, name, top.getSpeed_f22());
                List<EmDailyK> dailyKs = emClient.getDailyKs(code, LocalDate.now(), 300, true);


                if (dailyKs.size() < 260) {
                    log.info("k size {} < 260 block code", dailyKs.size());
                    blockList.add(code);
                    continue;
                }


                int last = dailyKs.size() - 1;
                BigDecimal stopF = code.startsWith("688") || code.startsWith("300") ? BDUtil.B1_2 : BDUtil.B1_1;

                BigDecimal highStop = dailyKs.get(last).getPreClose().multiply(stopF).setScale(3, RoundingMode.HALF_UP);
                //涨停价创90k新高
                boolean highStop90 = true;
                for (int i = last - 90; i < last; i++) {
                    if (dailyKs.get(i).getHigh().compareTo(highStop) > 0) {
                        log.info("涨停价不满足创90k新高 {} {} {} 涨停价={}", code, name, dailyKs.get(i).getHigh(), highStop);
                        blockList.add(code);
                        highStop90 = false;
                        break;
                    }
                }

                if (!highStop90) {
                    continue;
                }


                //hls ma60 > 1
                BigDecimal[] hsArr = dailyKs.stream().map(EmDailyK::getHsl).toList().toArray(new BigDecimal[0]);
                BigDecimal[] hsMa60 = MaUtil.ma(60, hsArr, 4);
                if (hsMa60[hsMa60.length - 1 - 1].compareTo(BDUtil.B1) < 0) {
                    log.warn("日换手率 index={} {} 不满足条件(>1)", hsMa60.length - 1, hsMa60[hsMa60.length - 1]);
                    blockList.add(code);
                    continue;
                }

                //hls 30k 之内 < 10
                boolean hls30k = true;
                for (int i = hsArr.length - 1 - 30; i < hsArr.length - 1; i++) {
                    if (hsArr[i].compareTo(BDUtil.B10) > 0) {
                        log.warn("日换手率 {} 不满足条件(<10)", hsArr[i]);
                        blockList.add(code);
                        hls30k = false;
                        break;
                    }
                }
                if (!hls30k) {
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

                boolean maAbove = false;
                boolean maUp = false;

                maAbove = k.getClose().compareTo(ma5[last]) > 0
                        && k.getClose().compareTo(ma10[last]) > 0
                        && k.getClose().compareTo(ma20[last]) > 0
                        && k.getClose().compareTo(ma30[last]) > 0
                        && k.getClose().compareTo(ma60[last]) > 0
                        && k.getClose().compareTo(ma120[last]) > 0
                        && k.getClose().compareTo(ma250[last]) > 0;

                maUp = ma5[last - 1].compareTo(ma5[last]) < 0 &&
                        ma10[last - 1].compareTo(ma10[last]) < 0 &&
                        ma20[last - 1].compareTo(ma20[last]) < 0 &&
                        ma30[last - 1].compareTo(ma30[last]) < 0 &&
                        ma60[last - 1].compareTo(ma60[last]) < 0 &&
                        ma120[last - 1].compareTo(ma120[last]) < 0 &&
                        ma250[last - 1].compareTo(ma250[last]) < 0;


                if (!(maAbove && maUp)) {
                    log.info("不满足均线之上, 均线向上 {}", maAbove + "_" + maUp);
                    blockList.add(code);
                    continue;
                }

                BigDecimal fAmt = BDUtil.b0_1.multiply(hourAmt);

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
                            StringBuilder content = new StringBuilder(code + "_" + name + "_" + k.getBk()
                                    + "<br>" + "价格=" + k.getClose() + ",涨幅=" + k.getPct()
                                    + "<br>" + "标准量=" + amtStr + ",M1=" + fx + "_" + fenshiAmtStr
                                    + "<br>" + LocalDateTime.now().toString().substring(0, 16));
                            log.info("价格突破成功 {}", content.toString());

                            List<String> guba = emClient.guba(code);
                            if (guba.size() > 0) {
                                for (String s : guba) {
                                    content.append(s);
                                }
                            }

                            boolean push = checkSendWxCount(code);
                            if (push) {
                                wxUtil.send(content.toString());
                                blockList.add(code);
                            }

                            break;
                        }
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


    @Resource
    private FenshiSimpleRepo fenshiSimpleRepo;

    //cron 工作日 上午 9:26分
    @Scheduled(cron = "30 25 9 * * ?")
//    @Scheduled(fixedRate = 1000)
    public void runOpen() {

        if (LocalDate.now().getDayOfWeek().getValue() == 6
                || LocalDate.now().getDayOfWeek().getValue() == 7) {
            return;
        }

        List<EmCList> list = emClient.getClistDefaultSize(true);

        List<EmCList> fList = list.stream().filter(
                e -> {
                    boolean a = e.getF6Amt() != null
                            && e.getF6Amt().compareTo(BDUtil.B5000W) > 0
                            && e.getF3Pct().compareTo(BDUtil.B1) <= 0
                            && e.getF3Pct().compareTo(BDUtil.BN1) >= 0;
                    return a;
//                    if (a) {
//                        //判断昨天集合竞价
//                        FenshiSimpleEntity db = fenshiSimpleRepo.findByCodeAndTradeDate(e.getF12Code(), LocalDate.now().minusDays(1).toString().replaceAll("-", ""));
//                        if (db != null
//                                && (db.getOpenAmt() == null || e.getF6Amt().compareTo(db.getOpenAmt()) > 0)) {
//                            return true;
//                        }
//
//                    }
//                    return false;

                }
        ).toList();

        if (fList.isEmpty()) {
            return;
        }


        log.info("runOpen size {}", fList.size());
        int notTd = 0;
        for (EmCList emCList : fList) {
            //判断昨天集合竞价
            log.info("runOpen code {}", JSON.toJSONString(emCList));

            //判断fx
            List<EmDailyK> ks = emClient.getDailyKs(emCList.getF12Code(), LocalDate.now(), 300, true);
            if (ks.size() < 300) {
                log.info("runOpen code {} size < 300", emCList.getF12Code());
                continue;
            }


            //不清楚这个时候 是否出了最新的k
            EmDailyK k = ks.get(ks.size() - 1);

            String td = LocalDate.now().toString().replaceAll("-", "");

            if (td.equalsIgnoreCase(k.getTradeDate())) {
                ks.remove(ks.size() - 1);
            } else {
                notTd++;
                if (notTd > 3) {
                    return;
                }
            }

            Map<String, BigDecimal[]> ma = MaUtil.ma(ks);
            BigDecimal[] ma5 = ma.get("ma5");
            BigDecimal[] ma10 = ma.get("ma10");
            BigDecimal[] ma20 = ma.get("ma20");
            BigDecimal[] ma30 = ma.get("ma30");
            BigDecimal[] ma60 = ma.get("ma60");
            BigDecimal[] ma120 = ma.get("ma120");
            BigDecimal[] ma250 = ma.get("ma250");

            int last = ks.size() - 1;
            if (k.getClose().compareTo(ma5[last]) < 0
                    || k.getClose().compareTo(ma10[last]) < 0
                    || k.getClose().compareTo(ma20[last]) < 0
                    || k.getClose().compareTo(ma30[last]) < 0
                    || k.getClose().compareTo(ma60[last]) < 0
                    || k.getClose().compareTo(ma120[last]) < 0
                    || k.getClose().compareTo(ma250[last]) < 0) {
                log.info("runOpen code {} 不满足均线之上", emCList.getF12Code());
                continue;
            }


            //计算 amt
            BigDecimal dayAmtTop10 = emClient.amtTop10p(ks);
            BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
            BigDecimal fAmt = BDUtil.b0_1.multiply(hourAmt);
            if (fAmt.compareTo(BDUtil.B1000W) < 0) {
                log.info("runOpen code {} 不满足条件(约成交额 fAmt<500w)", emCList.getF12Code());
                continue;
            }


            //成交量放大倍数
            BigDecimal fx = k.getAmt().divide(fAmt, 4, RoundingMode.HALF_UP);
            if (fx.compareTo(BDUtil.B5) > 0) {
                String content = k.getCode() + k.getName()
                        + "<br>bk=" + k.getBk()
                        + "<br>p=" + k.getClose()
                        + "<br>pct=" + k.getPct() + "%"
                        + "<br>fx=" + fx
                        + "<br>amt=" + BDUtil.amtHuman(k.getAmt())
                        + "<br>td=" + LocalDateTime.now().toString().substring(0, 16);
                try {
                    String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);
                    wxUtil.send(encodedContent);

                } catch (Exception ex) {
                    log.error("{}", ex.getMessage(), ex);
                }

            }
            log.info("runOpen code {} 不满足条件 fx > 5 fx={}", emCList.getF12Code(),fx);
        }
    }


}

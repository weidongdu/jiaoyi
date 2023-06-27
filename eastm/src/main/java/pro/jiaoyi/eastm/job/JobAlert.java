package pro.jiaoyi.eastm.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.model.EastSpeedInfo;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.util.EmMaUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static pro.jiaoyi.eastm.api.EmClient.*;

@Component
@Slf4j
public class JobAlert {
    //监控 放量有涨速


    public static String TIP ="";


    static {
        TIP += "<br>卖出: 成本价附近,昨日低点";
        TIP += "<br>卖出: 小幅整理期,震荡底部";
        TIP += "<br>卖出: 趋势未结束,底仓持有";
        TIP += "<br>卖出: 交易有盈利,浮动止盈";
        TIP += "<br>买入: 计划外的票,谨慎开仓";
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

    //cron 每天上午8点 清空
    @Scheduled(cron = "0 0 9 * * ?")
    public void clear() {
        DATE_CODE_NAME_MAP.clear();
        DATE_NAME_CODE_MAP.clear();
        DATE_STOCK_CODE_BK_MAP.clear();
        BK_MAP.clear();
        DATE_LIST_MAP.clear();
        DATE_KLINE_MAP.clear();
    }


    @Scheduled(fixedRate = 1000 * 10L)
    public void run() {
        if (!EmRealTimeClient.tradeTime()) return;

        Integer am = DAY_COUNT_MAP.get(LocalDate.now() + AM);
        if (am == null) {
            String content = "监控启动" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateUtil.PATTERN_yyyy_MM_dd+ "_"+ DateUtil.PATTERN_HH_mm_ss));
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
                if (code.startsWith("8")) continue;
                //过滤 假设涨停也无法满足条件
                List<String> blockList = DAY_BLOCKLIST_MAP.computeIfAbsent(LocalDate.now(), k -> new ArrayList<>());
                if (blockList.contains(code)) {
                    log.info("block code {} {}", code, name);
                    continue;
                }

                log.info("run speed {} {} {}", code, name, top.getSpeed_f22());

                List<EmDailyK> dailyKs = emClient.getDailyKs(code, LocalDate.now(), 300, true);
                if (dailyKs.size() < 250) {
                    log.info("k size {} < 250", dailyKs.size());
                    continue;
                }

                EmDailyK k = dailyKs.get(dailyKs.size() - 1);


                int tu = emRealTimeClient.tu(dailyKs, 60, 60, 0.4d);
                if (tu != 0) {
                    Map<String, BigDecimal[]> ma = EmMaUtil.ma(dailyKs);

                    BigDecimal[] ma5 = ma.get("ma5");
                    BigDecimal[] ma10 = ma.get("ma10");
                    BigDecimal[] ma20 = ma.get("ma20");
                    BigDecimal[] ma30 = ma.get("ma30");
                    BigDecimal[] ma60 = ma.get("ma60");
                    BigDecimal[] ma120 = ma.get("ma120");
                    BigDecimal[] ma250 = ma.get("ma250");

                    BigDecimal ma5_value = ma5[ma5.length - 1];
                    BigDecimal ma10_value = ma10[ma10.length - 1];
                    BigDecimal ma20_value = ma20[ma20.length - 1];
                    BigDecimal ma30_value = ma30[ma30.length - 1];
                    BigDecimal ma60_value = ma60[ma60.length - 1];
                    BigDecimal ma120_value = ma120[ma120.length - 1];
                    BigDecimal ma250_value = ma250[ma250.length - 1];


                    log.info("run {} {}", code, name);
                    if (k.getClose().compareTo(ma5_value) < 0
                            || k.getClose().compareTo(ma10_value) < 0
                            || k.getClose().compareTo(ma20_value) < 0
                            || k.getClose().compareTo(ma30_value) < 0
                            || k.getClose().compareTo(ma60_value) < 0
                            || k.getClose().compareTo(ma120_value) < 0
                            || k.getClose().compareTo(ma250_value) < 0) {
                        log.info("不满足均线之上");
                        blockList.add(code);
                        continue;
                    }
                    BigDecimal dayAmtTop10 = emClient.amtTop10p(dailyKs);
                    BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
                    BigDecimal fAmt = new BigDecimal("0.1").multiply(hourAmt);
                    if (fAmt.compareTo(new BigDecimal("2500000")) < 0) {
                        continue;
                    }
                    //判断70s 内 是否大于 0.1 fAmt
                    BigDecimal fenshiAmt = emRealTimeClient.getFenshiAmt(code, 70);
                    //成交量放大倍数
                    BigDecimal fx = fenshiAmt.divide(hourAmt, 2, RoundingMode.HALF_UP);
                    if (fx.compareTo(new BigDecimal("0.1")) < 0) {
                        log.info("成交量不满足条件");
                        continue;
                    }


                    String amtStr = amtStr(fAmt);
                    String fenshiAmtStr = amtStr(fenshiAmt);

                    String content = code + "_" + name + "_" + k.getBk()
                            + "<br>" + "价格=" + k.getClose() + ",涨幅=" + k.getPct()
                            + "<br>" + "标准量=" + amtStr + ",M1=" + fx + "_" + fenshiAmtStr
                            + "<br>" + LocalDateTime.now().toString().substring(0, 16);
                    log.info("价格突破成功 {}", content.replaceAll("<br>", "\n"));
                    boolean push = checkSendWxCount(code);
                    if (push) {
                        wxUtil.send(content);
                    }


                } else {
                    //设置 最高价, 如果还不满足 , 加入block list
//                    EmDailyK k = dailyKs.get(dailyKs.size() - 1);
                    BigDecimal f = new BigDecimal("1.2");
                    if (k.getCode().startsWith("60") || k.getCode().startsWith("0")) {
                        f = new BigDecimal("1.1");
                    }
                    k.setHigh(f.multiply(k.getPreClose()).setScale(2, RoundingMode.HALF_UP));
                    int tu2 = emRealTimeClient.tu(dailyKs, 60, 60, 0.4d);
                    if (tu2 == 0) {
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

        if (SEND_WX_MAP.size() > 1000){
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

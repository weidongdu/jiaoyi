package pro.jiaoyi.eastm.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.model.EastSpeedInfo;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class JobAlert {
    //监控 放量有涨速


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
    @Scheduled(cron = "0 0 8 * * ?")
    public void clear() {
        DAY_BLOCKLIST_MAP.clear();
    }


    @Scheduled(fixedRate = 1000 * 10L)
    public void run() {
        if (!EmRealTimeClient.tradeTime()) return;

        Integer am = DAY_COUNT_MAP.get(LocalDate.now() + AM);
        if (am == null) {
            wxUtil.send("监控启动" + LocalDateTime.now());
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

                boolean tu = emRealTimeClient.tu(dailyKs, 30, 30, 0.4d);
                if (tu) {
                    BigDecimal[] closeArr = dailyKs.stream().map(EmDailyK::getClose).toList().toArray(new BigDecimal[0]);

                    BigDecimal[] ma5 = MaUtil.ma(5, closeArr, 3);
                    BigDecimal[] ma10 = MaUtil.ma(10, closeArr, 3);
                    BigDecimal[] ma20 = MaUtil.ma(20, closeArr, 3);
                    BigDecimal[] ma30 = MaUtil.ma(30, closeArr, 3);
                    BigDecimal[] ma60 = MaUtil.ma(60, closeArr, 3);
                    BigDecimal[] ma120 = MaUtil.ma(120, closeArr, 3);
                    BigDecimal[] ma250 = MaUtil.ma(250, closeArr, 3);

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
                    wxUtil.send(content);


                } else {
                    //设置 最高价, 如果还不满足 , 加入block list
//                    EmDailyK k = dailyKs.get(dailyKs.size() - 1);
                    BigDecimal f = new BigDecimal("1.2");
                    if (k.getCode().startsWith("60") || k.getCode().startsWith("0")) {
                        f = new BigDecimal("1.1");
                    }
                    k.setHigh(f.multiply(k.getPreClose()).setScale(2, RoundingMode.HALF_UP));
                    boolean tu2 = emRealTimeClient.tu(dailyKs, 30, 30, 0.4d);
                    if (!tu2) {
                        log.info("涨停价还不满足,加入block list,{}", k);
                        blockList.add(code);
                    }
                }

            }
        }

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

}

package pro.jiaoyi.eastm.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
import java.util.Collections;
import java.util.List;

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


    @Scheduled(fixedRate = 1000 * 10L)
    public void run() {
        if (!EmRealTimeClient.tradeTime()) return;

        List<EastSpeedInfo> tops = emRealTimeClient.getSpeedTop(50);
        log.info("speed list {}", tops.size());
        if (tops.size() > 0) {
            for (EastSpeedInfo top : tops) {
                String code = top.getCode_f12();
                String name = top.getName_f14();
                if (code.startsWith("8")) continue;

                log.info("run speed {} {} {}", code, name, top.getSpeed_f22());

                List<EmDailyK> dailyKs = emClient.getDailyKs(code, LocalDate.now(), 200, true);
                if (dailyKs.size() < 120) {
                    log.info("k size {} < 120", dailyKs.size());
                    continue;
                }


                boolean tu = emRealTimeClient.tu(dailyKs, 60, 60, 0.4d);
                if (tu) {
                    log.info("run {} {}", code, name);
                    BigDecimal dayAmtTop10 = amtTop10p(dailyKs);
                    BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
                    BigDecimal fAmt = new BigDecimal("0.1").multiply(hourAmt);
                    String amtStr = "";

                    if (fAmt.compareTo(B_Y) > 0) {
                        amtStr = fAmt.divide(B_Y, 2, RoundingMode.HALF_UP) + "亿";
                    } else {
                        amtStr = fAmt.divide(B_W, 0, RoundingMode.HALF_UP) + "万";
                    }
                    log.info("价格突破成功 code={} name={} 分时量{}", code, name, amtStr);
                    String content = code + name + amtStr + "<br>" + LocalDateTime.now().toString().substring(0, 16);
                    wxUtil.send(content);
                }
            }
        }

    }


    public BigDecimal amtTop10p(List<EmDailyK> dailyKs) {
        ArrayList<BigDecimal> amts = new ArrayList<>();
        int size = dailyKs.size();
        for (int i = 1; i <= 60; i++) {
            EmDailyK k = dailyKs.get(size - 1 - i);
            amts.add(k.getAmt());
        }
        Collections.sort(amts);

        int avg = 6;
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < avg; i++) {
            BigDecimal amt = amts.get(amts.size() - 1 - i);
            total = total.add(amt);
        }
        return total.divide(BigDecimal.valueOf(avg), 0, RoundingMode.HALF_UP);
    }
}

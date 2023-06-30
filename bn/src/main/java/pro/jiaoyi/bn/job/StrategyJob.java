package pro.jiaoyi.bn.job;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.bn.config.WxUtil;
import pro.jiaoyi.bn.model.BnK;
import pro.jiaoyi.bn.sdk.FutureApi;
import pro.jiaoyi.common.strategy.BreakOutStrategy;
import pro.jiaoyi.common.util.DateUtil;

import java.util.*;

import static pro.jiaoyi.common.strategy.BreakOutStrategy.SIDE_MAP;

@Slf4j
@Component
public class StrategyJob {

    @Resource
    private FutureApi futureApi;

    @Resource
    private WxUtil wxUtil;

    @Value("${bn.top}")
    private int top;

    @Value("${bn.kline.interval}")
    private String interval;

    public static Set<String> SYMBOLS = new HashSet<>();

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void exchangeInfo() {
//        List<String> exchangeInfo = futureApi.getExchangeInfo();
//        if (exchangeInfo.size() == 0) return;
//        SYMBOLS.addAll(exchangeInfo);
        log.info("run exchangeInfo");
        List<String> top100 = futureApi.ticker24hrSymbol(top);
        SYMBOLS.addAll(top100);
        log.info("stop exchangeInfo");
    }

//    @Scheduled(fixedRate = 20 * 1000)
//    public void kline_m1() {
//        kline("1m",240);
//    }

    @Scheduled(fixedRate = 20 * 1000)
    public void kline_m5() {
        kline("5m",60);
    }

    @Scheduled(fixedRate = 20 * 1000)
    public void kline_m30() {
        kline("30m",60);
    }
    @Scheduled(fixedRate = 20 * 1000)
    public void kline_h4() {
        kline("4h",60);
    }

    public void kline(String p, int daysHigh) {
        log.info("run kline SYMBOLS.size={}", SYMBOLS.size());
        if (SYMBOLS.size() == 0) {
            exchangeInfo();
            return;
        }

        ArrayList<String> list = new ArrayList<>(SYMBOLS);
        Collections.shuffle(list);

        for (String symbol : SYMBOLS) {
            if (!symbol.endsWith("USDT")) {
                continue;
            }
            List<BnK> kline = futureApi.kline(symbol, p, 490);
            if (kline.size() == 0) {
                continue;
            }
            kline.forEach(bnK -> bnK.setName(symbol));
            log.info("kline {} size={}", symbol, kline.size());
            int last = kline.size() - 1;
            int days = 60;
            int b = BreakOutStrategy.breakOut(kline, days, daysHigh, days, 0.4f);
            if (b != 0) {
                String content = SIDE_MAP.get(b) + "_" + p;
                content += "<br>" + symbol + "_" + kline.get(last).getClose();
                content += "<br>" + "trade=" + DateUtil.tsToStr(kline.get(last).getTsOpen(), DateUtil.PATTERN_yyyyMMdd_HH_mm_ss);
                content += "<br>" + "push=" + DateUtil.tsToStr(new Date().getTime(), DateUtil.PATTERN_yyyyMMdd_HH_mm_ss);
                content += "<br><br>" + "http://190.92.246.121:28080/v/diff/lineRace?i%3D" + p + "%26s=" + symbol;

                wxUtil.send(content);
            }
        }

    }


}

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

@Slf4j
@Component
public class StrategyJob {

    @Resource
    private FutureApi futureApi;

    @Resource
    private WxUtil wxUtil;

    @Value("${bn.top}")
    private int top;

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

    @Scheduled(fixedRate = 20 * 1000)
    public void kline() {
        log.info("run kline SYMBOLS.size={}", SYMBOLS.size());
        if (SYMBOLS.size() == 0) return;

        ArrayList<String> list = new ArrayList<>(SYMBOLS);
        Collections.shuffle(list);

        for (String symbol : SYMBOLS) {
            if (!symbol.endsWith("USDT")) {
                continue;
            }
            List<BnK> kline = futureApi.kline(symbol, "5m", 300);
            log.info("kline {} size={}", symbol, kline.size());
            int last = kline.size() - 1;
            int days = 60;
            boolean b = BreakOutStrategy.breakOut(kline, days, days, days, 0.4f);
            if (b) {
                String content = "BreakOut";
                content += "<br>" + symbol + "_" + kline.get(last).getClose();
                content += "<br>" + "trade=" + DateUtil.tsToStr(kline.get(last).getTsOpen(), DateUtil.PATTERN_yyyyMMdd_HH_mm_ss);
                content += "<br>" + "push=" + DateUtil.tsToStr(new Date().getTime(), DateUtil.PATTERN_yyyyMMdd_HH_mm_ss);
                content += "<br><br>" + "http://190.92.246.121:28080/v/diff/lineRace?i=1m&s=" + symbol;

                wxUtil.send(content);
            }
        }

    }


}

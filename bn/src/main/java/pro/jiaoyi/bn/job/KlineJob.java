package pro.jiaoyi.bn.job;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.bn.config.WxUtil;
import pro.jiaoyi.bn.model.BnK;
import pro.jiaoyi.bn.model.OpenInterestDto;
import pro.jiaoyi.bn.sdk.FutureApi;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.CollectionsUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.EmojiUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
public class KlineJob {

    @Resource
    private WxUtil wxUtil;
    @Resource
    private FutureApi futureApi;

    /*
    btc > 1626950400000
     */
    public static Map<String, Long> WX_SYMBOL_SEND_COUNT = new java.util.concurrent.ConcurrentHashMap<>();

    /*
    btc > [count#1626950400000]
     */
    public static Map<String, List<Long>> SYMBOL_UP_COUNT_1H = new java.util.concurrent.ConcurrentHashMap<>();
    public static Map<String, List<Long>> SYMBOL_DN_COUNT_1H = new java.util.concurrent.ConcurrentHashMap<>();

    public static final Set<String> BLOCK_SET = new HashSet<>();
    @Scheduled(fixedDelay = 10 * 1000)
    public void run() {
        List<String> topN = getTop(100);
        ArrayList<String> upList = new ArrayList<>();
        ArrayList<String> dnList = new ArrayList<>();
        HashMap<String, BigDecimal> priceMap = new HashMap<>();
        HashMap<String, BigDecimal> oiMap = new HashMap<>();
        String p = "30m";
        for (String symbol : topN) {
            if (BLOCK_SET.contains(symbol)) continue;

            if (!symbol.endsWith("USDT")) continue;
            List<BnK> kline = futureApi.kline(symbol, p, 499);
            priceMap.put(symbol, kline.get(kline.size() - 1).getClose());
            int maUpAndMaAbove = maUpAndMaAbove(kline);
            if (maUpAndMaAbove == 0) continue;

            //合约chicangl
            List<OpenInterestDto> oiList = futureApi.getOI(symbol);
            if (oiList == null || oiList.size() == 0) continue;
            BigDecimal boi = new BigDecimal(oiList.get(oiList.size() - 1).getSumOpenInterestValue());
            oiMap.put(symbol, boi);
            int oiSide = ioSide(oiList);

            if (maUpAndMaAbove == 1 && oiSide == 1) {
                //此时已经满足条件了 , 判断一下长周期
                log.info("多头{}", symbol);
                STATS_LIST.add(new Stats(symbol, p, 1, LocalDateTime.now(), boi));

//                if (check30(symbol,"30m") != 1) continue;
                if (boi.compareTo(BDUtil.B2000W) < 0) continue;
                upList.add(symbol);
                addCount(symbol, 1);
            }

            if (maUpAndMaAbove == -1 && oiSide == 1) {
                log.info("空头{}", symbol);
                STATS_LIST.add(new Stats(symbol, p, -1, LocalDateTime.now(), boi));

//                if (check30(symbol,"30m") != -1) continue;
                if (boi.compareTo(BDUtil.B2000W) < 0) continue;
                dnList.add(symbol);
                addCount(symbol, -1);
            }
        }

        HashMap<String, Integer> sortMap = new HashMap<>();
        if (upList.size() > 0) {
            for (String s : upList) {
                sortMap.put(s, SYMBOL_UP_COUNT_1H.get(s).size());
            }
            upList = new ArrayList<>(CollectionsUtil.sortByValue(sortMap, true).keySet());

            for (int i = 0; i < upList.size(); i++) {
                String s = upList.get(i);
                s += "_x" + SYMBOL_UP_COUNT_1H.get(s).size() + "_" + priceMap.get(s) + "_" + BDUtil.amtHuman(oiMap.get(s));
                upList.set(i, s);
            }
            send(String.join("<br>", upList), EmojiUtil.UP + EmojiUtil.UP + EmojiUtil.UP);
        }

        if (dnList.size() > 0) {
            sortMap.clear();
            for (String s : dnList) {
                sortMap.put(s, SYMBOL_DN_COUNT_1H.get(s).size());
            }
            dnList = new ArrayList<>(CollectionsUtil.sortByValue(sortMap, true).keySet());

            for (int i = 0; i < dnList.size(); i++) {
                String s = dnList.get(i);
                s += "x" + SYMBOL_DN_COUNT_1H.get(s).size() + "_" + priceMap.get(s);
                dnList.set(i, s);
            }
            send(String.join("<br>", dnList), EmojiUtil.DOWN + EmojiUtil.DOWN + EmojiUtil.DOWN);
        }
    }

    public List<String> getTop(int topN) {
        List<String> symbols = futureApi.ticker24hrSymbol(topN);
        ArrayList<String> list = new ArrayList<>(symbols);
        Collections.shuffle(list);
        return list;
    }

    /**
     * 1 多头
     * -1 空头
     * 0 unknown
     *
     * @return
     */
    public int maUpAndMaAbove(List<BnK> kline) {
//        List<BnK> kline = futureApi.kline(symbol, "5m", 499);
        if (kline.size() < 499) return 0;

        int maAboveOrUnder = MaUtil.maAboveOrUnder(kline, 8);
        int maUpOrDown = MaUtil.maUpOrDown(kline, 8);

        if (maAboveOrUnder * maUpOrDown == 1) {
            return maAboveOrUnder;
        }

        return 0;
    }


    public int check30(String symbol, String p) {
        List<BnK> kline30 = futureApi.kline(symbol, p, 499);
        return maUpAndMaAbove(kline30);
    }

    public void send(String symbol, String content) {
        content = symbol.replaceAll("USDT", "") + "<br>" + content + "<br>" + LocalDateTime.now();
        Long ts = WX_SYMBOL_SEND_COUNT.get(symbol);
        long now = System.currentTimeMillis();
        if (ts == null) {
            WX_SYMBOL_SEND_COUNT.put(symbol, now);
            wxUtil.send(content);
            return;
        }

        // 1小时内发送过
        if (now - ts < 60 * 60 * 1000) {
            log.info("1小时内发送过 {}", content);
        } else {
            WX_SYMBOL_SEND_COUNT.put(symbol, now);
            wxUtil.send(content);
        }
    }

    public void addCount(String symbol, int side) {
        if (side == 1) {
            List<Long> list = SYMBOL_UP_COUNT_1H.computeIfAbsent(symbol, k -> new ArrayList<>());
            list.add(System.currentTimeMillis());
        }
        if (side == -1) {
            List<Long> list = SYMBOL_DN_COUNT_1H.computeIfAbsent(symbol, k -> new ArrayList<>());
            list.add(System.currentTimeMillis());
        }

        //移除1小时之前的数据
        long now = System.currentTimeMillis();
        long h1 = 60 * 60 * 1000;
        SYMBOL_UP_COUNT_1H.forEach((k, v) -> {
            v.removeIf(aLong -> now - aLong > h1);
        });
        SYMBOL_DN_COUNT_1H.forEach((k, v) -> {
            v.removeIf(aLong -> now - aLong > h1);
        });
    }

    public int ioSide(List<OpenInterestDto> oiList) {
        //要求连续3次 持仓上升
//        List<OpenInterestDto> oiList = futureApi.getOI(symbol);
        if (oiList == null || oiList.size() == 0) return 0;

        int last = oiList.size() - 1;
        BigDecimal o1 = new BigDecimal(oiList.get(last).getSumOpenInterest());
        BigDecimal o2 = new BigDecimal(oiList.get(last - 1).getSumOpenInterest());
        BigDecimal o3 = new BigDecimal(oiList.get(last - 2).getSumOpenInterest());

        if (o1.compareTo(o2) > 0 && o2.compareTo(o3) > 0) {
            return 1;
        }

        if (o1.compareTo(o2) < 0 && o2.compareTo(o3) < 0) {
            return -1;
        }

        return 0;
    }

    /**
     * 这里统计符合k线形态的stats
     * 按时间统计数据
     */
//    @Scheduled(fixedDelay = 10 * 1000)
    public void chart() {
        //目前这有 period 5分钟的数据

        //STATS_LIST删除1小时之前的数据
        LocalDateTime h1 = LocalDateTime.now().minusHours(1);
        STATS_LIST.removeIf(stats -> stats.getTs().isBefore(h1));

        List<LocalDateTime> dsList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        dsList.add(now.minusSeconds(30));
        dsList.add(now.minusSeconds(60));
        dsList.add(now.minusSeconds(2 * 60));
        dsList.add(now.minusSeconds(3 * 60));
        dsList.add(now.minusSeconds(5 * 60));
        dsList.add(now.minusSeconds(15 * 60));
        dsList.add(now.minusSeconds(30 * 60));
        dsList.add(now.minusSeconds(60 * 60));

        for (LocalDateTime d : dsList) {
            //过滤出最近30s的数据
            List<Stats> statsList = STATS_LIST.stream().filter(stats -> stats.getTs().isAfter(d)).toList();
            String key = d.format(DateTimeFormatter.ofPattern(DateUtil.PATTERN_yyyyMMdd_HHmmss));
            //统计每个方向的数量
            for (Stats stats : statsList) {

                if (stats.getSide() == 1) {
                    UP_SIDE_MAP.put(key, UP_SIDE_MAP.getOrDefault(key, 0L) + 1);
                }
                if (stats.getSide() == -1) {
                    DN_SIDE_MAP.put(key, DN_SIDE_MAP.getOrDefault(key, 0L) + 1);
                }
            }
        }

//        log.info("chart up={} dn={}", JSON.toJSONString(UP_SIDE_MAP),JSON.toJSONString(DN_SIDE_MAP));



    }

    public static final List<Stats> STATS_LIST = new ArrayList<>();
    //统计每个分组的数量 最近 30s 1min 5min 趋势
    public static final Map<String, Long> UP_SIDE_MAP = new HashMap<>();
    public static final Map<String, Long> DN_SIDE_MAP = new HashMap<>();

}

@Data
class Stats {
    String symbol;
    String period;
    int side;
    LocalDateTime ts;
    BigDecimal amt;

    public Stats(String symbol, String s, int i, LocalDateTime now, BigDecimal boi) {
        this.symbol = symbol;
        this.period = s;
        this.side = i;
        this.ts = now;
        this.amt = boi;
    }
}
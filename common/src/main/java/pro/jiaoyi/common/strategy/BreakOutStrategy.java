package pro.jiaoyi.common.strategy;

import lombok.extern.slf4j.Slf4j;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.model.K;
import pro.jiaoyi.common.util.DateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 突破策略
 */
@Slf4j
public class BreakOutStrategy {

    public static <T extends K> boolean breakOut(List<T> dailyKs, int ignoreDays, int daysHigh, int boxDays, double boxDaysFactor) {
        int max = 60;
        //最小值校验  由于不同周期, 结构类似, 所以要限制
        ignoreDays = Math.max(ignoreDays, max);
        daysHigh = Math.max(daysHigh, max);
        boxDays = Math.max(boxDays, max);
        if (dailyKs.size() < ignoreDays) return false;


        Map<String, BigDecimal[]> ma = MaUtil.ma(dailyKs);
        BigDecimal[] ma5 = ma.get("ma5");
        if (ma5.length == 0) return false;
        BigDecimal[] ma10 = ma.get("ma10");
        BigDecimal[] ma20 = ma.get("ma20");
        BigDecimal[] ma30 = ma.get("ma30");
        BigDecimal[] ma60 = ma.get("ma60");

        int size = dailyKs.size();
        int index = size - 1;

        K k = dailyKs.get(size - 1);
        if (k.getClose().compareTo(k.getOpen()) < 0
                || k.getClose().compareTo(BigDecimal.valueOf(40)) > 0) {
            log.info("今日开盘价{} > 最新价{}, 不符合条件", k.getOpen(), k.getClose());
            return false;
        }


        if (k.getPct().compareTo(BigDecimal.ZERO) > 0
                && k.getClose().compareTo(ma5[index]) > 0
                && k.getClose().compareTo(ma10[index]) > 0
                && k.getClose().compareTo(ma20[index]) > 0
                && k.getClose().compareTo(ma30[index]) > 0
                && k.getClose().compareTo(ma60[index]) > 0) {

            int count = 0;
            for (int j = 1; j < index - 1; j++) {
                if (index - j == 1) {
                    log.info("遍历所有, 持续新高 {}天 {}", j, dailyKs.get(index - j));
                    count = j;
                    break;
                }
                BigDecimal high = dailyKs.get(index - j).getHigh();
                if (high.compareTo(k.getClose()) >= 0) {
                    log.info("打破新高截止, {}天 {}", j, dailyKs.get(index - j));
                    count = j;
                    break;
                }
            }
            log.info("over days high , count = {} high", count);

            ArrayList<BigDecimal> highList = new ArrayList<>();
            ArrayList<BigDecimal> lowList = new ArrayList<>();

            int countBox = 0;//箱体计数
            for (int j = 1; j < count; j++) {
                //1, 高点超过高点
                //2, 低点低于高点
                int tmpIndex = index - j;
                K dk = dailyKs.get(tmpIndex);
                if (k.getLow().compareTo(dk.getHigh()) < 0 && k.getHigh().compareTo(dk.getHigh()) > 0) {
                    countBox++;
                }
                highList.add(dk.getHigh());
                lowList.add(dk.getLow());
            }

            log.info("over box high , count = {} high", countBox);

            if (count > daysHigh && countBox > boxDays * boxDaysFactor) {
//                log.error("满足条件箱体突破 {}", k);

                K fk = dailyKs.get(size - count);
                log.error("满足条件箱体突破 {}k [{}] from {} to {}", count, fk.getName(),
                        DateUtil.tsToStr(fk.getTsOpen(), DateUtil.PATTERN_yyyyMMdd_HH_mm_ss) + "=" + fk.getClose(),
                        DateUtil.tsToStr(k.getTsOpen(), DateUtil.PATTERN_yyyyMMdd_HH_mm_ss) + "=" + k.getClose());

                return true;
            }

            if (count > daysHigh) {
                log.info("开始判断曲线");
                highList.add(k.getClose());
                Collections.sort(highList);
                int locationHigh = highList.indexOf(k.getClose());
                BigDecimal locationHighPct = BigDecimal.valueOf(locationHigh).divide(BigDecimal.valueOf(highList.size()), 3, RoundingMode.HALF_UP);
                log.info("开始判断曲线 最新价 location pct = {}", locationHighPct);
                if (locationHighPct.compareTo(new BigDecimal("0.9")) < 0) {
                    return false;
                }

                //获取lowList 最低价
                BigDecimal lowest = lowList.stream().min(BigDecimal::compareTo).get();
                log.info("开始判断曲线 最低价 = {}", lowest);
                int locationLow = lowList.indexOf(lowest);
                BigDecimal locationLowPct = BigDecimal.valueOf(locationLow).divide(BigDecimal.valueOf(lowList.size()), 3, RoundingMode.HALF_UP);
                if ((locationLowPct.compareTo(new BigDecimal("0.4")) < 0
                        || locationLowPct.compareTo(new BigDecimal("0.6")) > 0)) {
                    return false;
                }

                BigDecimal hh = highList.get(highList.size() - 1);
                if (lowest.compareTo(new BigDecimal("0.7").multiply(hh)) > 0) {
                    K fk = dailyKs.get(size - count);
                    log.error("开始判断曲线 曲线成功{}k [{}] from {} to {}", count, fk.getName(),
                            DateUtil.tsToStr(fk.getTsOpen(), DateUtil.PATTERN_yyyyMMdd_HH_mm_ss) + "=" + fk.getClose(),
                            DateUtil.tsToStr(k.getTsOpen(), DateUtil.PATTERN_yyyyMMdd_HH_mm_ss) + "=" + k.getClose());
                    return true;
                }
            }
        }
        return false;
    }

}

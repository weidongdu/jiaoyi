package pro.jiaoyi.common.strategy;

import lombok.extern.slf4j.Slf4j;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.model.K;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.EmojiUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 突破策略
 */
@Slf4j
public class BreakOutStrategy {
    public static final Map<Integer, String> SIDE_MAP = Map.of(
            1, EmojiUtil.UP + "箱体",
            2, EmojiUtil.UP + "曲线",
            -1, EmojiUtil.DOWN + "箱体",
            -2, EmojiUtil.DOWN + "down 曲线",
            0, "未知");

    public static <T extends K> int breakOut(List<T> dailyKs, int ignoreDays, int daysHigh, int boxDays, double boxDaysFactor) {
        int max = 60;
        //最小值校验  由于不同周期, 结构类似, 所以要限制
        ignoreDays = Math.max(ignoreDays, max);
        daysHigh = Math.max(daysHigh, max);
        boxDays = Math.max(boxDays, max);
        if (dailyKs.size() < ignoreDays) return 0;


        int size = dailyKs.size();
        int last = size - 1;
        K k = dailyKs.get(last);


        Map<String, BigDecimal[]> ma = MaUtil.ma(dailyKs);
        BigDecimal[] ma5 = ma.get("ma5");
        if (ma5.length == 0) return 0;

        BigDecimal[] ma10 = ma.get("ma10");
        BigDecimal[] ma20 = ma.get("ma20");
        BigDecimal[] ma30 = ma.get("ma30");
        BigDecimal[] ma60 = ma.get("ma60");


        //突破分2个方向
        //1. 向上突破

        String side = "[MAUP]";
        int keep = 2;
        if (k.getPct().compareTo(BigDecimal.ZERO) > 0) {
            log.info("{}pct {} > 0 ", side, k.getPct());
            //均线向上发散  keep2
            int keepCount = 0;
            for (int i = 0; i < keep; i++) {
                if (k.getClose().compareTo(ma5[last - i]) < 0) break;
                if (ma5[last - i].compareTo(ma10[last - i]) < 0) break;
                if (ma10[last - i].compareTo(ma20[last - i]) < 0) break;
                if (ma20[last - i].compareTo(ma30[last - i]) < 0) break;
                if (ma30[last - i].compareTo(ma60[last - i]) < 0) break;
                keepCount++;
            }

            if (keepCount == keep) {//满足发散
                log.info("{}满足发散 keepCount {}", side, keepCount);
                //判断箱体
                int boxCount = 0;
                int highCount = 0;
                //从最后向前遍历
                //找到最高点> k.getClose
                for (int i = 1; i < last; i++) {
                    int index = last - i;
                    K preK = dailyKs.get(index);
                    //1, 判断 x days 新高
                    if (preK.getHigh().compareTo(k.getClose()) > 0) {
                        highCount = i;
                        log.info("{}新高Stop {}", side, i);
                        break;
                    }

                    if (i == last - 1) {
                        log.info("{}新高Stop {}", side, i);
                        highCount = i;
                    }
                }

                if (highCount > daysHigh) {
                    log.info("{}新高{} > days{}", side, highCount, daysHigh);
                    BigDecimal bHighCount = new BigDecimal(highCount);
                    // 新高满足
                    //开始判断box
                    ArrayList<BigDecimal> h = new ArrayList<>();
                    ArrayList<BigDecimal> l = new ArrayList<>();
                    for (int i = 1; i < highCount; i++) {
                        int index = last - i;
                        K preK = dailyKs.get(index);
                        h.add(preK.getHigh());
                        l.add(preK.getLow());
                    }


                    //获取 list max
                    BigDecimal hMax = h.stream().max(BigDecimal::compareTo).get();
                    BigDecimal lMin = l.stream().min(BigDecimal::compareTo).get();
                    BigDecimal diff = hMax.subtract(lMin);
                    BigDecimal rangePct = diff.divide(lMin, 4, RoundingMode.HALF_UP);
                    log.info("{}hMax={} lMin={} diff={} rangePct={}", side, hMax, lMin, diff, rangePct);

                    if (rangePct.compareTo(BDUtil.b0_2) < 0) {
                        log.info("{}rangePct {} < {}", side, rangePct, BDUtil.b0_2);
                        //BOX
                        //1, up down range 20%
                        //2, high point size > 1/10
                        //3, low point size > 1/10
                        //range fit
                        BigDecimal hArea = hMax.subtract(diff.multiply(BDUtil.b0_2));
                        BigDecimal lArea = lMin.add(diff.multiply(BDUtil.b0_2));
                        long hAreaCount = h.stream().filter(b -> b.compareTo(hArea) > 0).count();
                        long lAreaCount = h.stream().filter(b -> b.compareTo(lArea) < 0).count();
                        BigDecimal areaSize = BDUtil.b0_1.multiply(bHighCount);
                        log.info("{}hAreaCount={} lAreaCount={} areaSize={}", side, hAreaCount, lAreaCount, areaSize);
                        if (new BigDecimal(hAreaCount).compareTo(areaSize) > 0
                                && new BigDecimal(lAreaCount).compareTo(areaSize) > 0) {
                            //box fit
                            log.warn("{}hit box up", side);
                            return 1;
                        }

                        //CURVE
                        //两头高
                        int lMinLocation = l.indexOf(lMin);
                        BigDecimal lMinLocationPct = new BigDecimal(lMinLocation).divide(bHighCount, 4, RoundingMode.HALF_UP);
                        if (lMinLocationPct.compareTo(BDUtil.b0_4) > 0 && lMinLocationPct.compareTo(BDUtil.b0_6) < 0) {
                            //中间低
                            log.info("{}curve lMinLocationPct={} range {}-{} ", side, lMinLocationPct, BDUtil.b0_4, BDUtil.b0_6);
                            BigDecimal highPct10 = hMax.subtract(diff.multiply(BDUtil.b0_1));
                            int count = 0;
                            for (int i = 0; i < 2; i++) {
                                //前面2个
                                if (h.get(i).compareTo(highPct10) < 0) break;
                                if (h.get(highCount - i).compareTo(highPct10) < 0) break;
                                count++;
                            }
                            if (count == 2) {
                                //fit curve
                                log.warn("{}hit curve up", side);
                                return 2;
                            }
                        }
                    }
                }
                //判断曲线
            }
        }


        //2. 向下突破
        if (k.getPct().compareTo(BigDecimal.ZERO) < 0) {
            side = "[MADOWN]";
            log.info("{}pct {} < 0 ", side, k.getPct());
            //均线向上发散  keep2
            int keepCount = 0;
            for (int i = 0; i < keep; i++) {
                if (k.getClose().compareTo(ma5[last - i]) > 0) break;
                if (ma5[last - i].compareTo(ma10[last - i]) > 0) break;
                if (ma10[last - i].compareTo(ma20[last - i]) > 0) break;
                if (ma20[last - i].compareTo(ma30[last - i]) > 0) break;
                if (ma30[last - i].compareTo(ma60[last - i]) > 0) break;
                keepCount++;
            }

            if (keepCount == keep) {//满足发散
                log.info("{}满足发散 keepCount {}", side, keepCount);
                //判断箱体
                int boxCount = 0;
                int lowCount = 0;
                //从最后向前遍历
                //找到最高点> k.getClose
                for (int i = 1; i < last; i++) {
                    int index = last - i;
                    K preK = dailyKs.get(index);
                    //1, 判断 x days 新高
                    if (preK.getLow().compareTo(k.getClose()) < 0) {
                        lowCount = i;
                        log.info("{}新低Stop {}", side, i);
                        break;
                    }

                    if (i == last - 1) {
                        log.info("{}新低Stop {}", side, i);
                        lowCount = i;
                    }
                }

                if (lowCount > daysHigh) {
                    log.info("{}新低{} > days{}", side, lowCount, daysHigh);
                    BigDecimal bLowCount = new BigDecimal(lowCount);
                    // 新高满足
                    //开始判断box
                    ArrayList<BigDecimal> h = new ArrayList<>();
                    ArrayList<BigDecimal> l = new ArrayList<>();
                    for (int i = 1; i < lowCount; i++) {
                        int index = last - i;
                        K preK = dailyKs.get(index);
                        h.add(preK.getHigh());
                        l.add(preK.getLow());
                    }


                    //获取 list max
                    BigDecimal hMax = h.stream().max(BigDecimal::compareTo).get();
                    BigDecimal lMin = l.stream().min(BigDecimal::compareTo).get();
                    BigDecimal diff = hMax.subtract(lMin);
                    BigDecimal rangePct = diff.divide(lMin, 4, RoundingMode.HALF_UP);
                    log.info("{}hMax={} lMin={} diff={} rangePct={}", side, hMax, lMin, diff, rangePct);

                    if (rangePct.compareTo(BDUtil.b0_2) < 0) {
                        log.info("{}rangePct {} < {}", side, rangePct, BDUtil.b0_2);
                        //BOX
                        //1, up down range 20%
                        //2, high point size > 1/10
                        //3, low point size > 1/10
                        //range fit
                        BigDecimal hArea = hMax.subtract(diff.multiply(BDUtil.b0_2));
                        BigDecimal lArea = lMin.add(diff.multiply(BDUtil.b0_2));
                        long hAreaCount = h.stream().filter(b -> b.compareTo(hArea) > 0).count();
                        long lAreaCount = h.stream().filter(b -> b.compareTo(lArea) < 0).count();
                        BigDecimal areaSize = BDUtil.b0_1.multiply(bLowCount);
                        log.info("{}hAreaCount={} lAreaCount={} areaSize={}", side, hAreaCount, lAreaCount, areaSize);
                        if (new BigDecimal(hAreaCount).compareTo(areaSize) > 0
                                && new BigDecimal(lAreaCount).compareTo(areaSize) > 0) {
                            //box fit
                            log.warn("{}hit box up", side);
                            return -1;
                        }

                        //CURVE
                        //两头高
                        int hMaxLocation = h.indexOf(hMax);
                        BigDecimal hMaxLocationPct = new BigDecimal(hMaxLocation).divide(bLowCount, 4, RoundingMode.HALF_UP);
                        if (hMaxLocationPct.compareTo(BDUtil.b0_4) > 0 && hMaxLocationPct.compareTo(BDUtil.b0_6) < 0) {
                            //中间低
                            log.info("{}curve lMinLocationPct={} range {}-{} ", side, hMaxLocationPct, BDUtil.b0_4, BDUtil.b0_6);
                            BigDecimal lowPct10 = lMin.add(diff.multiply(BDUtil.b0_1));
                            int count = 0;
                            for (int i = 0; i < 2; i++) {
                                //前面2个
                                if (l.get(i).compareTo(lowPct10) > 0) break;
                                if (l.get(lowCount - i).compareTo(lowPct10) > 0) break;
                                count++;
                            }
                            if (count == 2) {
                                //fit curve
                                log.warn("{}hit curve up", side);
                                return -2;
                            }
                        }
                    }
                }
                //判断曲线
            }
        }

        return 0;
    }


}

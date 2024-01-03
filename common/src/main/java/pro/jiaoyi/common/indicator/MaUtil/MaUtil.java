package pro.jiaoyi.common.indicator.MaUtil;


import com.alibaba.fastjson.JSON;
import pro.jiaoyi.common.model.K;
import pro.jiaoyi.common.util.BDUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MaUtil {


    /**
     * 顺序行为, 按时间正序  2021 2022
     *
     * @param period
     * @param priceArr
     * @param scale
     * @return
     */
    public static BigDecimal[] ma(int period, BigDecimal[] priceArr, int scale) {

        BigDecimal[] mas = new BigDecimal[priceArr.length];
        BigDecimal tmp = BigDecimal.ZERO;
        for (int i = 0; i < priceArr.length; i++) {
            tmp = tmp.add(priceArr[i]);

            if (i < period - 1) {
                mas[i] = BigDecimal.ZERO;
                continue;
            }

            if (i > period - 1) {
                tmp = tmp.subtract(priceArr[i - period]);
            }

            mas[i] = tmp.divide(BigDecimal.valueOf(period), scale, RoundingMode.HALF_UP);
        }
        return mas;
    }


    private static final Map<String, BigDecimal[]> EMPTY_MA_MAP;

    static {
        Map<String, BigDecimal[]> tempMap = new ConcurrentHashMap<>(7);
        BigDecimal[] m0 = new BigDecimal[0];
        tempMap.put("ma5", m0);
        tempMap.put("ma10", m0);
        tempMap.put("ma20", m0);
        tempMap.put("ma30", m0);
        tempMap.put("ma60", m0);
        tempMap.put("ma120", m0);
        tempMap.put("ma250", m0);
        EMPTY_MA_MAP = Collections.unmodifiableMap(tempMap);
    }


    public static <T extends K> BigDecimal maDiffPct60(List<T> list) {
        if (list == null || list.size() < 60) {
            return BDUtil.BN1; // -1
        }

        Map<String, BigDecimal[]> ma = ma(list);
        BigDecimal[] ma5 = ma.get("ma5");
        BigDecimal[] ma10 = ma.get("ma10");
        BigDecimal[] ma20 = ma.get("ma20");
        BigDecimal[] ma30 = ma.get("ma30");
        BigDecimal[] ma60 = ma.get("ma60");

        int last = list.size() - 1;
        ArrayList<BigDecimal> mas = new ArrayList<>(List.of(ma5[last], ma10[last], ma20[last], ma30[last], ma60[last]));
        mas.sort(BigDecimal::compareTo);

        BigDecimal min = mas.get(0);
        BigDecimal max = mas.get(4);

        return max.divide(min, 4, RoundingMode.HALF_UP);
    }


    /**
     * 均线多头还是空头
     *
     * @param list
     * @param <T>
     * @return
     */
    public static <T extends K> int maUpOrDown(List<T> list, int scale) {
        if (list == null || list.size() < 255) {
            return 0;
        }

        BigDecimal[] closeArr = list.stream().map(K::getClose).toList().toArray(new BigDecimal[0]);
        BigDecimal[] ma5 = MaUtil.ma(5, closeArr, scale);
        BigDecimal[] ma10 = MaUtil.ma(10, closeArr, scale);
        BigDecimal[] ma20 = MaUtil.ma(20, closeArr, scale);
        BigDecimal[] ma30 = MaUtil.ma(30, closeArr, scale);
        BigDecimal[] ma60 = MaUtil.ma(60, closeArr, scale);
        BigDecimal[] ma120 = MaUtil.ma(120, closeArr, scale);
        BigDecimal[] ma250 = MaUtil.ma(250, closeArr, scale);

        int last = list.size() - 1;

        if (ma5[last - 1].compareTo(ma5[last]) > 0
                && ma10[last - 1].compareTo(ma10[last]) > 0
                && ma20[last - 1].compareTo(ma20[last]) > 0
                && ma30[last - 1].compareTo(ma30[last]) > 0
                && ma60[last - 1].compareTo(ma60[last]) > 0
                && ma120[last - 1].compareTo(ma120[last]) > 0
                && ma250[last - 1].compareTo(ma250[last]) > 0

                && ma5[last - 2].compareTo(ma5[last - 1]) > 0
                && ma10[last - 2].compareTo(ma10[last - 1]) > 0
                && ma20[last - 2].compareTo(ma20[last - 1]) > 0
                && ma30[last - 2].compareTo(ma30[last - 1]) > 0
                && ma60[last - 2].compareTo(ma60[last - 1]) > 0
                && ma120[last - 2].compareTo(ma120[last - 1]) > 0
                && ma250[last - 2].compareTo(ma250[last - 1]) > 0
        ) {
            //均线向下
            return -1;
        }

        if (ma5[last - 1].compareTo(ma5[last]) < 0
                && ma10[last - 1].compareTo(ma10[last]) < 0
                && ma20[last - 1].compareTo(ma20[last]) < 0
                && ma30[last - 1].compareTo(ma30[last]) < 0
                && ma60[last - 1].compareTo(ma60[last]) < 0
                && ma120[last - 1].compareTo(ma120[last]) < 0
                && ma250[last - 1].compareTo(ma250[last]) < 0

                && ma5[last - 2].compareTo(ma5[last - 1]) < 0
                && ma10[last - 2].compareTo(ma10[last - 1]) < 0
                && ma20[last - 2].compareTo(ma20[last - 1]) < 0
                && ma30[last - 2].compareTo(ma30[last - 1]) < 0
                && ma60[last - 2].compareTo(ma60[last - 1]) < 0
                && ma120[last - 2].compareTo(ma120[last - 1]) < 0
                && ma250[last - 2].compareTo(ma250[last - 1]) < 0


        ) {
            //均线向上
            return 1;
        }

        return 0;
    }

    /**
     * 均线之上 或之下
     *
     * @param list
     * @param <T>
     * @return
     */
    public static <T extends K> int maAboveOrUnder(List<T> list, int scale) {
        if (list == null || list.size() < 255) {
            return 0;
        }

        BigDecimal[] closeArr = list.stream().map(K::getClose).toList().toArray(new BigDecimal[0]);
        BigDecimal[] ma5 = MaUtil.ma(5, closeArr, scale);
        BigDecimal[] ma10 = MaUtil.ma(10, closeArr, scale);
        BigDecimal[] ma20 = MaUtil.ma(20, closeArr, scale);
        BigDecimal[] ma30 = MaUtil.ma(30, closeArr, scale);
        BigDecimal[] ma60 = MaUtil.ma(60, closeArr, scale);
        BigDecimal[] ma120 = MaUtil.ma(120, closeArr, scale);
        BigDecimal[] ma250 = MaUtil.ma(250, closeArr, scale);


        int last = list.size() - 1;
        BigDecimal close = list.get(last).getClose();
        BigDecimal high = list.get(last).getHigh();
        BigDecimal low = list.get(last).getLow();

        BigDecimal[] mas = new BigDecimal[6];
        mas[0] = ma5[last];
        mas[1] = ma10[last];
        mas[2] = ma20[last];
        mas[3] = ma30[last];
        mas[4] = ma60[last];
        mas[5] = ma120[last];
        Arrays.sort(mas);
        BigDecimal min = mas[0];
        BigDecimal max = mas[5];

        BigDecimal maDiff = max.subtract(min);
        BigDecimal diffPct = maDiff.divide(min, 4, RoundingMode.HALF_UP);
        if (diffPct.compareTo(new BigDecimal("0.01")) > 0) {
            return 0;
        }


        //1. close 在均线之上
        if (close.compareTo(ma5[last]) > 0
                && close.compareTo(ma10[last]) > 0
                && close.compareTo(ma20[last]) > 0
                && close.compareTo(ma30[last]) > 0
                && close.compareTo(ma60[last]) > 0
                && close.compareTo(ma120[last]) > 0
                && close.compareTo(ma250[last]) > 0) {

            BigDecimal h = BigDecimal.ZERO;
            for (int i = last - 100; i < last; i++) {
                if (h.compareTo(list.get(i).getHigh()) < 0) {
                    h = list.get(i).getHigh();
                }
            }

            //2. 当前价是最高价
            if (high.compareTo(h) >= 0) {
                // 5-30 ma diff pct < 1%
                BigDecimal cDiffPct = close.subtract(max).divide(max, 4, RoundingMode.HALF_UP);
                if (cDiffPct.compareTo(new BigDecimal("0.01")) < 0) {
                    return 1;
                }
            }

        }
        if (close.compareTo(ma5[last]) < 0
                && close.compareTo(ma10[last]) < 0
                && close.compareTo(ma20[last]) < 0
                && close.compareTo(ma30[last]) < 0
                && close.compareTo(ma60[last]) < 0
                && close.compareTo(ma120[last]) < 0
                && close.compareTo(ma250[last]) < 0) {


            BigDecimal l = BigDecimal.ZERO;
            for (int i = last - 100; i < last; i++) {
                if (l.compareTo(list.get(i).getLow()) > 0) {
                    l = list.get(i).getLow();
                }
            }

            if (low.compareTo(l) <= 0) {

                BigDecimal cDiffPct = min.subtract(close).divide(min, 4, RoundingMode.HALF_UP);
                if (cDiffPct.compareTo(new BigDecimal("0.01")) < 0) {
                    return -1;
                }
            }
        }

        return 0;
    }

    public static <T extends K> Map<String, BigDecimal[]> ma(List<T> list) {

        if (list == null || list.size() < 5) {
            return EMPTY_MA_MAP;
        }

        Map<String, BigDecimal[]> maMap = new ConcurrentHashMap<>();

        BigDecimal[] closeArr = list.stream().map(K::getClose).toList().toArray(new BigDecimal[0]);
        BigDecimal[] ma5 = MaUtil.ma(5, closeArr, 3);
        BigDecimal[] ma10 = MaUtil.ma(10, closeArr, 3);
        BigDecimal[] ma20 = MaUtil.ma(20, closeArr, 3);
        BigDecimal[] ma30 = MaUtil.ma(30, closeArr, 3);
        BigDecimal[] ma60 = MaUtil.ma(60, closeArr, 3);
        BigDecimal[] ma120 = MaUtil.ma(120, closeArr, 3);
        BigDecimal[] ma250 = MaUtil.ma(250, closeArr, 3);

        maMap.put("ma5", ma5);
        maMap.put("ma10", ma10);
        maMap.put("ma20", ma20);
        maMap.put("ma30", ma30);
        maMap.put("ma60", ma60);
        maMap.put("ma120", ma120);
        maMap.put("ma250", ma250);

        BigDecimal[] lastMa = new BigDecimal[7];
        lastMa[0] = ma5[ma5.length - 1];
        lastMa[1] = ma10[ma10.length - 1];
        lastMa[2] = ma20[ma20.length - 1];
        lastMa[3] = ma30[ma30.length - 1];
        lastMa[4] = ma60[ma60.length - 1];
        lastMa[5] = ma120[ma120.length - 1];
        lastMa[6] = ma250[ma250.length - 1];

        maMap.put("last", lastMa);

        return maMap;
    }


    public static void main(String[] args) {
        BigDecimal[] pa = new BigDecimal[10];

        for (int i = 0; i < 10; i++) {
            pa[i] = new BigDecimal(i + 1);
        }

        System.out.println(JSON.toJSONString(pa));

        BigDecimal[] ma = ma(5, pa, 0);
        System.out.println(JSON.toJSONString(ma));
    }

}

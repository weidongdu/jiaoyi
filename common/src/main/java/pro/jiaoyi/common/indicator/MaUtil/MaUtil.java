package pro.jiaoyi.common.indicator.MaUtil;


import com.alibaba.fastjson.JSON;
import pro.jiaoyi.common.model.K;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

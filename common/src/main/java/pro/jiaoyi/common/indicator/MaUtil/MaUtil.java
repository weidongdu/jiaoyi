package pro.jiaoyi.common.indicator.MaUtil;


import com.alibaba.fastjson.JSON;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MaUtil {


    /**
     * 顺序行为, 按时间正序  2021 2022
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

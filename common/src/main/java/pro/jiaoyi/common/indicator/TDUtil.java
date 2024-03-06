package pro.jiaoyi.common.indicator;

import lombok.extern.slf4j.Slf4j;
import pro.jiaoyi.common.model.K;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
public class TDUtil {

    //限制条件 最多从前面60k开始
    public static final int limit_ks = 60;

    //setUp 阶段
    public static <T extends K> int tds(List<T> list) {

        if (list.size() < limit_ks) {
            return 0;
        }

        int tds = 0;
        BigDecimal hiMax = BigDecimal.ZERO;
        BigDecimal loMin = BigDecimal.valueOf(10000 * 10000L);

        for (int i = list.size() - limit_ks + 4; i < list.size(); i++) {
            K k = list.get(i);

            K k4 = list.get(i - 4);
            K k3 = list.get(i - 3);
            K k2 = list.get(i - 2);
            K k1 = list.get(i - 1);

            String s = k.getCode() + k.getName() + DateUtil.tsToStr(k.getTsClose(), "yyyy-MM-dd");


            //设置 hiMax loMin
            //最高 high
            if (hiMax.compareTo(k.getHigh()) < 0) {
                hiMax = k.getHigh();
            }
            //最低 low
            if (loMin.compareTo(k.getLow()) > 0) {
                loMin = k.getLow();
            }

            //Setup阶段：此阶段是找出9根连续的股票K线或者是Candlestick。
            // 对于一个上升趋势，每根K线的收盘价都必须高于其前面第四根K线的收盘价。对于下降趋势，每根K线的收盘价必须低于其前面第四根K线的收盘价。
            // 一旦找到这样的9根连续K线，我们就说Setup阶段完成了。这个阶段的目的是确定市场的短期趋势。

            int flag = 0;
            if (k.getClose().compareTo(k4.getClose()) >= 0
                    && k.getClose().compareTo(k3.getClose()) >= 0
                    && k.getClose().compareTo(k2.getClose()) >= 0
                    && k.getClose().compareTo(k1.getClose()) >= 0) {
                flag = 1;
            }

            if (k.getClose().compareTo(k4.getClose()) <= 0
                    && k.getClose().compareTo(k3.getClose()) <= 0
                    && k.getClose().compareTo(k2.getClose()) <= 0
                    && k.getClose().compareTo(k1.getClose()) <= 0) {
                flag = -1;
            }

            if (flag == 0) {
                //重置setup
                tds = 0;
                continue;
            }

            tds += flag;

            if (tds == 9) {
                log.info("tds 9, 满足上升趋势setup index={} k={}", i, s);
                tdc(list, i + 1, 1, hiMax, loMin);
                //看后面15k 涨跌幅
                StringBuffer sb = new StringBuffer();
                for (int j = 1; j < 15; j++) {
                    if (i + j >= list.size()) {
                        break;
                    }
                    K k15 = list.get(i + j);
                    BigDecimal diff = k15.getOpen().subtract(k.getClose());
                    BigDecimal pct = diff.divide(k.getClose(), 4, RoundingMode.HALF_UP);
                    sb.append(j).append("=").append(BDUtil.p100(pct)).append(",");
                }
                log.info("期待向下 index={} k={} {}", i, s, sb.toString());
            }

            if (tds == -9) {
                log.info("tds -9, 满足下降趋势setup index={} k={}", i, s);
                tdc(list, i + 1, -1, hiMax, loMin);
                //看后面15k 涨跌幅
                StringBuffer sb = new StringBuffer();
                for (int j = 1; j < 15; j++) {
                    if (i + j >= list.size()) {
                        break;
                    }
                    K k15 = list.get(i + j);
                    BigDecimal diff = k15.getOpen().subtract(k.getClose());
                    BigDecimal pct = diff.divide(k.getClose(), 4, RoundingMode.HALF_UP);
                    sb.append(j).append("=").append(BDUtil.p100(pct)).append(",");
                }
                log.info("期待向上 index={} k={} {}", i, s, sb.toString());
            }
        }

        return 0;
    }

    //countdown 阶段
    public static <T extends K> void tdc(List<T> list, int tdsIndex, int side, BigDecimal hiMax, BigDecimal loMin) {

        int tds = 0;
        for (int i = tdsIndex; i < list.size(); i++) {

            K k = list.get(i);
            K k1 = list.get(i - 1);
            K k2 = list.get(i - 2);
            String s = k.getCode() + k.getName() + DateUtil.tsToStr(k.getTsClose(), "yyyy-MM-dd");
            if (side == 1) {
                //reset
                if (k.getHigh().compareTo(hiMax) > 0) {
                    break;
                }
                if (k.getClose().compareTo(k1.getLow()) >= 0
                        && k.getClose().compareTo(k2.getLow()) >= 0) {
                    tds++;
                    if (tds == 13) {
                        log.info("tdc 13, countdown 终结 index={} k={}", i, s);
                        break;
                    }
                }
            }

            if (side == -1) {
                //reset
                if (k.getLow().compareTo(loMin) < 0) {
                    break;
                }
                if (k.getClose().compareTo(k1.getHigh()) <= 0
                        && k.getClose().compareTo(k2.getHigh()) <= 0) {
                    tds--;
                    if (tds == -13) {
                        log.info("tdc -13, 满足下降趋势countdown 终结 index={} k={}", i, s);
                        break;
                    }
                }
            }
        }

    }
}

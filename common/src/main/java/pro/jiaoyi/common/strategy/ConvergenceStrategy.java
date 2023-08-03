package pro.jiaoyi.common.strategy;

import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.meta.SideEnum;
import pro.jiaoyi.common.model.K;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ConvergenceStrategy {

    public static <T extends K> int side(List<T> ks) {
        if (ks == null || ks.size() < 250) {
            return SideEnum.NONE.getValue();
        }

        Map<String, BigDecimal[]> ma = MaUtil.ma(ks);
        BigDecimal[] ma5 = ma.get("ma5");
        BigDecimal[] ma10 = ma.get("ma10");
        BigDecimal[] ma20 = ma.get("ma20");
        BigDecimal[] ma30 = ma.get("ma30");
        BigDecimal[] ma60 = ma.get("ma60");
        BigDecimal[] ma120 = ma.get("ma120");
        BigDecimal[] ma250 = ma.get("ma250");

        int size = ks.size();
        int last = size - 1;


        K k = ks.get(last);
        BigDecimal close = k.getClose();

        if (close.compareTo(ma5[last]) > 0
                && close.compareTo(ma10[last]) > 0
                && close.compareTo(ma20[last]) > 0
                && close.compareTo(ma30[last]) > 0
                && close.compareTo(ma60[last]) > 0
                && close.compareTo(ma120[last]) > 0
                && close.compareTo(ma250[last]) > 0
        ) {
            return SideEnum.UP.getValue();
        }


        if (close.compareTo(ma5[last]) < 0
                && close.compareTo(ma10[last]) < 0
                && close.compareTo(ma20[last]) < 0
                && close.compareTo(ma30[last]) < 0
                && close.compareTo(ma60[last]) < 0
                && close.compareTo(ma120[last]) < 0
                && close.compareTo(ma250[last]) < 0
        ) {
            return SideEnum.UP.getValue();
        }


        return SideEnum.NONE.getValue();
    }
}

package pro.jiaoyi.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BDUtil {
    public static final BigDecimal B100 = new BigDecimal("100");
    public static final BigDecimal B90 = new BigDecimal("90");
    public static final BigDecimal B80 = new BigDecimal("80");
    public static final BigDecimal B70 = new BigDecimal("70");
    public static final BigDecimal B60 = new BigDecimal("60");
    public static final BigDecimal B50 = new BigDecimal("50");
    public static final BigDecimal B40 = new BigDecimal("40");
    public static final BigDecimal B30 = new BigDecimal("30");
    public static final BigDecimal B20 = new BigDecimal("20");
    public static final BigDecimal B10 = new BigDecimal("10");


    public static final BigDecimal B9 = new BigDecimal("9");
    public static final BigDecimal B8 = new BigDecimal("8");
    public static final BigDecimal B7 = new BigDecimal("7");
    public static final BigDecimal B6 = new BigDecimal("6");
    public static final BigDecimal B5 = new BigDecimal("5");
    public static final BigDecimal B4 = new BigDecimal("4");
    public static final BigDecimal B3 = new BigDecimal("3");
    public static final BigDecimal B2 = new BigDecimal("2");
    public static final BigDecimal B1 = new BigDecimal("1");


    public static final BigDecimal b0_9 = new BigDecimal("0.9");
    public static final BigDecimal b0_8 = new BigDecimal("0.8");
    public static final BigDecimal b0_7 = new BigDecimal("0.7");
    public static final BigDecimal b0_6 = new BigDecimal("0.6");
    public static final BigDecimal b0_5 = new BigDecimal("0.5");
    public static final BigDecimal b0_4 = new BigDecimal("0.4");
    public static final BigDecimal b0_3 = new BigDecimal("0.3");
    public static final BigDecimal b0_2 = new BigDecimal("0.2");
    public static final BigDecimal b0_1 = new BigDecimal("0.1");



    public static String p100(BigDecimal b) {
        return b.multiply(B100).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

}

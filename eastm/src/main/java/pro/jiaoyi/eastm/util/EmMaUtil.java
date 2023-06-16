package pro.jiaoyi.eastm.util;

import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmMaUtil {

    private static final Map<String, BigDecimal[]> EMPTY_MA_MAP = new HashMap<>(7);

    static {
        BigDecimal[] m0 = new BigDecimal[0];
        EMPTY_MA_MAP.put("ma5", m0);
        EMPTY_MA_MAP.put("ma10", m0);
        EMPTY_MA_MAP.put("ma20", m0);
        EMPTY_MA_MAP.put("ma30", m0);
        EMPTY_MA_MAP.put("ma60", m0);
        EMPTY_MA_MAP.put("ma120", m0);
        EMPTY_MA_MAP.put("ma250", m0);
    }

    public static Map<String, BigDecimal[]> ma(List<EmDailyK> list) {

        if (list == null || list.size() < 5) {
            return EMPTY_MA_MAP;
        }

        Map<String, BigDecimal[]> maMap = new HashMap<>();

        BigDecimal[] closeArr = list.stream().map(EmDailyK::getClose).toList().toArray(new BigDecimal[0]);
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
}

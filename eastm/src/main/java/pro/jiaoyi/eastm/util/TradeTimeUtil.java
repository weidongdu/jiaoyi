package pro.jiaoyi.eastm.util;

import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.flow.common.TradeTimeEnum;

import java.time.LocalDate;
import java.time.LocalTime;

public class TradeTimeUtil {

    public static boolean isTrading(){
        return isTradeDay() && isTradeTime().equals(TradeTimeEnum.TRADE);
    }
    public static boolean isTradeDay() {
        // 周六 周日 不是交易日
        int value = LocalDate.now().getDayOfWeek().getValue();
        if (value == 6 || value == 7) {
            return false;
        }

        // 节假日 不是交易日
        if (EmClient.MARKET_STOP_DAY.contains(LocalDate.now())) {
            return false;
        }

        return true;
    }

    public static TradeTimeEnum isTradeTime() {
        //9:30 - 11:30
        //13:00 - 15:00
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(9, 25))) {
            return TradeTimeEnum.PRE;
        }
        if (now.isAfter(LocalTime.of(15, 0))) {
            return TradeTimeEnum.POST;
        }
        if (now.isAfter(LocalTime.of(11, 30)) && now.isBefore(LocalTime.of(13, 0))) {
            return TradeTimeEnum.MID;
        }
        return TradeTimeEnum.TRADE;
    }


}

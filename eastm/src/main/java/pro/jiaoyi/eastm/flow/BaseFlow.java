package pro.jiaoyi.eastm.flow;

import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.flow.common.TradeTimeEnum;
import pro.jiaoyi.eastm.util.TradeTimeUtil;

import java.time.LocalDate;
import java.time.LocalTime;

public interface BaseFlow {

    int getNo();

    /**
     * 每天执行一次
     */
    void runByDay();

    /**
     * 手动执行, 忽略时间校验, 按当前时间执行
     */
    void run();

    default boolean isTradeDay() {
        return TradeTimeUtil.isTradeDay();
    }

    default TradeTimeEnum isTradeTime() {
        return TradeTimeUtil.isTradeTime();
    }

}

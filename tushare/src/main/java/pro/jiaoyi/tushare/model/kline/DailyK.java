package pro.jiaoyi.tushare.model.kline;

import lombok.Data;
import lombok.ToString;
import pro.jiaoyi.common.model.K;

import java.math.BigDecimal;

@Data
@ToString(callSuper = true)
public class DailyK extends K {

    //fields=[ts_code, trade_date, open, high, low, close, pre_close, change, pct_chg, vol, amount]
    //[605336.SH, 20230523, 17.11, 17.19, 16.71, 16.76, 17.11, -0.35, -2.0456, 22055.92, 37362.742]

    private String trade_date;
    private BigDecimal pre_close;
    private BigDecimal change;

}

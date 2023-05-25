package pro.jiaoyi.tradingview.model.chart;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TvK {
//    {
//      "low": 16.18,
//      "high": 16.99,
//      "open": 16.35,
//      "time": "2021-01-26",
//      "close": 16.5
//    },
    private BigDecimal low;
    private BigDecimal high;
    private BigDecimal open;
    private String time;
    private BigDecimal close;
}

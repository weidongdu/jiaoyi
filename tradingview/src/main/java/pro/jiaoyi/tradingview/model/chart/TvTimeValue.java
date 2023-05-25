package pro.jiaoyi.tradingview.model.chart;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TvTimeValue {
//   {
//      "time": "2021-02-01",
//      "value": 16.32
//    },
    private String time;
    private BigDecimal value;
}

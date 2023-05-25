package pro.jiaoyi.tradingview.model.chart;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TvVol {
//    {
//      "time": "2021-01-26",
//      "color": "rgba(255,82,82, 0.8)",
//      "value": 628149.932
//    }
    private String time;
    private String color;
    private BigDecimal value;
}

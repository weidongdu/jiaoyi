package pro.jiaoyi.eastm.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class EastSpeedInfo {
    private BigDecimal price_f2;//: 4.05,
    private BigDecimal pct_f3;//: 10.05,
    private String code_f12;//: "601003",
    private String name_f14;//: "柳钢股份",
    private BigDecimal speed_f22;//: 2.02
}

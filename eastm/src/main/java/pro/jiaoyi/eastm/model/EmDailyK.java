package pro.jiaoyi.eastm.model;

import lombok.Data;
import lombok.ToString;
import pro.jiaoyi.common.model.K;

import java.math.BigDecimal;

@Data
@ToString(callSuper = true)
public class EmDailyK extends K {

    private BigDecimal preClose;
    private BigDecimal pctChange;
    private BigDecimal osc;
    private BigDecimal hsl;
    private String tradeDate;

    private String bk;

}

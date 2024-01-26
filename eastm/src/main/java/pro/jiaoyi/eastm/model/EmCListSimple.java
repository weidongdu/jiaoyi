package pro.jiaoyi.eastm.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
public class EmCListSimple {

    private BigDecimal f6Amt;//成交额//        "f6": 364294854.36,
    private String f12Code;//代码//        "f12": "000001",
    private String f14Name;//名称//        "f14": "平安银行",
    private String tradeDate;//交易日

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmCListSimple emCList = (EmCListSimple) o;
        return Objects.equals(f12Code, emCList.f12Code) && Objects.equals(tradeDate, emCList.tradeDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(f12Code, tradeDate);
    }
}

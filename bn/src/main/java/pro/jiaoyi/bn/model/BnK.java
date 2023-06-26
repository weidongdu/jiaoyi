package pro.jiaoyi.bn.model;

import lombok.Data;
import lombok.ToString;
import pro.jiaoyi.common.model.K;

import java.math.BigDecimal;

@Data
@ToString(callSuper = true)
public class BnK extends K {

    //    308,                // 成交笔数
    //    "1756.87402397",    // 主动买入成交量
    //    "28.46694368",      // 主动买入成交额
    //    "17928899.62484339" // 请忽略该参数

    private Integer count;
    private BigDecimal buyVol;
    private BigDecimal buyAmt;

}

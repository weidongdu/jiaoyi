package pro.jiaoyi.bn.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/*
        {
            "symbol": "BTCUSDT",            // 交易对
            "markPrice": "11793.63104562",  // 标记价格
            "indexPrice": "11781.80495970", // 指数价格
            "estimatedSettlePrice": "11781.16138815",  // 预估结算价,仅在交割开始前最后一小时有意义
            "lastFundingRate": "0.00038246",    // 最近更新的资金费率
            "nextFundingTime": 1597392000000,   // 下次资金费时间
            "interestRate": "0.00010000",       // 标的资产基础利率
            "time": 1597370495002               // 更新时间
        }
 */
@Entity
@Table(name = "t_bn_order")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class BnOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private BigDecimal vol;
    private BigDecimal amt;
    private int side;//1 = buy , -1 = sell
    private int type;//1 = open , -1 = close
    private Long ts;


}

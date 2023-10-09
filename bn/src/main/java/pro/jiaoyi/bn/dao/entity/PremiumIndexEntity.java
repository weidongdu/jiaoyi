package pro.jiaoyi.bn.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

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
@Table(name = "t_premium_index")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class PremiumIndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;//"symbol": "FTMUSDT",
    private BigDecimal markPrice;//"markPrice": "0.19160000",
    private BigDecimal indexPrice;//"indexPrice": "0.19174377",
    private BigDecimal estimatedSettlePrice;//"estimatedSettlePrice": "0.19195370",
    private BigDecimal lastFundingRate;//"lastFundingRate": "0.00005228",
    private BigDecimal interestRate;//"interestRate": "0.00010000",
    private Long nextFundingTime;//"nextFundingTime": 1696838400000,
    private Long time;//"time": 1696813553000


}

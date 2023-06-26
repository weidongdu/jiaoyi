package pro.jiaoyi.bn.model.trade;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class AccountPosition {
    /*
    {
      "symbol": "BTCUSDT",
      "initialMargin": "58.22812155",
      "maintMargin": "2.32912486",
      "unrealizedProfit": "-70.17178440",
      "positionInitialMargin": "58.22812155",
      "openOrderInitialMargin": "0",
      "leverage": "10",
      "isolated": true,
      "entryPrice": "29656.95454545",
      "maxNotional": "30000000",
      "positionSide": "BOTH",
      "positionAmt": "0.022",
      "notional": "582.28121559",
      "isolatedWallet": "109.16813331",
      "updateTime": 1686268800185,
      "bidNotional": "0",
      "askNotional": "0"
    }
     */
    private String symbol;//: "BTCUSDT",
    private BigDecimal initialMargin;//: "1.90070500",// 当前所需起始保证金(基于最新标记价格)
    private BigDecimal maintMargin;//: "0.15205640",//维持保证金
    private BigDecimal unrealizedProfit;//: "-0.01310000",// 持仓未实现盈亏
    private BigDecimal positionInitialMargin;//: "1.90070500",// 持仓所需起始保证金(基于最新标记价格)
    private BigDecimal openOrderInitialMargin;//: "0",// 当前挂单所需起始保证金(基于最新标记价格)
    private BigDecimal leverage;//: "20", // 杠杆倍率
    private Boolean isolated;//: true,// 是否是逐仓模式
    private BigDecimal entryPrice;//: "38001.0",// 持仓成本价
    private BigDecimal maxNotional;//: "7500000",// 当前杠杆下用户可用的最大名义价值
    private String positionSide;//: "SHORT",// 持仓方向
    private BigDecimal positionAmt;//: "-0.001",// 持仓数量
    private BigDecimal notional;//: "-38.01410000",
    private BigDecimal isolatedWallet;//: "1.90055347",
    private Date updateTime;//: 1651403381208,
    private BigDecimal bidNotional;//: "0",
    private BigDecimal askNotional;//: "0"
}

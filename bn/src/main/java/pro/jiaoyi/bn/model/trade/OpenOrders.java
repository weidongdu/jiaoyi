package pro.jiaoyi.bn.model.trade;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/*

[
  {
    "avgPrice": "0.00000",              // 平均成交价
    "clientOrderId": "abc",             // 用户自定义的订单号
    "cumQuote": "0",                        // 成交金额
    "executedQty": "0",                 // 成交量
    "orderId": 1917641,                 // 系统订单号
    "origQty": "0.40",                  // 原始委托数量
    "origType": "TRAILING_STOP_MARKET", // 触发前订单类型
    "price": "0",                   // 委托价格
    "reduceOnly": false,                // 是否仅减仓
    "side": "BUY",                      // 买卖方向
    "positionSide": "SHORT", // 持仓方向
    "status": "NEW",                    // 订单状态
    "stopPrice": "9300",                    // 触发价，对`TRAILING_STOP_MARKET`无效
    "closePosition": false,   // 是否条件全平仓
    "symbol": "BTCUSDT",                // 交易对
    "time": 1579276756075,              // 订单时间
    "timeInForce": "GTC",               // 有效方法
    "type": "TRAILING_STOP_MARKET",     // 订单类型
    "activatePrice": "9020", // 跟踪止损激活价格, 仅`TRAILING_STOP_MARKET` 订单返回此字段
    "priceRate": "0.3", // 跟踪止损回调比例, 仅`TRAILING_STOP_MARKET` 订单返回此字段
    "updateTime": 1579276756075,        // 更新时间
    "workingType": "CONTRACT_PRICE", // 条件价格触发类型
    "priceProtect": false            // 是否开启条件单触发保护
  }
]

 */
@Data
public class OpenOrders {
    private BigDecimal orderId;//: 52144720529,
    private String symbol;//: "BTCUSDT",
    private String status;//: "NEW",
    private String clientOrderId;//: "android_BMCEPu2bwENklSpoAoZk",
    private BigDecimal price;//: "0",
    private BigDecimal avgPrice;//: "0",
    private BigDecimal origQty;//: "0.001",
    private BigDecimal executedQty;//: "0",
    private BigDecimal cumQuote;//: "0",
    private String timeInForce;//: "GTE_GTC",
    private String type;//: "STOP_MARKET",
    private Boolean reduceOnly;//: true,
    private Boolean closePosition;//: false,
    private String side;//: "BUY",
    private String positionSide;//: "SHORT",
    private BigDecimal stopPrice;//: "38100",
    private String workingType;//: "MARK_PRICE",
    private Boolean priceProtect;//: true,
    private String origType;//: "STOP_MARKET",
    private Date time;//: 1651403394139,
    private Date updateTime;//: 1651403394139

}

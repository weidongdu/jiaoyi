package pro.jiaoyi.bn.model;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class Ticker24hr {
    private String symbol; //"symbol":"BALUSDT",
    private BigDecimal priceChange; //"priceChange":"0.075", //24小时价格变动
    private BigDecimal priceChangePercent; //"priceChangePercent":"1.126", //24小时价格变动百分比
    private BigDecimal weightedAvgPrice; //"weightedAvgPrice":"6.751", //加权平均价
    private BigDecimal lastPrice; //"lastPrice":"6.735", //最近一次成交价
    private BigDecimal lastQty; //"lastQty":"7.5", //最近一次成交额
    private BigDecimal openPrice; //"openPrice":"6.660", //24小时内第一次成交的价格
    private BigDecimal highPrice; //"highPrice":"6.887", //24小时最高价
    private BigDecimal lowPrice; //"lowPrice":"6.591", //24小时最低价
    private BigDecimal volume; //"volume":"2422540.4", //24小时成交量
    private BigDecimal quoteVolume; //"quoteVolume":"16355713.110", //24小时成交额
    private Long openTime; //"openTime":1680497820000, //24小时内，第一笔交易的发生时间
    private Long closeTime; //"closeTime":1680584225298, //24小时内，最后一笔交易的发生时间
    private Long firstId; //"firstId":115774666, // 首笔成交id
    private Long lastId; //"lastId":115900170, // 末笔成交id
    private BigDecimal count; //"count":125505 // 成交笔数

}
package pro.jiaoyi.eastm.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmCList {

    private BigDecimal f2Close;//最新价//        "f2": 11.9,
    private BigDecimal f3Pct;//涨跌幅//        "f3": -0.92,
    private BigDecimal f4Chg;//涨跌额//        "f4": -0.11,
    private BigDecimal f5Vol;//成交量(手)//        "f5": 305362,
    private BigDecimal f6Amt;//成交额//        "f6": 364294854.36,
    private BigDecimal f7Amp;//振幅//        "f7": 0.92,
    private BigDecimal f8Turnover;//换手率//        "f8": 0.16,
    private BigDecimal f9Pe;//市盈率(动态)//        "f9": 3.95,
    private BigDecimal f10VolRatio;//量比//        "f10": 2.8,
    private String f12Code;//代码//        "f12": "000001",
    private String f14Name;//名称//        "f14": "平安银行",
    private BigDecimal f15High;//最高//        "f15": 12.0,
    private BigDecimal f16Low;//最低//        "f16": 11.89,
    private BigDecimal f17Open;//今开//        "f17": 11.99,
    private BigDecimal f18Close;//昨收//        "f18": 12.01,
    private BigDecimal f22Speed;//涨速//        "f22": -0.25,
    private BigDecimal f23Pb;//市净率//        "f23": 0.61
    private String f100bk;//所属板块//        "f100": "银行"
    private String tradeDate;//交易日

}

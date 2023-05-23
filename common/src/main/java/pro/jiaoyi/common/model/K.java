package pro.jiaoyi.common.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * K线数据 基础类
 */
@Data
public class K {
    /**
     * code
     */
    private String code;
    /**
     * name
     */
    private String name;
    /**
     * 开盘价
     */
    private BigDecimal open;
    /**
     * 收盘价
     */
    private BigDecimal close;
    /**
     * 最高价
     */
    private BigDecimal high;
    /**
     * 最低价
     */
    private BigDecimal low;
    /**
     * 涨跌幅
     */
    private BigDecimal pct;
    /**
     * 成交量
     */
    private BigDecimal vol;
    /**
     * 成交额
     */
    private BigDecimal amt;

    /**
     * 周期
     * 一分钟 MIN1,
     * 五分钟 MIN5,
     * 十五分钟 MIN15,
     * 三十分钟 MIN30,
     * 一小时 HOUR1,
     * 四小时 HOUR1,
     * 一天 DAY1,
     * 1周 WEEK1,
     * 1月 MONTH1
     */
    private String period;

    /**
     * 开盘时间
     * 默认就是交易日
     */
    private long tsOpen;

    /**
     * 收盘时间
     */
    private long tsClose;

}

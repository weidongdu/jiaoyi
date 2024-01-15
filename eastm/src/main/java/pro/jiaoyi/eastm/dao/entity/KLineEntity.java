package pro.jiaoyi.eastm.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "t_kline")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class KLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tradeDateStr;
    private LocalDate tradeDate;
    private String bk;

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
     * 换手率
     */
    private BigDecimal hsl;
    /**
     * 流通市值
     */
    private BigDecimal mv;


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
    private Long tsOpen;

    /**
     * 收盘时间
     */
    private Long tsClose;

    private BigDecimal ma5;
    private BigDecimal ma10;
    private BigDecimal ma20;
    private BigDecimal ma30;
    private BigDecimal ma60;
    private BigDecimal ma120;
    private BigDecimal ma250;

    private BigDecimal vma5;
    private BigDecimal vma10;
    private BigDecimal vma20;
    private BigDecimal vma30;
    private BigDecimal vma60;
    private BigDecimal vma120;
    private BigDecimal vma250;

    //成交量在 最近5天的位置 pct
    //0 -> 最近5天最小成交量
    //100% -> 最近5天最小成交量
    private BigDecimal vl5;
    private BigDecimal vl10;
    private BigDecimal vl20;
    private BigDecimal vl30;
    private BigDecimal vl60;
    private BigDecimal vl120;
    private BigDecimal vl250;

    /**
     * 涨跌幅5日
     */
    private BigDecimal pct5;
    private BigDecimal pct10;
    private BigDecimal pct20;
    private BigDecimal pct30;
    private BigDecimal pct60;
    private BigDecimal pct120;
    private BigDecimal pct250;

    /**
     * (最高- 最低)/ 最高 涨跌幅
     * 有前后关系
     */
    private BigDecimal maxHighLowPct5;
    private BigDecimal maxHighLowPct10;
    private BigDecimal maxHighLowPct20;
    private BigDecimal maxHighLowPct30;
    private BigDecimal maxHighLowPct60;
    private BigDecimal maxHighLowPct120;
    private BigDecimal maxHighLowPct250;


    /**
     * (最高 - 最低)/ 最低 涨跌幅
     * 有前后关系
     */
    private BigDecimal maxLowHighPct5;
    private BigDecimal maxLowHighPct10;
    private BigDecimal maxLowHighPct20;
    private BigDecimal maxLowHighPct30;
    private BigDecimal maxLowHighPct60;
    private BigDecimal maxLowHighPct120;
    private BigDecimal maxLowHighPct250;



}

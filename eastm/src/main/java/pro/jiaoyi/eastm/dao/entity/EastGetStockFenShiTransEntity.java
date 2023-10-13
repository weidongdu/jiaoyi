package pro.jiaoyi.eastm.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


/*
{
    "rc": 0,
    "rt": 108,
    "svr": 182481519,
    "lt": 2,
    "full": 0,
    "data": {
        "c": "300144",
        "m": 0,
        "n": "宋城演艺",
        "ct": 0,
        "cp": 15270,
        "tc": 4409,
        "data": [
            {
                "t": 150003,
                "p": 15630,
                "v": 5009,
                "bs": 1
            }
        ]
    }
}
*/

@Entity
@Table(name = "t_fenshi")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class EastGetStockFenShiTransEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;//: "300144",
    private int market;//: 0,
    private String name;//: "宋城演艺",
    private int ct;//: 0,
    private BigDecimal closePre;//: 15270,
    private int totalCount;//: 4409,

    @Column(columnDefinition = "text")
    private String data;


    private BigDecimal openPrice;//开盘价
    private BigDecimal openPct;//开盘价pct
    private BigDecimal openVol;//开盘量
    private String openVolStr;//开盘量
    private BigDecimal openAmt;//开盘额
    private String openAmtStr;//开盘额

    private BigDecimal hourAmt;
    private String hourAmtStr;
    private BigDecimal fAmtM1;//一分钟标准的成交额
    private String fAmtM1Str;//一分钟标准的成交额
    private BigDecimal fx;

    private BigDecimal close;//收盘价
    private BigDecimal pct;//涨跌幅
    private BigDecimal amt;//成交额
    private String amtStr;//成交额
    private BigDecimal hsl;//成交额
    private String bk;//成交额
    private BigDecimal holdPct;//close-open / open

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDate createDate;

}


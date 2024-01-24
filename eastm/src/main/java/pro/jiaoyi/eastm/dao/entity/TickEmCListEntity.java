package pro.jiaoyi.eastm.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_tick_market")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class TickEmCListEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    //全市场 绝对涨跌数量
    private int absUp;//pct
    private int absDn;//pct
    private int absZ;//pct

    //全市场 按实体涨跌数量
    private int openUp;//tick up
    private int openDn;//tick down
    private int openZ;//tick zero


    //tick级 涨跌数量
    private int tickUp;
    private int tickDn;
    private int tickZ;

    private int tick;// up - down
    private BigDecimal tickPct;// (up - down) / total

    private BigDecimal tickUpAmt;
    private BigDecimal tickDnAmt;
    private BigDecimal tickZAmt;

    private BigDecimal upAmtRatio;// up - down
    private BigDecimal dnAmtRatio;// (up - down) / total
    private BigDecimal zAmtRatio;// (up - down) / total

    //股票 up/dn ratio
    private BigDecimal sudr;
    //金额 up/dn ratio
    private BigDecimal audr;
    //net udr (stock - audr)
    private BigDecimal nudr;
    //trin
    private BigDecimal trin;//(tup/aup) / (tdn/adn)

    private LocalDateTime createTime;

}

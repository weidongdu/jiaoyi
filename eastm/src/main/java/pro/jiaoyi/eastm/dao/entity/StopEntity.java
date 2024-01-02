package pro.jiaoyi.eastm.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "t_stop_k")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class StopEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;
    private String tradeDate;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal pct;
    private BigDecimal amt;
    private BigDecimal stopCount;


    private BigDecimal preAmt;
    private BigDecimal preAmtRate;

    private BigDecimal postK1Open;
    private BigDecimal postK1Close;
    private BigDecimal postK1Amt;
    private String postK1TradeDate;

    private BigDecimal postK2Open;
    private BigDecimal postK2Close;
    private BigDecimal postK2Amt;
    private String postK2TradeDate;

    private BigDecimal postK3Open;
    private BigDecimal postK3Close;
    private BigDecimal postK3Amt;
    private String postK3TradeDate;

    private BigDecimal postK4Open;
    private BigDecimal postK4Close;
    private BigDecimal postK4Amt;
    private String postK4TradeDate;

    private BigDecimal postK5Open;
    private BigDecimal postK5Close;
    private BigDecimal postK5Amt;
    private String postK5TradeDate;

    private BigDecimal postK1OpenPct;
    private BigDecimal postK1ClosePct;

    private BigDecimal postK2OpenPct;
    private BigDecimal postK2ClosePct;

    private BigDecimal postK3OpenPct;
    private BigDecimal postK3ClosePct;

    private BigDecimal postK4OpenPct;
    private BigDecimal postK4ClosePct;

    private BigDecimal postK5OpenPct;
    private BigDecimal postK5ClosePct;

}

//package pro.jiaoyi.eastm.dao.entity;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import pro.jiaoyi.common.util.BDUtil;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "t_fenshi_simple")
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//@Data
//public class FenshiEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String code;
//    private String name;
//    private LocalDateTime fenshiTime;
//    private BigDecimal closePre;
//
//    private BigDecimal open;
//    private BigDecimal openPct;
//    private BigDecimal openVol;
//    private BigDecimal openAmt;
//
//    private BigDecimal hourAmt;
//    private BigDecimal fAmt;
//    //成交量放大倍数
//    private BigDecimal fx;
//
//    private BigDecimal close;
//    private BigDecimal pct;
//
//}

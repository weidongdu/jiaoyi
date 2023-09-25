//package pro.jiaoyi.bn.dao.entity;
//
//import jakarta.persistence.*;
//import lombok.Data;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "t_k_side_count")
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//@Data
//public class KSideCountEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private LocalDateTime ts;
//    private int side;
//    private Integer size;
//    private String period;//1m,5m,15m,30m,1h,4h,1d,1w,1M
//
//}

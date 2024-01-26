package pro.jiaoyi.eastm.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_fenshi_amt")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class EmCListSimpleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private BigDecimal f6Amt;//成交额//        "f6": 364294854.36,
    private String f12Code;//代码//        "f12": "000001",
    private LocalDateTime tradeDate;//交易日
}

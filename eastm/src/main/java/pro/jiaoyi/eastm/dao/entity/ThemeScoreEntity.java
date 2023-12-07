package pro.jiaoyi.eastm.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_theme_score")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class ThemeScoreEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String f1Theme;
    private BigDecimal f2Score;
    private BigDecimal f3Chg;
    private LocalDateTime createTime;

}

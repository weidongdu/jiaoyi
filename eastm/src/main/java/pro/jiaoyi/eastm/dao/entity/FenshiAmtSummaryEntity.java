package pro.jiaoyi.eastm.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_fenshi_amt_summary")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class FenshiAmtSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String f12code;
    private Long count;
    private String tradeDate;

}

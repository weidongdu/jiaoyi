package pro.jiaoyi.search.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_keywords_fail_to_search_entity")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class KeywordsFailToSearchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 三个 union 唯一
    private String source;//BAIDU
    private String keyword;//关键词
    private String masterKeyword;//主关键词

    private LocalDateTime createTime;//下拉框
    private LocalDateTime updateTime;//下拉框

}

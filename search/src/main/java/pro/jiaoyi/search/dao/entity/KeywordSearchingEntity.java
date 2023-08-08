package pro.jiaoyi.search.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_keyword_searching_entity")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class KeywordSearchingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;//BAIDU
    private String keyword;//关键词
    private String masterKeyword;//主关键词

    public KeywordSearchingEntity(String source, String keyword, String masterKeyword) {
        this.source = source;
        this.keyword = keyword;
        this.masterKeyword = masterKeyword;
    }
}

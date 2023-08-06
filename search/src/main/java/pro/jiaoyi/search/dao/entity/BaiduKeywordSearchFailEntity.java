package pro.jiaoyi.search.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_baidu_keyword_search_fail")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class BaiduKeywordSearchFailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyword;//关键词
    private LocalDateTime createTime;//下拉框
    private LocalDateTime updateTime;//下拉框

    public BaiduKeywordSearchFailEntity(){}

    public BaiduKeywordSearchFailEntity(String keyword){
        this.keyword = keyword;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
}

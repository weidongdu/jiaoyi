package pro.jiaoyi.search.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "t_keywords_wait_to_search")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class KeywordsWaitToSearchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer level;//0,1,2,3
    private String source;//BAIDU
    private String keyword;//关键词
    private String masterKeyword;//主关键词

    private LocalDateTime createTime;//下拉框
    private LocalDateTime updateTime;//下拉框

    private Integer searchCount;//搜索次数
    private Integer searchCountMax;//最大搜索次数

    public KeywordsWaitToSearchEntity(String source, String masterKeyword, String keyword, Integer level) {
        this.source = source;
        this.masterKeyword = masterKeyword;
        this.keyword = keyword;
        this.level = level;
        this.searchCount = 0;
        this.searchCountMax = 2;
    }

    public KeywordsWaitToSearchEntity() {

    }

    @PrePersist
    public void setDefaultTimeZonePrePersist() {
        this.createTime = LocalDateTime.now(ZoneOffset.ofHours(8));
        this.updateTime = LocalDateTime.now(ZoneOffset.ofHours(8));
    }

    @PreUpdate
    public void setDefaultTimeZonePreUpdate() {
        this.updateTime = LocalDateTime.now(ZoneOffset.ofHours(8));
    }

}

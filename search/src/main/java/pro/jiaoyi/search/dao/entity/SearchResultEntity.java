package pro.jiaoyi.search.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_search_result")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class SearchResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String master;//入口词
    private String keyword;//关键词
    private String source;//baidu zhihu
    private String plat;//pc mobile
    private String type;//dropdown related result


    @Column(length = 1024)
    private String title;

    @Column(length = 1024)
    private String content;

    @Column(length = 1024)
    private String url;

    @Column(length = 1024)
    private String realUrl;//真实url
    private Integer page;//搜索结果页数
    private Integer orderRank;//搜索结果排名

    @Column(length = 1024)
    private String keywordRelated;//相关搜索

    private LocalDateTime createTime;//下拉框
    private LocalDateTime updateTime;//下拉框

    @Column(length = 1024)
    private String remark;//备注
}

package pro.jiaoyi.search.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_zhihu_question_score")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class ZhihuQuestionScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime fetchTime;//加入待查询时间 从 search_result 导入
    private Integer searchCount;//查询次数
    private Integer pn;//查询次数
    private Integer orderRank;//查询次数

    private Integer ds;//运行天数
    private BigDecimal visitPerDay;//每天流量


    private String qid;//question id
    private String title;//关键词

    private LocalDateTime created;//创建时间 1357894226秒 *1000 变成时间戳ms
    private LocalDateTime updatedTime;//修改时间

    private Integer answerCount;// "answerCount": 21,
    private Integer visitCount;//   "visitCount": 917797,
    private Integer commentCount;//   "commentCount": 4,
    private Integer followerCount;//   "followerCount": 107,
    private Integer collapsedAnswerCount;//   "collapsedAnswerCount": 3,

    private String a1_name;
    private Integer a1_voteupCount;
    private LocalDateTime a1_createdTime;// "createdTime": 1558767139,
    private LocalDateTime a1_updatedTime;// "createdTime": 1558767139,

    private String a2_name;
    private Integer a2_voteupCount;
    private LocalDateTime a2_createdTime;// "createdTime": 1558767139,
    private LocalDateTime a2_updatedTime;// "createdTime": 1558767139,

    private String a3_name;
    private Integer a3_voteupCount;
    private LocalDateTime a3_createdTime;// "createdTime": 1558767139,
    private LocalDateTime a3_updatedTime;// "createdTime": 1558767139,

    private String a4_name;
    private Integer a4_voteupCount;
    private LocalDateTime a4_createdTime;// "createdTime": 1558767139,
    private LocalDateTime a4_updatedTime;// "createdTime": 1558767139,

    private String a5_name;
    private Integer a5_voteupCount;
    private LocalDateTime a5_createdTime;// "createdTime": 1558767139,
    private LocalDateTime a5_updatedTime;// "createdTime": 1558767139,


    public ZhihuQuestionScoreEntity() {

    }
}

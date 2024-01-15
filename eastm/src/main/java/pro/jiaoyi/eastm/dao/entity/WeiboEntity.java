package pro.jiaoyi.eastm.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_weibo")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class WeiboEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uid;//用户id
    private String content;//微博内容
    private String mid;//message id
    private LocalDateTime createTime;//创建时间


}

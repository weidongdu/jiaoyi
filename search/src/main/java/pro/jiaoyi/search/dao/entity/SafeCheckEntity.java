package pro.jiaoyi.search.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "t_safe_check")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public class SafeCheckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;//BAIDU
    private LocalDateTime createTime;//下拉框
    private LocalDateTime updateTime;//下拉框

    private String host;


    @PrePersist
    public void setDefaultTimeZonePrePersist() {
        this.createTime = LocalDateTime.now(ZoneOffset.ofHours(8));
        this.updateTime = LocalDateTime.now(ZoneOffset.ofHours(8));
    }

    @PreUpdate
    public void setDefaultTimeZonePreUpdate() {
        this.updateTime = LocalDateTime.now(ZoneOffset.ofHours(8));
    }

    public SafeCheckEntity() {
    }

    public SafeCheckEntity(String source) {
        this.source = source;
    }

}

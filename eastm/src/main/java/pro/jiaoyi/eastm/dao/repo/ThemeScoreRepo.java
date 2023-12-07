package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.OpenEmCListEntity;
import pro.jiaoyi.eastm.dao.entity.ThemeScoreEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ThemeScoreRepo extends JpaRepository<ThemeScoreEntity, Long>, CrudRepository<ThemeScoreEntity, Long> {
    List<ThemeScoreEntity> findAllByCreateTimeBetween(LocalDateTime localDateTime, LocalDateTime now);
}

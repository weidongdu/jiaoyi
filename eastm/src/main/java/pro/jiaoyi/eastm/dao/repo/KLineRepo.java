package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.KLineEntity;
import pro.jiaoyi.eastm.dao.entity.UserEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface KLineRepo extends JpaRepository<KLineEntity,Long>, CrudRepository<KLineEntity, Long> {

    int countByCode(String code);
    List<KLineEntity> findByTradeDate(LocalDate tradeDate);;
    List<KLineEntity> findByTsOpen(Long ts);
    List<KLineEntity> findByCode(String code);
}

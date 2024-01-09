package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.KLineEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface KLineRepo extends JpaRepository<KLineEntity,Long>, CrudRepository<KLineEntity, Long> {

    int countByCode(String code);
    List<KLineEntity> findByTradeDate(LocalDate tradeDate);;
    List<KLineEntity> findByTsOpen(Long ts);
    List<KLineEntity> findByCode(String code);

    @Query(nativeQuery = true, value = " SELECT * FROM t_kline t " +
            "WHERE t.code = ?1 " +
            "and t.ts_open > ?2 ")
    List<KLineEntity> findByCodeLimit5(String code,Long tsOpen);


    int deleteByCode(String code);
    int deleteByCodeIn(List<String> code);


    @Query(nativeQuery = true, value = " SELECT * FROM t_kline t " +
            "WHERE t.code = ?1 " +
            "ORDER BY t.id DESC " +
            "LIMIT 1 ")
    KLineEntity findByCodeLast(String code);
}

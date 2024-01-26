package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.EmCListSimpleEntity;

import java.time.LocalDateTime;

@Repository
public interface EmCListSimpleEntityRepo extends JpaRepository<EmCListSimpleEntity, Long>, CrudRepository<EmCListSimpleEntity, Long> {

    // 通过交易日期删除
    @Modifying
    @Query(nativeQuery = true, value = " DELETE FROM t_fenshi_amt t " +
            "WHERE t.trade_date < ?1 ")
    int deleteByTradeDateBefore(LocalDateTime tradeDate);


    // select count(*) from t_fenshi_amt t where t.f12code = '000063' and t.`trade_date` < '2024-01-27 00:00:00'
    @Query(nativeQuery = true, value = " select count(*) from t_fenshi_amt t " +
            "where t.f12code = ?2 and t.`trade_date` < ?1 ")
    int countByCode(LocalDateTime localDateTime, String code);
}

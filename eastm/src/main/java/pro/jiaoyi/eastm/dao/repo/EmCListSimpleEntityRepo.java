package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.EmCListSimpleEntity;

import java.time.LocalDateTime;
import java.util.List;

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

    //SELECT  t.* FROM  t_fenshi_amt t WHERE  t.`f12code` = '300418'  AND DATE(t.`trade_date`) = CURDATE() ORDER BY  t.`f6amt` DESC LIMIT 10;
    @Query(nativeQuery = true, value = " SELECT  t.* FROM  t_fenshi_amt t WHERE  t.`f12code` =  ?1  AND DATE(t.`trade_date`) = CURDATE() ORDER BY  t.`f6amt` DESC LIMIT 10; ")
    List<EmCListSimpleEntity> findByF12codeAndTradeDateOOrderByF6AmtDesc(String f12code);    //SELECT  t.* FROM  t_fenshi_amt t WHERE  t.`f12code` = '300418'  AND DATE(t.`trade_date`) = CURDATE() ORDER BY  t.`f6amt` DESC LIMIT 10;

    @Query(nativeQuery = true, value = " SELECT  t.* FROM  t_fenshi_amt t WHERE  t.`f12code` =  ?1  ORDER BY  t.`id` DESC LIMIT 10; ")
    List<EmCListSimpleEntity> findByF12codeOrderByIdDesc(String f12code);

}

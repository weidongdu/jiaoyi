package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.FenshiSimpleEntity;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FenshiSimpleRepo extends JpaRepository<FenshiSimpleEntity,Long>, CrudRepository<FenshiSimpleEntity, Long> {



    @Query(nativeQuery = true, value = " select count(*) from t_fenshi_simple t  " +
            " WHERE t.code = ?1 " +
            " and t.trade_date = ?2 ")
    int countByCodeAndTradeDate(String code, String td);

    FenshiSimpleEntity findByCodeAndTradeDate(String code, String td);



    @Query(nativeQuery = true, value = " select t.`code` from t_fenshi_simple t  " +
            " WHERE t.open_amt > ?1 ")
    List<String> findCodesByOpenAmt( BigDecimal openAmt);


    List<FenshiSimpleEntity> findByCode(String f12Code);
}

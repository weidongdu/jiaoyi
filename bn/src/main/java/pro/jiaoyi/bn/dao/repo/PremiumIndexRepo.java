package pro.jiaoyi.bn.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pro.jiaoyi.bn.dao.entity.PremiumIndexEntity;

import java.util.List;
import java.util.Set;

@Repository
public interface PremiumIndexRepo extends JpaRepository<PremiumIndexEntity,Long>, CrudRepository<PremiumIndexEntity, Long> {

    List<PremiumIndexEntity> findByTimeIn(Set<Long> timeSet);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = " DELETE FROM t_premium_index t where t.time < ?1 ")
    int deleteByDays(@Param("time") long time);


    @Query(nativeQuery = true, value = " select a.* from t_premium_index a " +
            " where a.`symbol` like '%USDT'" +
            " and a.time = (select b.time from t_premium_index b order by b.id desc limit 1) " +
            " order by a.last_funding_rate limit 300;")
    List<PremiumIndexEntity> findFeeList();
}

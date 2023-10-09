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
}

package pro.jiaoyi.bn.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pro.jiaoyi.bn.dao.entity.BnOrderEntity;
import pro.jiaoyi.bn.dao.entity.PremiumIndexEntity;

import java.util.List;
import java.util.Set;

@Repository
public interface BnOrderRepo extends JpaRepository<BnOrderEntity, Long>, CrudRepository<BnOrderEntity, Long> {


    @Query(nativeQuery = true, value = "select * from t_bn_order t where t.symbol = ?1 and t.ts > ?2 limit 1")
    BnOrderEntity findList(String symbol, Long ts);
}

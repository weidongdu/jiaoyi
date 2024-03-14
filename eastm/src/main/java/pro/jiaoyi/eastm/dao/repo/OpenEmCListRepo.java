package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.OpenEmCListEntity;

import java.util.List;

@Repository
public interface OpenEmCListRepo extends JpaRepository<OpenEmCListEntity, Long>, CrudRepository<OpenEmCListEntity, Long> {
    List<OpenEmCListEntity> findByTradeDate(String tdPre);

    List<OpenEmCListEntity> findByF12Code(String code);

    OpenEmCListEntity findByF12CodeAndTradeDate(String code, String t);

    @Query(value = "SELECT * FROM t_open_market WHERE f3pct > 0.5 AND f3pct < 5 AND f6amt > 10000000", nativeQuery = true)
    List<OpenEmCListEntity> findOpenHigh();
}

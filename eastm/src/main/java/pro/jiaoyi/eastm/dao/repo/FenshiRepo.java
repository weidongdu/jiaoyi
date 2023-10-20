package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.EastGetStockFenShiTransEntity;

import java.time.LocalDate;

@Repository
public interface FenshiRepo extends JpaRepository<EastGetStockFenShiTransEntity,Long>, CrudRepository<EastGetStockFenShiTransEntity, Long> {
    EastGetStockFenShiTransEntity findByCodeAndCreateDate(String code, LocalDate now);
}

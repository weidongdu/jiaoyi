package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.TickEmCListEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TickEmCListRepo extends JpaRepository<TickEmCListEntity, Long>, CrudRepository<TickEmCListEntity, Long> {

    List<TickEmCListEntity> findByCreateTimeAfter(LocalDateTime localDateTime);

    List<TickEmCListEntity> findByCreateTimeAfterAndMarketEquals(LocalDateTime localDateTime,String market);

}

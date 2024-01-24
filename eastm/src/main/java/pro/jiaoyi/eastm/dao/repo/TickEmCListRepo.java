package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.TickEmCListEntity;

@Repository
public interface TickEmCListRepo extends JpaRepository<TickEmCListEntity, Long>, CrudRepository<TickEmCListEntity, Long> {
}

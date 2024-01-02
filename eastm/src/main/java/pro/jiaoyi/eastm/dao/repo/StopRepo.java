package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.EmDailyKEntity;
import pro.jiaoyi.eastm.dao.entity.StopEntity;

import java.util.List;

@Repository
public interface StopRepo extends JpaRepository<StopEntity,Long>, CrudRepository<StopEntity, Long> {
}

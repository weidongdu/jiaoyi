package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.EmDailyKEntity;

import java.util.List;

@Repository
public interface EmDailyKRepo extends JpaRepository<EmDailyKEntity,Long>, CrudRepository<EmDailyKEntity, Long> {


    List<EmDailyKEntity> findByCode(String code);
}

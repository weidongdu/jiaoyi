package pro.jiaoyi.search.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.search.dao.entity.SafeCheckEntity;

@Repository
public interface SafeCheckRepo extends JpaRepository<SafeCheckEntity,Long>, CrudRepository<SafeCheckEntity, Long> {

    SafeCheckEntity findBySource(String name);
}

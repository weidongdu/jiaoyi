package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.UserEntity;

@Repository
public interface UserRepo extends JpaRepository<UserEntity,Long>, CrudRepository<UserEntity, Long> {
}
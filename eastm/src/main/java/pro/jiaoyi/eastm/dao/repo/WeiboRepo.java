package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.StopEntity;
import pro.jiaoyi.eastm.dao.entity.WeiboEntity;

import java.util.List;

@Repository
public interface WeiboRepo extends JpaRepository<WeiboEntity,Long>, CrudRepository<WeiboEntity, Long> {
    List<WeiboEntity> findByMidIn(List<String> midList);
}

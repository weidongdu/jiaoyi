package pro.jiaoyi.search.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.search.dao.entity.ZhihuQuestionEntity;

import java.util.List;

@Repository
public interface ZhihuQuestionRepo extends JpaRepository<ZhihuQuestionEntity, Long>, CrudRepository<ZhihuQuestionEntity, Long> {

    ZhihuQuestionEntity findByQid(String qid);
    List<ZhihuQuestionEntity> findByPnAndOrderRank(int pn, int order);
}

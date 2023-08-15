package pro.jiaoyi.search.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.search.dao.entity.ZhihuQuestionEntity;
import pro.jiaoyi.search.dao.entity.ZhihuQuestionScoreEntity;

import java.util.List;

@Repository
public interface ZhihuQuestionScoreRepo extends JpaRepository<ZhihuQuestionScoreEntity, Long>, CrudRepository<ZhihuQuestionScoreEntity, Long> {

}

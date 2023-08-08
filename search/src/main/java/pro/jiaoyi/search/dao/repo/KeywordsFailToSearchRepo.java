package pro.jiaoyi.search.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.search.dao.entity.BaiduKeywordSearchFailEntity;
import pro.jiaoyi.search.dao.entity.KeywordsFailToSearchEntity;

@Repository
public interface KeywordsFailToSearchRepo extends JpaRepository<KeywordsFailToSearchEntity,Long>, CrudRepository<KeywordsFailToSearchEntity, Long> {
    BaiduKeywordSearchFailEntity findByKeywordAndSourceAnd(String keyword);

}

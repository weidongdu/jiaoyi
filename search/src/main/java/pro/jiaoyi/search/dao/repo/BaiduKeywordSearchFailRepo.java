package pro.jiaoyi.search.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.search.dao.entity.BaiduKeywordSearchFailEntity;
import pro.jiaoyi.search.dao.entity.SearchResultEntity;

@Repository
public interface BaiduKeywordSearchFailRepo extends JpaRepository<BaiduKeywordSearchFailEntity,Long>, CrudRepository<BaiduKeywordSearchFailEntity, Long> {
    BaiduKeywordSearchFailEntity findByKeyword(String keyword);

}

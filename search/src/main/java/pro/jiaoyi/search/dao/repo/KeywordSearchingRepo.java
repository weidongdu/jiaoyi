package pro.jiaoyi.search.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.search.dao.entity.KeywordSearchingEntity;
import pro.jiaoyi.search.dao.entity.SearchResultEntity;

@Repository
public interface KeywordSearchingRepo extends JpaRepository<KeywordSearchingEntity,Long>, CrudRepository<KeywordSearchingEntity, Long> {
    KeywordSearchingEntity findBySourceAndMasterKeywordAndKeyword(String source,String masterKeyword,String keyword);
    int deleteBySourceAndMasterKeywordAndKeyword(String source,String masterKeyword,String keyword);
}

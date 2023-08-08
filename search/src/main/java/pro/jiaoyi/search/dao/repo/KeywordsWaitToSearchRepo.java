package pro.jiaoyi.search.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import pro.jiaoyi.search.dao.entity.KeywordsWaitToSearchEntity;

import java.util.List;

public interface KeywordsWaitToSearchRepo extends JpaRepository<KeywordsWaitToSearchEntity,Long>, CrudRepository<KeywordsWaitToSearchEntity, Long> {
    KeywordsWaitToSearchEntity findBySourceAndMasterKeywordAndKeyword(String source,String masterKeyword,String keyword);

    List<KeywordsWaitToSearchEntity> findBySource(String name);
    List<KeywordsWaitToSearchEntity> findBySourceAndSearchCountLessThanSearchCountMax(String name);
    KeywordsWaitToSearchEntity findFirstBySourceAndSearchCountLessThanSearchCountMax(String name);
}

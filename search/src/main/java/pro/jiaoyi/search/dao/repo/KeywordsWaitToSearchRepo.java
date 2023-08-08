package pro.jiaoyi.search.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import pro.jiaoyi.search.dao.entity.KeywordsWaitToSearchEntity;

import java.util.List;

public interface KeywordsWaitToSearchRepo extends JpaRepository<KeywordsWaitToSearchEntity, Long>, CrudRepository<KeywordsWaitToSearchEntity, Long> {
    KeywordsWaitToSearchEntity findBySourceAndMasterKeywordAndKeyword(String source, String masterKeyword, String keyword);

    List<KeywordsWaitToSearchEntity> findBySource(String name);

    @Query(nativeQuery = true, value = " SELECT * FROM t_keywords_wait_to_search t WHERE t.source = ?1 and t.search_count < t.search_count_max limit 10")
    List<KeywordsWaitToSearchEntity> qFindBySourceAndSearchCountLessThanSearchCountMax(String name);

    @Query(nativeQuery = true, value = " SELECT * FROM t_keywords_wait_to_search t WHERE t.source = ?1 and t.search_count < t.search_count_max limit 1")
    KeywordsWaitToSearchEntity qFindFirstBySourceAndSearchCountLessThanSearchCountMax(String name);
}

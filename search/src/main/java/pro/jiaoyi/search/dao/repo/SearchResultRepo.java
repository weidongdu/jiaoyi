package pro.jiaoyi.search.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.search.dao.entity.SearchResultEntity;

import java.util.List;

@Repository
public interface SearchResultRepo extends JpaRepository<SearchResultEntity, Long>, CrudRepository<SearchResultEntity, Long> {

    Integer countByKeywordAndSource(String keyword, String source);

//    @Query(nativeQuery = true, value = " select t.`id` ,t.`title`, t.`keyword`,t.`page`,t.`order_rank`,t.`real_url` , t.`create_time` from `t_search_result` t where t.domain= 'zhihu.com' and t.`real_url` like 'https://www.zhihu.com/question%' order by t.`id`")
    List<SearchResultEntity> findByDomainAndPageAndOrderRank(String domain,Integer page,Integer order);
}

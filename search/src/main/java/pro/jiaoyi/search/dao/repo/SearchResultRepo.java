package pro.jiaoyi.search.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.search.dao.entity.SearchResultEntity;

import java.util.List;

@Repository
public interface SearchResultRepo extends JpaRepository<SearchResultEntity,Long>, CrudRepository<SearchResultEntity, Long> {

    Integer countByKeywordAndSource(String keyword,String source);
}

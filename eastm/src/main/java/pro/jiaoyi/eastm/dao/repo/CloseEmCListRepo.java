package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.CloseEmCListEntity;

import java.util.List;

@Repository
public interface CloseEmCListRepo extends JpaRepository<CloseEmCListEntity, Long>, CrudRepository<CloseEmCListEntity, Long> {
}

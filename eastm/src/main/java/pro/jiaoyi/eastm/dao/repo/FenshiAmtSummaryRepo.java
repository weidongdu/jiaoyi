package pro.jiaoyi.eastm.dao.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pro.jiaoyi.eastm.dao.entity.FenshiAmtSummaryEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FenshiAmtSummaryRepo extends JpaRepository<FenshiAmtSummaryEntity, Long>, CrudRepository<FenshiAmtSummaryEntity, Long> {

    // 保存汇总数据
    FenshiAmtSummaryEntity save(FenshiAmtSummaryEntity summary);

    @Query(nativeQuery = true, value = "SELECT f12code, COUNT(*) AS count FROM t_fenshi_amt WHERE DATE(trade_date) = :tradeDate GROUP BY f12code")
    List<Object[]> executeNativeQuery(@Param("tradeDate") LocalDate tradeDate);

    @Query("SELECT f FROM FenshiAmtSummaryEntity f WHERE f.tradeDate = (SELECT MAX(f2.tradeDate) FROM FenshiAmtSummaryEntity f2)")
    List<FenshiAmtSummaryEntity> findByMaxTradeDate();

    @Query(nativeQuery = true, value =  "select * from t_fenshi_amt_summary t where t.`f12code` = ?1  order by id desc LIMIT ?2")
    List<FenshiAmtSummaryEntity> findRecentDataByCode(String code, int n);


    FenshiAmtSummaryEntity findByF12codeAndTradeDate(String f12code, String tradeDate);
}

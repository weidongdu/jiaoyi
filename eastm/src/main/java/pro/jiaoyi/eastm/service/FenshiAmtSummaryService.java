package pro.jiaoyi.eastm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.dao.entity.FenshiAmtSummaryEntity;
import pro.jiaoyi.eastm.dao.repo.FenshiAmtSummaryRepo;

import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
public class FenshiAmtSummaryService {

    private final FenshiAmtSummaryRepo fenshiAmtSummaryRepo;


    public FenshiAmtSummaryService(FenshiAmtSummaryRepo summaryRepository) {
        this.fenshiAmtSummaryRepo = summaryRepository;
    }

    public void executeSummary() {
        // 执行查询
        LocalDateTime now = LocalDateTime.now();
        String nowStr = DateUtil.ldtToStr(now, DateUtil.PATTERN_yyyyMMdd_HHmmss);
        log.info("Start to 执行当日 分时数据汇总 summary for date: {}", nowStr);
        List<Object[]> results = fenshiAmtSummaryRepo.executeNativeQuery(now.toLocalDate());
        // 保存结果到汇总表
        for (Object[] result : results) {
            String f12code = (String) result[0];
            Long count = (Long) result[1];

            // 保存到汇总表
            FenshiAmtSummaryEntity summary = new FenshiAmtSummaryEntity();
            summary.setF12code(f12code);
            summary.setCount(count);
            summary.setTradeDate(nowStr);
            try {
                fenshiAmtSummaryRepo.save(summary);
            } catch (Exception e) {
                // 处理异常，例如记录日志或抛出自定义异常
                log.error("Failed to save summary for f12code: {}", f12code, e);
            }
        }
    }
}

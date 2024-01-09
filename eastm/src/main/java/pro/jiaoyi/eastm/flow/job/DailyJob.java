package pro.jiaoyi.eastm.flow.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.eastm.flow.BaseFlow;

import java.util.Comparator;
import java.util.List;

//@Component
@Slf4j
public class DailyJob {
    private final List<BaseFlow> baseFlows;

    @Autowired
    public DailyJob(List<BaseFlow> baseFlows) {
        // 获取所的BaseFlow 的实现类 , 按 getNo() 排序, 依次执行 runByDay()
        this.baseFlows = baseFlows;
    }

    @Scheduled(fixedRate = 1000 * 60 * 60 * 24)
    public void execute() {
        log.info("DailyJob run");
        // 按照getNo()排序
        baseFlows.sort(Comparator.comparingInt(BaseFlow::getNo));
        // 依次执行runByDay()
        for (BaseFlow flow : baseFlows) {
//            flow.runByDay();
            flow.run();
            log.info("run {} done", flow.getClass().getSimpleName());
        }
    }
}

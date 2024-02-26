package pro.jiaoyi.eastm.flow;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.flow.common.CommonInfo;
import pro.jiaoyi.eastm.flow.common.FlowNo;
import pro.jiaoyi.eastm.flow.common.TypeEnum;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class StatsFlow implements BaseFlow {

    @Override
    public int getNo() {
        return FlowNo.STATS;
    }

    @Override
    public void runByDay() {

        run();
    }

    @Override
    public void run() {
        log.info("{} run {} , 统计数据", this.getClass().getSimpleName(), getNo());
        // ma5 之上的数量
    }
}

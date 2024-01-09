package pro.jiaoyi.eastm;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.eastm.flow.job.DailyJob;
import pro.jiaoyi.eastm.flow.KlineFlow;

@SpringBootTest
@Slf4j
class FlowTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private KlineFlow dataFlow;

//    @Test
//    public void run() {
//        dataFlow.run();
//    }

    @Resource
    private DailyJob dailyJob;

    @Test
    public void run() {
        dailyJob.execute();
    }


}

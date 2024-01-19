package pro.jiaoyi.eastm;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.flow.job.DailyJob;
import pro.jiaoyi.eastm.flow.KlineFlow;
import pro.jiaoyi.eastm.model.EmCList;

import java.util.List;

@SpringBootTest
@Slf4j
class FlowTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private EmClient emClient;
    @Test
    public void run() {
        List<EmCList> list = emClient.getClistDefaultSize(true);
        for (EmCList emCList : list) {
            log.info("v={} amt={} close={} pct={} code={}",
                    emCList.getF5Vol(), emCList.getF6Amt(), emCList.getF2Close(), emCList.getF3Pct(), emCList.getF12Code()+emCList.getF14Name());
        }
    }




}

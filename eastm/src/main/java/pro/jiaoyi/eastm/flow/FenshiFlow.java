package pro.jiaoyi.eastm.flow;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.flow.common.CommonInfo;
import pro.jiaoyi.eastm.flow.common.FlowNo;
import pro.jiaoyi.eastm.flow.common.TradeTimeEnum;

@Component
@Slf4j
public class FenshiFlow implements BaseFlow {

    @Override
    public int getNo() {
        return FlowNo.FENSHI;
    }

    @Resource
    private EmClient emClient;

    @Override
    public void runByDay() {

        if (!isTradeDay()) {
            log.info("not trade day");
            return;
        }

        if (!isTradeTime().equals(TradeTimeEnum.POST)) {
            log.info("not 盘后");
            return;
        }

        run();
    }

    @Override
    public void run() {
        log.info("{} run {} , 基于分时略选股", this.getClass().getSimpleName(), getNo());
        CommonInfo.CODE_K_MAP.forEach((code, k) -> {


        });
    }


}

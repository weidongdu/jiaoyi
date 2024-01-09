package pro.jiaoyi.eastm.flow;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.flow.common.CommonInfo;
import pro.jiaoyi.eastm.flow.common.FlowNo;
import pro.jiaoyi.eastm.flow.common.TradeTimeEnum;
import pro.jiaoyi.eastm.flow.common.TypeEnum;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RangeFlow implements BaseFlow {

    @Override
    public int getNo() {
        return FlowNo.RANGE;
    }

    @Resource
    private EmClient emClient;

    @Override
    public void runByDay() {

        run();
    }

    @Override
    public void run() {
        log.info("{} run {} , 基于区间策略选股", this.getClass().getSimpleName(), getNo());
        ArrayList<String> codes = new ArrayList<>();

        CommonInfo.CODE_K_MAP.forEach((code, k) -> {
            if (!CommonInfo.TYPE_CODES_MAP.get(TypeEnum.INDEX_INCLUDE.getType()).contains(code)) {
                return;
            }
            List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now(), 500, false);
            boolean r = emClient.rangePct(code, ks);
            if (r) codes.add(code);
        });
        CommonInfo.TYPE_CODES_MAP.put(TypeEnum.RANGE.getType(), codes);
    }
}

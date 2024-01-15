package pro.jiaoyi.eastm.flow;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.flow.common.FlowNo;
import pro.jiaoyi.eastm.flow.common.TradeTimeEnum;
import pro.jiaoyi.eastm.flow.common.CommonInfo;
import pro.jiaoyi.eastm.flow.common.TypeEnum;

import java.math.BigDecimal;
import java.util.ArrayList;

@Component
@Slf4j
public class AmtFlow implements BaseFlow {

    @Override
    public int getNo() {
        return FlowNo.AMT;
    }

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
        log.info("{} run {} , 基于成交量策略选股", this.getClass().getSimpleName(), getNo());
        ArrayList<String> codes = new ArrayList<>();

        CommonInfo.CODE_K_MAP.forEach((code, k) -> {
            if (CommonInfo.TYPE_CODES_MAP.get(TypeEnum.INDEX_INCLUDE.getType()).contains(code)) {
                if (k.getAmt().compareTo(BDUtil.B1_5Y) > 0
                        && k.getVma5().compareTo(BDUtil.B1_5Y) > 0
                        && k.getPct().compareTo(BigDecimal.ZERO) > 0) {

                    log.info("code {} , amt {} , vma5 {} , pct {} ",
                            code + k.getName(), BDUtil.amtHuman(k.getAmt()), BDUtil.amtHuman(k.getVma5()), BDUtil.p100(k.getPct()));
                    codes.add(code);
                }
            }
        });
        CommonInfo.TYPE_CODES_MAP.put(TypeEnum.AMT.getType(), codes);
    }


}

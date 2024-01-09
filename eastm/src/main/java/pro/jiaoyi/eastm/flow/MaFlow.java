package pro.jiaoyi.eastm.flow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.eastm.flow.common.CommonInfo;
import pro.jiaoyi.eastm.flow.common.FlowNo;
import pro.jiaoyi.eastm.flow.common.TradeTimeEnum;
import pro.jiaoyi.eastm.flow.common.TypeEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

@Component
@Slf4j
public class MaFlow implements BaseFlow {

    @Override
    public int getNo() {
        return FlowNo.MA;
    }

    @Override
    public void runByDay() {

        run();
    }

    @Override
    public void run() {
        log.info("{} run {} , 基于均线策略选股", this.getClass().getSimpleName(), getNo());
        ArrayList<String> codes = new ArrayList<>();

        CommonInfo.CODE_K_MAP.forEach((code, k) -> {
            if (!CommonInfo.TYPE_CODES_MAP.get(TypeEnum.INDEX_INCLUDE.getType()).contains(code)) {
                return;
            }

            if (k.getClose().compareTo(k.getMa5()) < 0
                    || k.getClose().compareTo(k.getMa10()) < 0
                    || k.getClose().compareTo(k.getMa20()) < 0
                    || k.getClose().compareTo(k.getMa30()) < 0
                    || k.getClose().compareTo(k.getMa60()) < 0) {
                log.info("不满足60 30 20 10 5 短均线之上");
                return;
            }

            if (k.getClose().compareTo(k.getMa120()) < 0) {
                BigDecimal diff = k.getMa120().subtract(k.getClose());
                BigDecimal diffPct = diff.divide(k.getClose(), 4, RoundingMode.HALF_UP);
                if (diffPct.compareTo(new BigDecimal("0.03")) > 0 && diffPct.compareTo(new BigDecimal("0.1")) < 0) {
                    log.info("ma 120 以下, 且 高度在3% 到 10% 之间");
                    return;
                }
            }

            if (k.getClose().compareTo(k.getMa250()) < 0) {
                BigDecimal diff = k.getMa250().subtract(k.getClose());
                BigDecimal diffPct = diff.divide(k.getClose(), 4, RoundingMode.HALF_UP);
                if (diffPct.compareTo(new BigDecimal("0.03")) > 0 && diffPct.compareTo(new BigDecimal("0.1")) < 0) {
                    log.info("ma 250 以下, 且 高度在3% 到 10% 之间");
                    return;
                }
            }
            codes.add(code);
        });
        CommonInfo.TYPE_CODES_MAP.put(TypeEnum.MA.getType(), codes);
    }
}

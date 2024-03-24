package pro.jiaoyi.eastm.flow;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.flow.common.CommonInfo;
import pro.jiaoyi.eastm.flow.common.FlowNo;
import pro.jiaoyi.eastm.flow.common.TypeEnum;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.time.LocalDate;
import java.util.*;

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
        //成交额在 1.5Y以上

        Map<String, List<String>> map = new HashMap<>();
        CommonInfo.CODE_K_MAP.forEach((code, k) -> {
            if (k.getAmt().compareTo(BDUtil.B1_5Y) > 0) {
                if (CommonInfo.TYPE_CODES_MAP.get(TypeEnum.EM_MA_UP.getType()).contains(code)
                        || CommonInfo.TYPE_CODES_MAP.get(TypeEnum.MA.getType()).contains(code)
                        || CommonInfo.TYPE_CODES_MAP.get(TypeEnum.RANGE.getType()).contains(code)
                        || CommonInfo.TYPE_CODES_MAP.get(TypeEnum.FENSHI_P.getType()).contains(code)) {
                    return;
                }
                map.computeIfAbsent(k.getBk(), k1 -> new ArrayList<>()).add(code);
            }
        });

        List<String> codes = new ArrayList<>();
        ArrayList<String> bks = new ArrayList<>(map.keySet());
        Collections.sort(bks);
        CommonInfo.TYPE_CODES_MAP.put(TypeEnum.B1_5Y.getType(), codes);

        bks.forEach(bk -> {
            List<String> codes1 = map.get(bk);
            codes.addAll(codes1);
        });
    }
}

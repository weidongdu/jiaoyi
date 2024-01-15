package pro.jiaoyi.eastm.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.model.fenshi.DetailTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiVo;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SpeedService {

    @Resource
    private EmRealTimeClient emRealTimeClient;

    private static final Cache<String, String> BLOCK_CODE_MAP = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();

    public Map<String, BigDecimal> getWindowAmt(String code, String name, Map<String, Integer> windowMap) {

        EastGetStockFenShiVo fEastGetStockFenShiVo = emRealTimeClient.getFenshiByCode(code);
        if (fEastGetStockFenShiVo == null) {
            log.info("fenshi is null: {}", name);
            BLOCK_CODE_MAP.put(code, "");
            return Collections.emptyMap();
        }


        EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(fEastGetStockFenShiVo);
        if (trans == null) {
            log.info("trans is null: {}", name);
            BLOCK_CODE_MAP.put(code, "");
            return Collections.emptyMap();
        }


        List<DetailTrans> DetailTransList = trans.getData();
        if (DetailTransList == null || DetailTransList.isEmpty()) {
            log.info("DetailTransList is null: {}", name);
            BLOCK_CODE_MAP.put(code, "");
            return Collections.emptyMap();
        }

        HashMap<String, BigDecimal> map = new HashMap<>();
        windowMap.forEach((m, i) -> {
            BigDecimal fenshiAmt = emRealTimeClient.getFenshiAmt(DetailTransList, i);
            map.put(m, fenshiAmt);
        });

        return map;
    }
}

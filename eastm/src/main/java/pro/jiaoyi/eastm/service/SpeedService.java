package pro.jiaoyi.eastm.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.dao.entity.EmCListSimpleEntity;
import pro.jiaoyi.eastm.dao.repo.EmCListSimpleEntityRepo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmCListSimple;
import pro.jiaoyi.eastm.model.fenshi.DetailTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiVo;
import pro.jiaoyi.eastm.util.TradeTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SpeedService {

    @Resource
    private EmRealTimeClient emRealTimeClient;

    @Resource
    private EmCListSimpleEntityRepo emCListSimpleEntityRepo;

    private static final List<EmCListSimple> CACHE_EM_SEQ_LIST = new ArrayList<>();

    public void addAll(List<EmCList> list) {
        for (EmCList emCList : list) {
            EmCListSimple emCListSimple = new EmCListSimple();
            emCListSimple.setF6Amt(emCList.getF6Amt());
            emCListSimple.setF12Code(emCList.getF12Code());
            emCListSimple.setF14Name(emCList.getF14Name());
            emCListSimple.setTradeDate(DateUtil.tsToStr(new Date().getTime(), DateUtil.PATTERN_yyyyMMdd_HHmmss));
            CACHE_EM_SEQ_LIST.add(emCListSimple);
        }
        //这里要记录下来, 用来做什么呢, 用来排除掉 突然暴量 但是平是成交都是100w以下的

        //清理 70s 前的数据
        CACHE_EM_SEQ_LIST.removeIf(emCListSimple -> {
            LocalDateTime localDateTime = DateUtil.strToLocalDateTime(emCListSimple.getTradeDate(), DateUtil.PATTERN_yyyyMMdd_HHmmss);
            return localDateTime.isBefore(LocalDateTime.now().minusSeconds(70));
        });
    }

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


    //获取最近60s 内的成交量
    public BigDecimal getFenshiAmtSimple(String code, int second) {
        //累计计算最近90s 内的list 成交量
        List<EmCListSimple> lastS = CACHE_EM_SEQ_LIST.stream().filter(em -> {
            String tradeDate = em.getTradeDate(); //此时为 格式 yyyyMMdd_HHmmss
            LocalDateTime localDateTime = DateUtil.strToLocalDateTime(tradeDate, DateUtil.PATTERN_yyyyMMdd_HHmmss);
            return em.getF12Code().equals(code) && localDateTime.isAfter(LocalDateTime.now().minusSeconds(second));
        }).toList();

        if (lastS.size() > 1) {
            EmCListSimple first = lastS.get(0);
            EmCListSimple last = lastS.get(lastS.size() - 1);
            return last.getF6Amt().subtract(first.getF6Amt());
        }
        return BigDecimal.ZERO;
    }

    //获取最近60s 内的成交量
    public BigDecimal getFenshiAmtSimple(String code) {
        //累计计算最近90s 内的list 成交量
        List<EmCListSimple> lastS = CACHE_EM_SEQ_LIST.stream().filter(em -> em.getF12Code().equals(code)).toList();

        if (lastS.size() > 1) {
            EmCListSimple first = lastS.get(0);
            EmCListSimple last = lastS.get(lastS.size() - 1);
            return last.getF6Amt().subtract(first.getF6Amt());
        }
        return BDUtil.BN1;
    }

    public void runFenshiM1(List<EmCList> list) {
        log.info("runFenshiM1 start");
        long s1 = System.currentTimeMillis();
        //save
        ArrayList<EmCListSimpleEntity> ll = new ArrayList<>();

        int saveCount = 0;
        for (EmCList emCList : list) {
            EmCListSimpleEntity em = new EmCListSimpleEntity();
            BigDecimal m1 = getFenshiAmtSimple(emCList.getF12Code());
            if (m1.compareTo(BDUtil.B100W) < 0 || m1.compareTo(BDUtil.BN1) == 0) {
                continue;
            }
            em.setF6Amt(m1);
            em.setF12Code(emCList.getF12Code());
            em.setTradeDate(LocalDateTime.now());
            ll.add(em);

            if (ll.size() > 100) {
                //save
                saveCount += ll.size();
                emCListSimpleEntityRepo.saveAll(ll);
                ll.clear();
            }
        }

        if (!ll.isEmpty()) {
            saveCount += ll.size();
            emCListSimpleEntityRepo.saveAll(ll);
        }
        log.info("runFenshiM1 , save {} end use [{}] ms", saveCount, System.currentTimeMillis() - s1);
    }

    @Transactional
    public void deleteFenshiM1Pre() {
        for (int i = 1; i < 30; i++) {
            LocalDate l = LocalDate.now().minusDays(i);
            if (TradeTimeUtil.isTradeDay(l)) {
                //删除 LocalDate l 之前的数据
                emCListSimpleEntityRepo.deleteByTradeDateBefore(l.atStartOfDay());
                break;
            }
        }
    }

    public int countByCode(String code, LocalDate l) {
        //删除 LocalDate l 之前的数据
        return emCListSimpleEntityRepo.countByCode(l.atStartOfDay(), code);
    }


}

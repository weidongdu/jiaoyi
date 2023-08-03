package pro.jiaoyi.eastm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.config.VipIndexEnum;
import pro.jiaoyi.eastm.dao.entity.KLineEntity;
import pro.jiaoyi.eastm.dao.repo.KLineRepo;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class EastStockService {
    @Autowired
    private EmClient emClient;
    @Autowired
    private KLineRepo kLineRepo;

    public List<String> getVl0() {

        List<EmDailyK> index = emClient.getDailyKs(VipIndexEnum.index_000001.getCode(), LocalDate.now(), 500, false);
        HashSet<String> codeSet = new HashSet<>();
        for (int i = index.size() - 10; i < index.size(); i++) {
            EmDailyK k = index.get(i);
            Long td = k.getTsOpen();

            List<KLineEntity> list = kLineRepo.findByTsOpen(td);
            //sort by field amt
            List<String> codes = list.stream().filter(item ->
                    item.getVl5().compareTo(BigDecimal.ZERO) == 0
                            && item.getVl10().compareTo(BigDecimal.ZERO) == 0
                            && item.getVl20().compareTo(BigDecimal.ZERO) == 0
                            && item.getVl30().compareTo(BigDecimal.ZERO) == 0
                            && item.getVl60().compareTo(BigDecimal.ZERO) == 0
                            && item.getVl120().compareTo(BigDecimal.ZERO) == 0
                            && item.getVl250().compareTo(BigDecimal.ZERO) == 0
            ).sorted(Comparator.comparing(KLineEntity::getAmt)).map(KLineEntity::getCode).toList();

            codeSet.addAll(codes);
        }

        ArrayList<String> codeList = new ArrayList<>();
        for (String code : codeSet) {
            List<KLineEntity> list = kLineRepo.findByCode(code);
            //获取 KLineEntity high 最高点
            BigDecimal max = list.stream().map(KLineEntity::getHigh).max(Comparator.comparing(BigDecimal::doubleValue)).get();
            //获取 KLineEntity low 最低点
            BigDecimal highX = max.divide(list.get(list.size() - 1).getClose(), 2, RoundingMode.HALF_UP);
            if (highX.compareTo(BDUtil.B4) > 0) {
                codeList.add(code);
            }
        }

        return codeList;
    }
}

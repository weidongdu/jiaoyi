package pro.jiaoyi.bn.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.jiaoyi.bn.dao.entity.PremiumIndexEntity;
import pro.jiaoyi.bn.dao.repo.PremiumIndexRepo;
import pro.jiaoyi.bn.sdk.FutureApi;

import java.util.HashSet;
import java.util.List;


@Component
@Slf4j
public class BnPremiumIndexService {

    @Resource
    private FutureApi futureApi;

    @Resource
    private PremiumIndexRepo premiumIndexRepo;

    @Scheduled(fixedDelay = 10 * 1000)
    @Async
    public void savePremiumIndex() {
        List<PremiumIndexEntity> list = futureApi.premiumIndex();
        List<PremiumIndexEntity> newList = removeDuplicate(list);
        log.info("newList size:{}", newList.size());
        if (newList.size() == 0) {
            return;
        }
        premiumIndexRepo.saveAll(newList);
    }

    private List<PremiumIndexEntity> removeDuplicate(List<PremiumIndexEntity> list) {
        List<Long> timeSet = list.stream().map(PremiumIndexEntity::getTime).toList();
        List<PremiumIndexEntity> listDb = premiumIndexRepo.findByTimeIn(new HashSet<>(timeSet));//.forEach(premiumIndexRepo::delete);
        log.info("listDb size:{}", listDb.size());
        return list.stream().filter(l -> {
            for (PremiumIndexEntity premiumIndexEntity : listDb) {
                if (premiumIndexEntity.getTime().equals(l.getTime())
                        && premiumIndexEntity.getSymbol().equals(l.getSymbol())) {
                    return false;
                }
            }
            return true;
        }).toList();
    }

    @Scheduled(cron = "0 0 * * * ?") // 每小时的整点触发
    public void deleteByDays() {
        deleteByDays(7);
    }

    public void deleteByDays(int days) {
        long now = System.currentTimeMillis();
        long start = now - days * 24 * 60 * 60 * 1000L;

        premiumIndexRepo.deleteByDays(start);
        log.info("deleteByDays:{}", days);
    }


}

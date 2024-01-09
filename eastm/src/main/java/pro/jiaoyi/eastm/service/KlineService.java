package pro.jiaoyi.eastm.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.jiaoyi.eastm.dao.entity.KLineEntity;
import pro.jiaoyi.eastm.dao.repo.KLineRepo;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class KlineService {

    @Resource
    private KLineRepo kLineRepo;

    @Transactional
    public void updateDB(String code, String name, List<KLineEntity> kLineEntities) {
        //根据code 删除
        int deleteSize = kLineRepo.deleteByCode(code);
        log.info("delete code={} size={}", code + name, deleteSize);
        //update db
        kLineRepo.saveAll(kLineEntities);
        log.info("saveAll code={} size={}", code + name, kLineEntities.size());
    }

    @Transactional
    public void updateDB(String code, String name, KLineEntity k) {
        //根据code 删除
        int deleteSize = kLineRepo.deleteByCode(code);
        log.info("delete code={} size={}", code + name, deleteSize);
        //update db
        kLineRepo.saveAndFlush(k);
        log.info("saveAndFlush code={}", code + name);
    }

    @Transactional
    public void updateDB(ArrayList<KLineEntity> kListToSave) {
        List<String> codes = new ArrayList<>(kListToSave.stream().map(KLineEntity::getCode).toList());
        kLineRepo.deleteByCodeIn(codes);
        kLineRepo.saveAll(kListToSave);
    }
}

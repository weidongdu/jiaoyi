package pro.jiaoyi.eastm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.dao.entity.WeiboEntity;
import pro.jiaoyi.eastm.dao.repo.WeiboRepo;

import java.util.List;

@Service
@Slf4j
public class WeiboService {
    @Autowired
    private WeiboRepo weiboRepo;
    @Autowired
    private WxUtil wxUtil;

    public void send(List<WeiboEntity> list) {
        List<String> mids = list.stream().map(WeiboEntity::getMid).toList();
        List<WeiboEntity> existList = weiboRepo.findByMidIn(mids);
        if (existList.isEmpty()) {
            weiboRepo.saveAll(list);
            //send
            for (WeiboEntity weiboEntity : existList) {
                wxUtil.send(weiboEntity.getContent());
            }
            return;
        }

        List<String> existMidList = existList.stream().map(WeiboEntity::getMid).toList();
        List<WeiboEntity> notExistList = list.stream().filter(item -> !existMidList.contains(item.getMid())).toList();
        weiboRepo.saveAll(notExistList);
        wxUtil.send("新增微博" + notExistList.size() + "条");
        for (WeiboEntity weiboEntity : existList) {
            wxUtil.send(weiboEntity.getContent());
        }
    }

}

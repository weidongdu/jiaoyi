package pro.jiaoyi.eastm.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.dao.entity.WeiboEntity;
import pro.jiaoyi.eastm.dao.repo.WeiboRepo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
            for (WeiboEntity weiboEntity : list) {
                log.info("wb {}", JSON.toJSONString(weiboEntity));
                String encode = URLEncoder.encode(weiboEntity.getContent(), StandardCharsets.UTF_8);
                wxUtil.send(encode);
            }
            return;
        }

        List<String> existMidList = existList.stream().map(WeiboEntity::getMid).toList();
        List<WeiboEntity> notExistList = list.stream().filter(item -> !existMidList.contains(item.getMid())).toList();
        weiboRepo.saveAll(notExistList);
        if (notExistList.size() == 0) {
            return;
        }

        for (WeiboEntity weiboEntity : notExistList) {
            log.info("wb {}", JSON.toJSONString(weiboEntity));
            String encode = URLEncoder.encode(weiboEntity.getContent(), StandardCharsets.UTF_8);
            wxUtil.send(encode);
        }
    }

}

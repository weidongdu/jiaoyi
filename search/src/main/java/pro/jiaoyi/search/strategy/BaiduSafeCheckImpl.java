package pro.jiaoyi.search.strategy;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;
import pro.jiaoyi.search.config.SourceEnum;
import pro.jiaoyi.search.dao.entity.SafeCheckEntity;
import pro.jiaoyi.search.dao.repo.SafeCheckRepo;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Component
public class BaiduSafeCheckImpl implements SafeCheck {

    public static final int STOP_TIME_MS = 1000 * 60 * 30;//1小时
    @Resource
    private SafeCheckRepo safeCheckRepo;

    @Override
    public Boolean safeCheck(WebDriver driver) {
        log.info("BaiduSafeCheckImpl");
        //1, 百度安全验证 title 可以识别
        //解决办法: 1,图片旋转, 识别  (目前解决不了)
        //解决办法: 2,停止, 更换ip , 保存当前状态 keyword + pn , 放入fail list;

        if (driver == null || driver.getTitle() == null) {
            return true;
        } else {
            if (driver.getTitle().contains("百度安全验证")) {
                log.info("百度安全验证 更换IP 或者暂停");
                String ipAddress = null;
                try {
                    InetAddress localhost = InetAddress.getLocalHost();
                    ipAddress = localhost.getHostAddress();
                } catch (UnknownHostException e) {
                    log.error("error", e);
                }

                SafeCheckEntity db = safeCheckRepo.findBySourceAndHost(SourceEnum.BAIDU.name(), ipAddress);
                if (db == null) {
                    SafeCheckEntity entity = new SafeCheckEntity(SourceEnum.BAIDU.name());
                    entity.setHost(ipAddress);
                    safeCheckRepo.save(entity);
                }
                return true;
            }
        }
        return false;
    }
}

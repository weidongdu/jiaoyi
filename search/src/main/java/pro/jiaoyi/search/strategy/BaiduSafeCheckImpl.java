package pro.jiaoyi.search.strategy;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BaiduSafeCheckImpl implements SafeCheck {
    @Override
    public Boolean safeCheck(WebDriver driver) {
        log.info("BaiduSafeCheckImpl");
        //1, 百度安全验证 title 可以识别
        //解决办法: 1,图片旋转, 识别  (目前解决不了)
        //解决办法: 2,停止, 更换ip , 保存当前状态 keyword + pn , 放入fail list;

        if (driver == null || driver.getTitle() == null || driver.getTitle().contains("百度安全验证")) {
            return true;
        }
        return false;
    }
}

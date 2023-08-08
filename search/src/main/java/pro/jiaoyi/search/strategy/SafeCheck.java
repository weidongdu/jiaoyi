package pro.jiaoyi.search.strategy;

import org.openqa.selenium.WebDriver;

/**
 * 反爬策略
 */
public interface SafeCheck {
    Boolean safeCheck(WebDriver driver);
}

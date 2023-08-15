package pro.jiaoyi.search.util;

import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import pro.jiaoyi.common.util.DateUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SeleniumUtil {

    //    public static final String WEB_DRIVER_PATH = "/home/ubuntu/github/run/chromedriver-linux64/chromedriver";
    public static final String WEB_DRIVER_PATH = "/Users/dwd/Downloads/search/chromedriver-mac-arm64/chromedriver";

    public static WebDriver getDriver(Boolean headless) {
        System.setProperty("webdriver.chrome.driver", WEB_DRIVER_PATH);
        ChromeOptions options = getChromeOptions(headless);
        return new ChromeDriver(options);
    }

    public static WebDriver getDriver(Proxy proxy, Boolean headless) {

        if (proxy == null) return getDriver(headless);
        System.setProperty("webdriver.chrome.driver", WEB_DRIVER_PATH);
        ChromeOptions options = getChromeOptions(headless);
        options.setCapability("proxy", proxy);
        return new ChromeDriver(options);
    }

    public static ChromeOptions getChromeOptions(Boolean headless) {
        ChromeOptions options = new ChromeOptions();

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateUtil.PATTERN_yyyyMMdd_HHmm));
        options.addArguments("user-data-dir=/Users/dwd/dev/GitHub/jiaoyi/search/tmp/user_profile" + "/" + time);
        options.addArguments("--remote-allow-origins=*");
        if (headless) options.addArguments("--headless");
        return options;
    }

}

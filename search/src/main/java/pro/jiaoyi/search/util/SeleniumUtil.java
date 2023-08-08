package pro.jiaoyi.search.util;

import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import pro.jiaoyi.common.util.DateUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SeleniumUtil {
    public static WebDriver getDriver() {

//        System.setProperty("webdriver.chrome.driver", "/home/dwd/dev/chromedriver-linux64/chromedriver");
        System.setProperty("webdriver.chrome.driver", "/Users/dwd/Downloads/search/chromedriver-mac-arm64/chromedriver");
        ChromeOptions options = getChromeOptions();
        return new ChromeDriver(options);
    }

    public static WebDriver getDriver(Proxy proxy) {

        if (proxy == null) return getDriver();

        System.setProperty("webdriver.chrome.driver", "/Users/dwd/Downloads/search/chromedriver-mac-arm64/chromedriver");
//        ChromeOptions options = new ChromeOptions();
//        Proxy proxy = new Proxy();
//        proxy.setHttpProxy("<HOST:PORT>");
        ChromeOptions options = getChromeOptions();
        options.setCapability("proxy", proxy);


        return new ChromeDriver(options);
    }

    public static ChromeOptions getChromeOptions(){
        ChromeOptions options = new ChromeOptions();

//        options.addArguments("--headless");
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateUtil.PATTERN_yyyyMMdd_HHmmss));
        options.addArguments("user-data-dir=/Users/dwd/Downloads/search/user_profile" + "/"+ time);
        options.addArguments("--remote-allow-origins=*");
        return options;
    }

}

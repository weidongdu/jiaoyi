package pro.jiaoyi.search.util;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class SeleniumUtil {
    public static WebDriver getDriver() {

        System.setProperty("webdriver.chrome.driver", "/Users/dwd/Downloads/search/chromedriver-mac-arm64/chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("user-data-dir=/Users/dwd/Downloads/search/user_profile");
        options.addArguments("--remote-allow-origins=*");
        //deviceName: "iPhone X"
//        options.addArguments();

        WebDriver driver = new ChromeDriver(options);
        return driver;
    }
}

package pro.jiaoyi.search.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RegUtil {
    public static void main1(String[] args) {
        String url = "https://www.baidu.com/s?ie=utf-8&f=8&rsv_bp=1&tn=baidu&wd=selenium%20%E6%96%B0%E5%BB%BA%E6%A0%87%E7%AD%BE%20&oq=selenium%2520%25E6%2596%25B0%25E5%25BB%25BA%25E6%25A0%2587%25E7%25AD%25BE%2520cookie&rsv_pq=a31d274a000837a3&rsv_t=e7e04FfhhPHsMhb98dbRP9xPr3w%2Frk1qVpbE6%2BAtLX0ijGIaRQOug57NOEY&rqlang=cn&rsv_dl=tb&rsv_enter=1&rsv_btype=t&inputT=442&rsv_sug3=5&rsv_sug1=5&rsv_sug7=100&rsv_sug2=0&rsv_sug4=903";
        String domain = domain(null);
        System.out.println(domain);
    }


    public static String domain(String url) {
//        String url = "https://www.baidu.com/s?ie=utf-8&f=8&rsv_bp=1&tn=baidu&wd=selenium%20%E6%96%B0%E5%BB%BA%E6%A0%87%E7%AD%BE%20&oq=selenium%2520%25E6%2596%25B0%25E5%25BB%25BA%25E6%25A0%2587%25E7%25AD%25BE%2520cookie&rsv_pq=a31d274a000837a3&rsv_t=e7e04FfhhPHsMhb98dbRP9xPr3w%2Frk1qVpbE6%2BAtLX0ijGIaRQOug57NOEY&rqlang=cn&rsv_dl=tb&rsv_enter=1&rsv_btype=t&inputT=442&rsv_sug3=5&rsv_sug1=5&rsv_sug7=100&rsv_sug2=0&rsv_sug4=903";
        if (!StringUtils.hasText(url)) {
            return null;
        }
        Pattern pattern = Pattern.compile("(?<=://)([^/]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String domain = matcher.group(1);
            // Remove www. prefix if present
            domain = domain.startsWith("www.") ? domain.substring(4) : domain;
            // Extract the first-level domain
            String[] parts = domain.split("\\.");
            if (parts.length > 1) {
                String topLevelDomain = parts[parts.length - 1];
                String secondLevelDomain = parts[parts.length - 2];
                String firstLevelDomain = secondLevelDomain + "." + topLevelDomain;
                log.debug("First-level domain: {}", firstLevelDomain);
                return firstLevelDomain;
            } else {
                log.debug("Invalid domain");
            }
        }
        return null;
    }


    public static String zhihuQuestionId(String urls) {
//        String urls = "https://zhuanlan.zhihu.com/p/610893185";
        String pattern = "https://www\\.zhihu\\.com/question/(\\d+)";

        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(urls);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}

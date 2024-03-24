package pro.jiaoyi.eastm.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.apache.bcel.classfile.Code;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class EmBaseInfoClient {

    @Autowired
    private OkHttpUtil okHttpUtil;
    public static final Cache<String, BigDecimal> CACHE_AMT = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.DAYS)
            .maximumSize(1000)
            .build();

    public Map<String, String> getHeader() {

        Map<String, String> header = new HashMap<>();
        header.put("Accept", "*/*");
        header.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        header.put("Cache-Control", "no-cache");
        header.put("Connection", "keep-alive");
        header.put("Origin", "https://emweb.securities.eastmoney.com");
        header.put("Pragma", "no-cache");
        header.put("Referer", "https://emweb.securities.eastmoney.com/");
        header.put("Sec-Fetch-Dest", "empty");
        header.put("Sec-Fetch-Mode", "cors");
        header.put("Sec-Fetch-Site", "same-site");
        header.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        header.put("sec-ch-ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"");
        header.put("sec-ch-ua-mobile", "?0");
        header.put("sec-ch-ua-platform", "\"macOS\"");

        return header;
    }

    /**
     * 主营范围
     */
    public String businessScope(String code) {

        code = marketCode(code);

        String url = ("https://datacenter.eastmoney.com/securities/api/data/v1/get" +
                "?reportName=RPT_HSF9_BASIC_ORGINFO" +
                "&columns=SECUCODE%2CSECURITY_CODE%2CBUSINESS_SCOPE" +
                "&quoteColumns=" +
//                "&filter=(SECUCODE%3D%22"+300024.SZ+"%22)" +
                "&filter=(SECUCODE%3D%22" + code + "%22)" +
                "&pageNumber=1" +
                "&pageSize=1" +
                "&sortTypes=" +
                "&sortColumns=" +
                "&source=HSF10" +
                "&client=PC" +
                "&v=05801539214834999");

        byte[] bytes = okHttpUtil.getForBytes(url, getHeader());
        return new String(bytes);

    }

    public String marketCode(String code) {

        if (code.startsWith("6")) {
            code = code + ".SH";
        }

        if (code.startsWith("0") || code.startsWith("3")) {
            code = code + ".SZ";
        }

        if (code.startsWith("8") || code.startsWith("4")) {
            code = code + ".BJ";
        }
        return code;
    }

    public String businessReview(String code) {

        String url = "https://datacenter.eastmoney.com/securities/api/data/v1/get" +
                "?reportName=RPT_F10_OP_BUSINESSANALYSIS" +
                "&columns=SECUCODE%2CSECURITY_CODE%2CREPORT_DATE%2CBUSINESS_REVIEW" +
                "&quoteColumns=" +
                "&filter=(SECUCODE%3D%22" + marketCode(code) + "%22)" +
                "&pageNumber=1" +
                "&pageSize=1" +
                "&sortTypes=" +
                "&sortColumns=" +
                "&source=HSF10" +
                "&client=PC" +
                "&v=06284490607335318";

        byte[] bytes = okHttpUtil.getForBytes(url, getHeader());
        return new String(bytes);
    }


}

package pro.jiaoyi.eastm.api;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.model.EastGetStockFenShiVo;
import pro.jiaoyi.eastm.model.EastSpeedInfo;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class EmRealTimeClient {

    @Autowired
    private OkHttpUtil okHttpUtil;

    private static final String BASE_URL = "http://12.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=%d&po=1&fltt=2&invt=2&fid=f22&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048&fields=f2,f3,f12,f14,f22";
    public static final String base = "http://push2ex.eastmoney.com/getStockFenShi";
    public static String ut = "7eea3edcaed734bea9cbfc24409ed989";
    public static String dpt = "wzfscj";
    private static final HashMap<String, String> hMap = new HashMap<String, String>();

    static {
        hMap.put("Accept", "*/*");
        hMap.put("Accept-Language", "zh-CN,zh;q=0.9");
        hMap.put("Cache-Control", "no-cache");
        hMap.put("Pragma", "no-cache");
        hMap.put("Proxy-Connection", "keep-alive");
        hMap.put("Referer", "http://quote.eastmoney.com/");
        hMap.put("Sec-GPC", "1");
        hMap.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.99 Safari/537.36");
    }

    public static boolean tradeTime() {
        LocalDateTime now = LocalDateTime.now();

        String amStart = LocalDate.now() + "09:30:00";
        String amEnd = LocalDate.now() + "11:30:00";

        String pmStart = LocalDate.now() + "13:00:00";
        String pmEnd = LocalDate.now() + "15:00:00";

        String p = DateUtil.PATTERN_yyyy_MM_dd + DateUtil.PATTERN_HH_mm_ss;
        if (now.isAfter(DateUtil.strToLocalDateTime(amStart, p))
                && now.isBefore(DateUtil.strToLocalDateTime(amEnd, p))) {
            return true;
        }

        if (now.isAfter(DateUtil.strToLocalDateTime(pmStart, p))
                && now.isBefore(DateUtil.strToLocalDateTime(pmEnd, p))) {
            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        System.out.println(tradeTime());
    }

    //获取涨速榜
    public List<EastSpeedInfo> getSpeedTop(int num) {
        byte[] bytes = okHttpUtil.getForBytes(String.format(BASE_URL, num), hMap);
        String jsonString = new String(bytes, StandardCharsets.UTF_8);

        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        JSONObject data = jsonObject.getJSONObject("data");
        JSONObject diff = data.getJSONObject("diff");
        ArrayList<EastSpeedInfo> list = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            JSONObject j = diff.getJSONObject(String.valueOf(i));
            EastSpeedInfo eastSpeedInfo = new EastSpeedInfo(j.getBigDecimal("f2"), j.getBigDecimal("f3"), j.getString("f12"), j.getString("f14"), j.getBigDecimal("f22"));

            if (eastSpeedInfo.getSpeed_f22().compareTo(BigDecimal.ONE) > 0
                    && eastSpeedInfo.getPct_f3().compareTo(BigDecimal.ZERO) > 0
                    && eastSpeedInfo.getPct_f3().compareTo(BigDecimal.ONE) < 0
                    && eastSpeedInfo.getPrice_f2().compareTo(BigDecimal.TEN) > 0
                    && !eastSpeedInfo.getName_f14().contains("ST")) {
                list.add(eastSpeedInfo);
            }
        }
        return list;
    }

    //获取分时成交
    public void getFenshiAmt(String code) {

    }

    //获取最近分时成交
    public void getFenshiAmtTotal(int lastMin) {

    }

    //获取最近60天 最大成交量 10% 的平均值
    public void daysAmt(String code, int days) {

    }

    //箱体突破判定
    public boolean tu(List<EmDailyK> dailyKs, int daysHigh, int boxDays, double boxDaysFactor) {
        if (dailyKs.size() < 120) return false;

        BigDecimal[] closeArr = dailyKs.stream().map(EmDailyK::getClose).toList().toArray(new BigDecimal[0]);
        BigDecimal[] ma5 = MaUtil.ma(5, closeArr, 3);
        BigDecimal[] ma10 = MaUtil.ma(10, closeArr, 3);
        BigDecimal[] ma20 = MaUtil.ma(20, closeArr, 3);
        BigDecimal[] ma30 = MaUtil.ma(30, closeArr, 3);
        BigDecimal[] ma60 = MaUtil.ma(60, closeArr, 3);

        int size = dailyKs.size();
        int index = size - 1;

        EmDailyK k = dailyKs.get(size - 1);

        if (k.getClose().compareTo(ma5[index]) > 0
                && k.getClose().compareTo(ma10[index]) > 0
                && k.getClose().compareTo(ma20[index]) > 0
                && k.getClose().compareTo(ma30[index]) > 0
                && k.getClose().compareTo(ma60[index]) > 0) {

            int count = 0;

            for (int j = 1; j < index - 1; j++) {
                if (index - j == 1) {
                    count = j;
                    break;
                }
                BigDecimal high = dailyKs.get(index - j).getHigh();
                if (high.compareTo(k.getClose()) >= 0) {
                    count = j;
                    break;
                }


            }
            log.info("over days high , count = {} high", count);

            int countBox = 0;//箱体计数
            for (int j = 1; j < count; j++) {
                //1, 高点超过高点
                //2, 低点低于高点
                int tmpIndex = index - j;
                BigDecimal high = dailyKs.get(tmpIndex).getHigh();
                if (k.getLow().compareTo(high) < 0 && k.getHigh().compareTo(high) > 0) {
                    countBox++;
                }
            }
            log.info("over box high , count = {} high", countBox);

            if (count > daysHigh && countBox > boxDays * boxDaysFactor) {
                log.error("满足条件箱体突破");
                return true;
            }
        }

        return false;
    }

    public String url(String code) {
        int market = 0;
        if (code.startsWith("6")) {
            market = 1;
        }

        String url = base
                + "?"
                + "ut=" + ut
                + "&dpt=" + dpt
                + "&pagesize=6000"
                + "&pageindex=0"
                + "&sort=1"
                + "&ft=1"
                + "&id=0"
                + "&code=" + code
                + "&market=" + market;

        return url;
    }

    public EastGetStockFenShiVo getByCode(String code) {

        try {
            byte[] bytes = okHttpUtil.getForBytes(url(code), hMap);
            String body = new String(bytes, Charset.defaultCharset());
            JSONObject data = JSONObject.parseObject(body).getJSONObject("data");
            log.info("body length={}", body.length());


            if (data != null) {
                EastGetStockFenShiVo vo = data.toJavaObject(EastGetStockFenShiVo.class);
                return vo;
            }

        } catch (Exception e) {
            log.warn("get fenshi exception {} {}", e.getMessage(), e);
        }

        return null;
    }
}
package pro.jiaoyi.eastm.api;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.strategy.BreakOutStrategy;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.model.EastSpeedInfo;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.model.fenshi.DetailTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiVo;

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
        //判断现在是不是星期六或者星期日
        if (LocalDate.now().getDayOfWeek().getValue() == 6 || LocalDate.now().getDayOfWeek().getValue() == 7) {
            return false;
        }

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
                    && eastSpeedInfo.getPct_f3().compareTo(new BigDecimal("2.5")) < 0
                    && !eastSpeedInfo.getName_f14().contains("ST")) {
                list.add(eastSpeedInfo);
            }
        }
        return list;
    }


    //箱体突破判定
    public boolean tu(List<EmDailyK> dailyKs, int daysHigh, int boxDays, double boxDaysFactor) {
        return BreakOutStrategy.breakOut(dailyKs,60,daysHigh,boxDays,boxDaysFactor);
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

    public EastGetStockFenShiVo getFenshiByCode(String code) {

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

    public BigDecimal getFenshiAmt(String code, int second) {
        EastGetStockFenShiVo f = this.getFenshiByCode(code);
        if (f == null) {
            return BigDecimal.ZERO;
        }

        EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(f);
        if (trans == null) {
            return BigDecimal.ZERO;
        }

        List<DetailTrans> list = trans.getData();
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }

        //累计计算最近90s 内的list 成交量
        long start = second * 1000L;
        List<DetailTrans> lastS = list.stream().filter(d -> d.getTs() >= (System.currentTimeMillis() - start)).toList();
        BigDecimal sum = BigDecimal.ZERO;
        for (DetailTrans detailTrans : lastS) {
            sum = sum.add(detailTrans.amt());
        }

        return sum;
    }


}

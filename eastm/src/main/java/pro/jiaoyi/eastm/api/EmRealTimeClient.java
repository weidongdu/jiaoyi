package pro.jiaoyi.eastm.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.strategy.BreakOutStrategy;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.model.EastSpeedInfo;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.model.fenshi.DetailTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiVo;
import pro.jiaoyi.eastm.util.EmMaUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

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

        String amStart = LocalDate.now() + "09:26:00";
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
        log.info("不在交易时间内");
        return false;
    }

    public static void main(String[] args) {
        System.out.println(tradeTime());
    }

    //获取涨速榜
    public List<EastSpeedInfo> getSpeedTop(int num, boolean filter) {
        byte[] bytes = okHttpUtil.getForBytes(String.format(BASE_URL, num), hMap);
        String jsonString = new String(bytes, StandardCharsets.UTF_8);

        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        JSONObject data = jsonObject.getJSONObject("data");
        JSONObject diff = data.getJSONObject("diff");
        ArrayList<EastSpeedInfo> list = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            JSONObject j = diff.getJSONObject(String.valueOf(i));
            EastSpeedInfo eastSpeedInfo = new EastSpeedInfo(j.getBigDecimal("f2"), j.getBigDecimal("f3"), j.getString("f12"), j.getString("f14"), j.getBigDecimal("f22"));

            if (filter) {
                if (eastSpeedInfo.getSpeed_f22().compareTo(BigDecimal.ONE) > 0 //涨速 > 1
                        && eastSpeedInfo.getPct_f3().compareTo(BigDecimal.ZERO) > 0 // 涨幅>0
                        && eastSpeedInfo.getPct_f3().compareTo(BDUtil.B3) <= 0 // 涨幅 <5
                        && eastSpeedInfo.getPrice_f2().compareTo(BDUtil.B50) < 0 //价格小于50
                        && !eastSpeedInfo.getName_f14().contains("ST")) {
                    list.add(eastSpeedInfo);
                }
            } else {
                list.add(eastSpeedInfo);
            }
        }
        log.info("top list {}", String.join(",", list.stream().map(EastSpeedInfo::getName_f14).toList()));
        return list;

    }

    public List<EastSpeedInfo> getSpeedTop(int num) {
        return getSpeedTop(num, true);
    }


    //箱体突破判定
//    public int tu(List<EmDailyK> dailyKs, int daysHigh, int boxDays, double boxDaysFactor) {
//        return BreakOutStrategy.breakOut(dailyKs, 60, daysHigh, boxDays, boxDaysFactor);
//    }

    public int tu(List<EmDailyK> dailyKs, int daysHigh, int boxDays, double boxDaysFactor) {
        return BreakOutStrategy.breakOut(dailyKs, 60, daysHigh, boxDays, boxDaysFactor);
    }

    public boolean tu_old(List<EmDailyK> dailyKs, int daysHigh, int boxDays, double boxDaysFactor) {
        if (dailyKs.size() < 60) return false;

        Map<String, BigDecimal[]> ma = EmMaUtil.ma(dailyKs);
        BigDecimal[] ma5 = ma.get("ma5");
        BigDecimal[] ma10 = ma.get("ma10");
        BigDecimal[] ma20 = ma.get("ma20");
        BigDecimal[] ma30 = ma.get("ma30");
        BigDecimal[] ma60 = ma.get("ma60");


        int size = dailyKs.size();
        int index = size - 1;

        EmDailyK k = dailyKs.get(size - 1);
        if (k.getClose().compareTo(k.getOpen()) < 0
                || k.getClose().compareTo(BigDecimal.valueOf(40)) > 0) {
            log.info("今日开盘价{} > 最新价{}, 不符合条件", k.getOpen(), k.getClose());
            return false;
        }
        if (k.getPct().compareTo(BigDecimal.ZERO) > 0
                && k.getClose().compareTo(ma5[index]) > 0
                && k.getClose().compareTo(ma10[index]) > 0
                && k.getClose().compareTo(ma20[index]) > 0
                && k.getClose().compareTo(ma30[index]) > 0
                && k.getClose().compareTo(ma60[index]) > 0) {

            int count = 0;
            for (int j = 1; j < index - 1; j++) {
                if (index - j == 1) {
                    log.info("遍历所有, 持续新高 {}天 {}", j, dailyKs.get(index - j));
                    count = j;
                    break;
                }
                BigDecimal high = dailyKs.get(index - j).getHigh();
                if (high.compareTo(k.getClose()) >= 0) {
                    log.info("打破新高截止, {}天 {}", j, dailyKs.get(index - j));
                    count = j;
                    break;
                }
            }
            log.info("over days high , count = {} high", count);

            ArrayList<BigDecimal> highList = new ArrayList<>();
            ArrayList<BigDecimal> lowList = new ArrayList<>();

            int countBox = 0;//箱体计数
            for (int j = 1; j < count; j++) {
                //1, 高点超过高点
                //2, 低点低于高点
                int tmpIndex = index - j;
                EmDailyK dk = dailyKs.get(tmpIndex);
                if (k.getLow().compareTo(dk.getHigh()) < 0 && k.getHigh().compareTo(dk.getHigh()) > 0) {
                    countBox++;
                }
                highList.add(dk.getHigh());
                lowList.add(dk.getLow());
            }

            log.info("over box high , count = {} high", countBox);

            if (count > daysHigh && countBox > boxDays * boxDaysFactor) {
                log.error("满足条件箱体突破 {}", k);
                return true;
            }

            if (count > daysHigh) {
                log.info("开始判断曲线");
                highList.add(k.getClose());
                Collections.sort(highList);
                int locationHigh = highList.indexOf(k.getClose());
                BigDecimal locationHighPct = BigDecimal.valueOf(locationHigh).divide(BigDecimal.valueOf(highList.size()), 3, RoundingMode.HALF_UP);
                log.info("最新价 location pct = {}", locationHighPct);
                if (locationHighPct.compareTo(new BigDecimal("0.9")) < 0) {
                    return false;
                }

                //获取lowList 最低价
                BigDecimal lowest = lowList.stream().min(BigDecimal::compareTo).get();
                log.info("最低价 = {}", lowest);
                int locationLow = lowList.indexOf(lowest);
                BigDecimal locationLowPct = BigDecimal.valueOf(locationLow).divide(BigDecimal.valueOf(lowList.size()), 3, RoundingMode.HALF_UP);
                if ((locationLowPct.compareTo(new BigDecimal("0.4")) < 0
                        || locationLowPct.compareTo(new BigDecimal("0.6")) > 0)) {
                    return false;
                }

                BigDecimal hh = highList.get(highList.size() - 1);
                if (lowest.compareTo(new BigDecimal("0.7").multiply(hh)) > 0) {
                    EmDailyK fk = dailyKs.get(size - count);
                    log.info("曲线成功{}k [{}] from {} to {}", count, fk.getName(), fk.getTradeDate() + "=" + fk.getClose(), k.getTradeDate() + "=" + k.getClose());
                    return true;
                }
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

    public EastGetStockFenShiVo getFenshiByCode(String code) {

        try {
            byte[] bytes = okHttpUtil.getForBytes(url(code), hMap);
            String body = new String(bytes, Charset.defaultCharset());
            JSONObject data = JSONObject.parseObject(body).getJSONObject("data");
            log.debug("body length={}", body.length());

            if (data != null) {
                EastGetStockFenShiVo vo = data.toJavaObject(EastGetStockFenShiVo.class);
                return vo;
            }

        } catch (Exception e) {
            log.warn("get fenshi exception {} {}", e.getMessage(), e);
        }

        return null;
    }

    public EastGetStockFenShiVo getFenshiByCodeFromLocal(String code, String path, boolean absPath) {

        try {
            String abs = "";
            if (absPath) {
                abs = path;
            } else {
                abs = path + "/" + code + ".txt";
            }
            String body = FileUtil.readFromFile(abs);
            JSONObject data = JSONObject.parseObject(body).getJSONObject("data");
            log.debug("body length={}", body.length());

            if (data != null) {
                EastGetStockFenShiVo vo = data.toJavaObject(EastGetStockFenShiVo.class);
                return vo;
            }

        } catch (Exception e) {
            log.warn("get fenshi exception {} {}", e.getMessage(), e);
        }

        return null;
    }

    public BigDecimal getFenshiAmt(List<DetailTrans> list, int second) {

        //累计计算最近90s 内的list 成交量
        long start = second * 1000L;
        List<DetailTrans> lastS = list.stream().filter(d -> d.getTs() >= (System.currentTimeMillis() - start)).toList();
        BigDecimal sum = BigDecimal.ZERO;
        for (DetailTrans detailTrans : lastS) {
            sum = sum.add(detailTrans.amt());
        }

        return sum;
    }

    /**
     * 开盘m1
     *
     * @param list
     * @return
     */
    public BigDecimal getFenshiAmtOpenM1(List<DetailTrans> list) {


        //累计计算最近90s 内的list 成交量
        LocalDateTime open0931 = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 31, 0));
        LocalDateTime open0925 = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 25, 0));
        // LocalDateTime 转timestamp
        long ts0931 = DateUtil.toTimestamp(open0931);
        long ts0925 = DateUtil.toTimestamp(open0925);


        List<DetailTrans> lastS = list.stream().filter(d -> d.getTs() >= ts0925 && d.getTs() < ts0931).toList();
        BigDecimal sum = BigDecimal.ZERO;
        for (DetailTrans detailTrans : lastS) {
            sum = sum.add(detailTrans.amt());
        }

        return sum;
    }

    /**
     * 集合竞价
     */
    public BigDecimal getFenshiAmtOpenM0(List<DetailTrans> list, int second) {

        //累计计算最近90s 内的list 成交量
        long start = second * 1000L;
        List<DetailTrans> lastS = list.stream().filter(d -> d.getTs() >= (System.currentTimeMillis() - start)).toList();
        BigDecimal sum = BigDecimal.ZERO;
        for (DetailTrans detailTrans : lastS) {
            sum = sum.add(detailTrans.amt());
        }

        return sum;
    }

    public BigDecimal getFenshiWindowsHigh(List<DetailTrans> list, int second) {

        //累计计算最近90s 内的list 成交量
        long start = second * 1000L;
        List<DetailTrans> lastS = list.stream().filter(d -> d.getTs() >= (System.currentTimeMillis() - start)).toList();
        BigDecimal high = BigDecimal.ZERO;
        for (DetailTrans detailTrans : lastS) {
            if (detailTrans.getPrice().compareTo(high) > 0) {
                high = detailTrans.getPrice();
            }
        }
        return high;
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


        return getFenshiAmt(list, second);
    }


}

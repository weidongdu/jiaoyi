package pro.jiaoyi.eastm.api;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.model.KPeriod;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.config.IndexEnum;
import pro.jiaoyi.eastm.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static pro.jiaoyi.eastm.util.ExcelUtil.simpleRead;

@Component
@Slf4j
public class EmClient {

    @Autowired
    private OkHttpUtil okHttpUtil;

    public static final BigDecimal B100 = new BigDecimal("100");
    public static final BigDecimal B1000 = new BigDecimal("1000");

    //获取日线行情数据
    public List<EmDailyK> getDailyKs(String code, LocalDate end, int lmt, boolean force) {
        if (code == null) return Collections.emptyList();

        if (code.startsWith("BK")) {
            code = "90." + code;
        }
        if (!force) {
            //查本地缓存
            String key = DateUtil.today() + "-" + code;
            List<EmDailyK> emDailyKS = DATE_KLINE_MAP.get(key);
            if (emDailyKS != null && emDailyKS.size() > 0) {
                log.info("hit local cache for code:{}", key);
                return emDailyKS;
            }
        }


        String secid = "0." + code;
        if (code.startsWith("6")) {
            secid = "1." + code;
        }

        String url = "http://push2his.eastmoney.com/api/qt/stock/kline/get?secid=" + secid
                + "&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6"
                + "&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61"
                + "&klt=101&fqt=1" + "&end=" + end.toString().replace("-", "") + "&lmt=" + lmt;

        //bk行情处理
        if (code.startsWith("90")) {
            url = "http://71.push2his.eastmoney.com/api/qt/stock/kline/get?secid=" + code
                    + "&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6" +
                    "&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61" +
                    "&klt=101&fqt=1&end=20500101&lmt=1000000";
        }


        byte[] bytes = okHttpUtil.getForBytes(url, headerMap);
        if (bytes.length > 0) {
            EmResult<EmDataKline> emResult = JSONObject.parseObject(new String(bytes), new TypeReference<>() {
            });
            EmDataKline data = emResult.getData();
            List<String> klines = data.getKlines();

            int size = klines.size();
            ArrayList<EmDailyK> list = new ArrayList<>(klines.size());
            for (int i = 0; i < size; i++) {
                String kline = klines.get(i);
                //              open    close   high    low     成交量(手)  金额(元)           振幅      涨跌幅     涨跌额     换手
                // "2022-07-13, 18.47,  18.64,  18.87,  18.21,  117882,     226722741.59,   3.59,   1.41,       0.26,   1.04",
                String[] ks = kline.split(",");
                String date = ks[0];
                String open = ks[1];
                String close = ks[2];
                String high = ks[3];
                String low = ks[4];
                String vol = ks[5];
                String amt = ks[6];
                String osc = ks[7];
                String pct = ks[8];
                String change = ks[9];
                String swap = ks[10];

                EmDailyK dk = new EmDailyK();
                dk.setCode(code);
                dk.setName(data.getName());
                dk.setOpen(new BigDecimal(open));
                dk.setClose(new BigDecimal(close));
                dk.setHigh(new BigDecimal(high));
                dk.setLow(new BigDecimal(low));
                dk.setVol(new BigDecimal(vol));
                dk.setAmt(new BigDecimal(amt));
                //针对板块处理
                if (code.startsWith("90")) {
                    dk.setBk(data.getName());
                }
                dk.setBk(getBkValueByStockCode(code));

                if (i == 0) {
                    //第一天的开盘价就是前收盘价 假定
                    dk.setPreClose(dk.getOpen());
                } else {
                    dk.setPreClose(list.get(i - 1).getClose());
                }
                dk.setOsc(new BigDecimal(osc));
                dk.setPct(new BigDecimal(pct));
                dk.setHsl(new BigDecimal(swap));
                dk.setPctChange(new BigDecimal(change));

                dk.setTradeDate(date.replace("-", ""));
                dk.setTsOpen(DateUtil.toTimestamp(DateUtil.strToLocalDate(date, DateUtil.PATTERN_yyyy_MM_dd)));
                dk.setTsClose(dk.getTsOpen());
                dk.setPeriod(KPeriod.D1.getP());
                list.add(dk);

            }
            if (list.size() == 0) {
                return Collections.emptyList();
            }

            String s1 = list.get(0).getTradeDate() + " " + list.get(0).getCode() + " " + list.get(0).getName() + " " + list.get(0).getPct();
            String s2 = list.get(size - 1).getTradeDate() + " " + list.get(size - 1).getCode() + " " + list.get(size - 1).getName() + " " + list.get(size - 1).getPct();
            log.info("获取日线行情数据 size={} start={} end={}", size, s1, s2);

            DATE_KLINE_MAP.put(DateUtil.today() + "-" + code, list);
            return list;
        }

        return Collections.emptyList();
    }


    /**
     * 获取当日市场全部股票代码以及数据
     *
     * @param page     默认1
     * @param pageSize 默认10000
     * @return
     */

    private List<EmCList> getClist(int page, int pageSize) {
        String url = "http://9.push2.eastmoney.com/api/qt/clist/get?pn=" + page + "&pz=" + pageSize + "&po=0&np=1&fltt=2&invt=2&wbp2u=6502094531899276|0|1|0|web&fid=f12&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048&fields=f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f14,f15,f16,f17,f18,f22,f23,f100";
        byte[] bytes = okHttpUtil.getForBytes(url, headerMap);
        if (bytes.length > 0) {
            return parseEmCLists(bytes);
        }
        return Collections.emptyList();
    }

    @NotNull
    private static List<EmCList> parseEmCLists(byte[] bytes) {
        EmResult<EmDataCList> emResult = JSONObject.parseObject(new String(bytes), new TypeReference<>() {
        });
        EmDataCList data = emResult.getData();
        List<JSONObject> diff = data.getDiff();
        List<EmCList> list = new ArrayList<>(diff.size());
        for (JSONObject f : diff) {
            EmCList cList = new EmCList();
            cList.setF2Close(f.getBigDecimal("f2"));
            cList.setF3Pct(f.getBigDecimal("f3"));
            cList.setF4Chg(f.getBigDecimal("f4"));
            cList.setF5Vol(f.getBigDecimal("f5"));
            cList.setF6Amt(f.getBigDecimal("f6"));
            cList.setF7Amp(f.getBigDecimal("f7"));
            cList.setF8Turnover(f.getBigDecimal("f8"));
            cList.setF9Pe(f.getBigDecimal("f9"));
            cList.setF10VolRatio(f.getBigDecimal("f10"));
            cList.setF12Code(f.getString("f12"));
            cList.setF14Name(f.getString("f14"));
            cList.setF15High(f.getBigDecimal("f15"));
            cList.setF16Low(f.getBigDecimal("f16"));
            cList.setF17Open(f.getBigDecimal("f17"));
            cList.setF18Close(f.getBigDecimal("f18"));
            cList.setF22Speed(f.getBigDecimal("f22"));
            cList.setF23Pb(f.getBigDecimal("f23"));
            cList.setF100bk(f.getString("f100"));
            list.add(cList);
        }

        log.info("获取当日市场全部股票代码以及数据 size={}", list.size());
        return list;
    }

    public List<EmCList> getClistDefaultSize(boolean force) {
        //从本地缓存先加载
        if (!force) {
            List<EmCList> list = DATE_LIST_MAP.get(DateUtil.today());
            if (list != null && list.size() > 0) {
                return list;
            }
        }
        List<EmCList> clist = getClist(1, 10000);
        if (clist.size() > 0) {
            DATE_LIST_MAP.put(DateUtil.today(), clist);
        }
        return clist;
    }

    public String getBkValueByStockCode(String code) {

        Map<String, String> map = DATE_STOCK_CODE_BK_MAP.get(DateUtil.today());
        if (map != null && map.size() > 0) {
            return map.get(code);
        }

        List<EmCList> list = getClistDefaultSize(false);
        HashMap<String, String> codeBk = new HashMap<>();
        for (EmCList emCList : list) {
            codeBk.put(emCList.getF12Code(), emCList.getF100bk());
        }
        DATE_STOCK_CODE_BK_MAP.put(DateUtil.today(), codeBk);
        return codeBk.get(code);
    }

    public void initBkMap(List<EmCList> list) {
        if (list.size() > 0) {
            BK_MAP.clear();
            for (EmCList bk : list) {
                BK_MAP.put(bk.getF12Code(), bk.getF14Name());
            }
        }
    }

    public String getBkValueByBkCode(String code) {
        return BK_MAP.get(code);
    }

    public String getBkCodeByBkValue(String value) {
        String code = "";
        for (String bkCode : BK_MAP.keySet()) {
            if (BK_MAP.get(bkCode).equals(value)) {
                code = bkCode;
                break;
            }
        }
        return code;
    }

    /**
     * code name map
     *
     * @param force 是否强制刷新, true 则不使用缓存, false 则使用缓存
     * @return
     */
    public Map<String, String> getCodeNameMap(boolean force) {
        //先拿到当天的缓存
        String key = DateUtil.today();
        if (!force) {
            Map<String, String> cnMap = DATE_CODE_NAME_MAP.get(key);
            if (cnMap != null && cnMap.size() > 0) {
                return cnMap;
            }
        }

        List<EmCList> clistDefault = getClistDefaultSize(force);
        Map<String, String> cnMap = new HashMap<>();
        for (EmCList cList : clistDefault) {
            cnMap.put(cList.getF12Code(), cList.getF14Name());
        }

        DATE_CODE_NAME_MAP.put(key, cnMap);
        return cnMap;
    }

    /**
     * name code map
     *
     * @param force
     * @return
     */
    public Map<String, String> getNameCodeMap(boolean force) {
        //先拿到当天的缓存
        String key = DateUtil.today();
        if (!force) {
            Map<String, String> ncMap = DATE_NAME_CODE_MAP.get(key);
            if (ncMap != null && ncMap.size() > 0) {
                return ncMap;
            }
        }
        List<EmCList> clistDefault = getClistDefaultSize(force);
        Map<String, String> ncMap = new HashMap<>();
        for (EmCList cList : clistDefault) {
            ncMap.put(cList.getF14Name(), cList.getF12Code());
        }

        DATE_CODE_NAME_MAP.put(key, ncMap);
        return ncMap;
    }

    /**
     * 获取成分股 沪深300
     *
     * @return
     */
    public List<EmCList> getIndex(IndexEnum type) {
        switch (type) {

            case ALL:
                return getClistDefaultSize(false);
            case HS300:
                return getIndex(IndexEnum.HS300.getUrl());
            case CYCF:
                return getIndex(IndexEnum.CYCF.getUrl());
            case ZZ500:
                return getIndex(IndexEnum.ZZ500.getUrl());
            case ZZ1000:
                return getIndex1000();
            case IndexAll:
                List<EmCList> index = getIndex(IndexEnum.CYCF.getUrl());
                index.addAll(getIndex(IndexEnum.HS300.getUrl()));
                index.addAll(getIndex(IndexEnum.ZZ500.getUrl()));
                index.addAll(getIndex1000());
                return index;
            case O_TP7:
                return getIndexTp7();
            case O_TP02:
//                return getIndexTp02();
                return Collections.emptyList();

            case O_BK:
                List<EmCList> bkList = getIndex(IndexEnum.O_BK.getUrl());
                initBkMap(bkList);
                return bkList;

            case O_TAMT60:
            default:
                return Collections.emptyList();
        }
    }

    private List<EmCList> getIndexTp02() {
        List<EmCList> list = getClistDefaultSize(false);

        HashSet<String> filterSet = new HashSet<>();
        filterSet.addAll(getIndex1000().stream().map(EmCList::getF12Code).toList());
        filterSet.addAll(getIndex(IndexEnum.ZZ500.getUrl()).stream().map(EmCList::getF12Code).toList());
        filterSet.addAll(getIndex(IndexEnum.HS300.getUrl()).stream().map(EmCList::getF12Code).toList());
        filterSet.addAll(getIndex(IndexEnum.CYCF.getUrl()).stream().map(EmCList::getF12Code).toList());

        BigDecimal B_2 = new BigDecimal("-2");
        BigDecimal B7 = new BigDecimal("7");
        BigDecimal B5000_0000 = new BigDecimal("50000000");
        List<EmCList> filterList = list.stream()
                .filter(c -> c.getF3Pct().compareTo(B_2) > 0
                        && c.getF3Pct().compareTo(B7) < 0
                        && !c.getF14Name().contains("退")
                        //成交额大于 5000万
                        && c.getF6Amt().compareTo(B5000_0000) > 0
                        && !filterSet.contains(c.getF12Code())
                )
                .collect(Collectors.toList());
        List<EmCList> emCLists = null;
        try {
            emCLists = filterIndexTp02(filterList);
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }
        return (emCLists);

    }

    public List<EmCList> filterIndexTp02(List<EmCList> list) throws InterruptedException {
        ArrayList<EmCList> filterList = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        for (EmCList emCList : list) {

            if (count.addAndGet(1) % 5 == 0) {
                log.info("run {} / {}", count.get(), list.size());
                Thread.sleep(1000);
            }

            List<EmDailyK> ks = getDailyKs(emCList.getF12Code(), LocalDate.now(), 500, false);
            if (ks.size() < 120) {
                continue;
            }

            EmDailyK k = ks.get(ks.size() - 1);
            //过滤 均线之上
            BigDecimal[] priceArr = ks.stream().map(EmDailyK::getClose).toList().toArray(new BigDecimal[0]);

            BigDecimal[] ma5 = MaUtil.ma(5, priceArr, 3);
            BigDecimal[] ma10 = MaUtil.ma(10, priceArr, 3);
            BigDecimal[] ma20 = MaUtil.ma(20, priceArr, 3);
            BigDecimal[] ma30 = MaUtil.ma(30, priceArr, 3);
            BigDecimal[] ma60 = MaUtil.ma(60, priceArr, 3);

            if (k.getClose().compareTo(ma5[ma5.length - 1]) <= 0
                    || k.getClose().compareTo(ma10[ma10.length - 1]) <= 0
                    || k.getClose().compareTo(ma20[ma20.length - 1]) <= 0
                    || k.getClose().compareTo(ma30[ma30.length - 1]) <= 0
                    || k.getClose().compareTo(ma60[ma60.length - 1]) <= 0) {

            } else {
                filterList.add(emCList);
            }
        }
        return filterList;
    }

    private List<EmCList> getIndexTp7() {
        List<EmCList> list = getClistDefaultSize(false);
        BigDecimal B = new BigDecimal("7");
        return list.stream().filter(c -> c.getF3Pct().compareTo(B) > 0).collect(Collectors.toList());
    }


    private List<EmCList> getIndex(String url) {
        byte[] bytes = okHttpUtil.getForBytes(url, headerMap);
        if (bytes.length > 0) {
            return parseEmCLists(bytes);
        }
        return Collections.emptyList();
    }

    /**
     * 中证1000 从文件获取
     * 官网
     * <a href="https://www.csindex.com.cn/zh-CN/indices/index-detail/000852#/indices/family/detail?indexCode=000852">...</a>
     * 文件
     * <a href="https://csi-web-dev.oss-cn-shanghai-finance-1-pub.aliyuncs.com/static/html/csindex/public/uploads/file/autofile/cons/000852cons.xls">...</a>
     *
     * @return
     */
    private List<EmCList> getIndex1000() {
//        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateUtil.PATTERN_yyyyMMdd_HHmm));
        String filePath = "zz1000_" + DateUtil.today() + ".xls";
        if (okHttpUtil.downloadFile(IndexEnum.ZZ1000.getUrl(), null, filePath)) {
            List<EmCList> list = new ArrayList<>();
            simpleRead(filePath, list, getClistDefaultSize(false));
            return list;
        }
        return Collections.emptyList();
    }


//    private static void removeOldCache(Map map, int daysBefore) {
//        ArrayList<String> oldList = new ArrayList<>();
//        //移除前面15天的缓存
//        if (map.size() > daysBefore * 2) {
//            for (Object s : map.keySet()) {
//                LocalDate localDate = DateUtil.strToLocalDate((String) s, DateUtil.PATTERN_yyyyMMdd);
//                if (localDate.isBefore(LocalDate.now().minusDays(daysBefore))) {
//                    oldList.add((String) s);
//                }
//            }
//            if (oldList.size() > 0) {
//                for (String s : oldList) {
//                    map.remove(s);
//                }
//            }
//        }
//    }


    /**
     * 60日 成交额 前10% 的平均值(日成交额)
     *
     * @param dailyKs
     * @return
     */
    public BigDecimal amtTop10p(List<EmDailyK> dailyKs) {
        ArrayList<BigDecimal> amts = new ArrayList<>();
        int size = dailyKs.size();
        for (int i = 1; i <= 60; i++) {
            EmDailyK k = dailyKs.get(size - 1 - i);
            amts.add(k.getAmt());
        }
        Collections.sort(amts);

        int avg = 6;
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < avg; i++) {
            BigDecimal amt = amts.get(amts.size() - 1 - i);
            total = total.add(amt);
        }
        return total.divide(BigDecimal.valueOf(avg), 0, RoundingMode.HALF_UP);
    }

    public static final Map<String, String> headerMap = new HashMap<>();

    static {
        headerMap.put("Accept", "*/*");
        headerMap.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headerMap.put("Cache-Control", "no-cache");
        headerMap.put("Connection", "keep-alive");
        headerMap.put("Pragma", "no-cache");
        headerMap.put("Referer", "http://quote.eastmoney.com/");
        headerMap.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36");
    }

    public static final Map<String, Map<String, String>> DATE_CODE_NAME_MAP = new ConcurrentHashMap<>();
    public static final Map<String, Map<String, String>> DATE_NAME_CODE_MAP = new ConcurrentHashMap<>();
    public static final Map<String, Map<String, String>> DATE_STOCK_CODE_BK_MAP = new ConcurrentHashMap<>();
    public static final Map<String, String> BK_MAP = new ConcurrentHashMap<>();
    public static final Map<String, List<EmCList>> DATE_LIST_MAP = new ConcurrentHashMap<>();
    public static final Map<String, List<EmDailyK>> DATE_KLINE_MAP = new ConcurrentHashMap<>();



}

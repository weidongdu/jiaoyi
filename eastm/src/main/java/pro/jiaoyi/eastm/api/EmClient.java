package pro.jiaoyi.eastm.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.model.KPeriod;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.config.IndexEnum;
import pro.jiaoyi.eastm.model.*;
import pro.jiaoyi.eastm.util.EmMaUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static pro.jiaoyi.common.util.BDUtil.*;
import static pro.jiaoyi.eastm.util.ExcelUtil.simpleRead;

@Component
@Slf4j
public class EmClient {

    public static AtomicInteger COUNT = new AtomicInteger(0);

    @Autowired
    private OkHttpUtil okHttpUtil;

    @Value("${project.dir}")
    private String projectDir;

    public static final BigDecimal B100 = new BigDecimal("100");
    public static final BigDecimal B1000 = new BigDecimal("1000");

    //获取日线行情数据
    public List<EmDailyK> getDailyKs(String code, LocalDate end, int lmt, boolean force) {
        if (code == null) {
            return Collections.emptyList();
        }

        if (code.startsWith("BK")) {
            code = "90." + code;
        }
        if (!force) {
            //查本地缓存
            String dateStr = DateUtil.tradeDate();
            String key = dateStr + "-" + code;
            List<EmDailyK> emDailyKS = DATE_KLINE_MAP.get(key);
            if (emDailyKS != null && emDailyKS.size() > 0) {
                log.info("hit local cache for code:{}", key);
                return emDailyKS;
            }


            //查本地文件
            String path = "kline/" + dateStr + "/" + key + ".json";
            path = projectDir + "/" + path;

            if (FileUtil.fileCheck(path)) {
                //读取文件
                log.info("hit local file for code:{}", path);
                String ks = FileUtil.readFromFile(path);
                if (ks != null && ks.startsWith("[")) {
                    List<EmDailyK> list = JSONArray.parseArray(ks, EmDailyK.class);
                    DATE_KLINE_MAP.put(key, list);
                    return list;
                }
            }
        }


        String secid = "0." + code;
        if (code.startsWith("6")) {
            secid = "1." + code;
        }

        String url = "http://push2his.eastmoney.com/api/qt/stock/kline/get?secid=" + secid
                + "&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6"
                + "&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61"
                + "&klt=101&fqt=1"
                + "&end=" + end.toString().replace("-", "")
                + "&lmt=" + lmt;

        //bk行情处理
        if (code.startsWith("90")) {
            url = "http://71.push2his.eastmoney.com/api/qt/stock/kline/get?secid=" + code
                    + "&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6" +
                    "&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61" +
                    "&klt=101&fqt=1&end=20500101&lmt=1000000";
        }

        if (code.startsWith("index")) {
            code = code.substring("index".length());
            url = "http://22.push2his.eastmoney.com/api/qt/stock/kline/get?secid=" + code
                    + "&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6" +
                    "&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61" +
                    "&klt=101&fqt=1&end=20500101&lmt=10000";
        }

        if (COUNT.incrementAndGet() % 10 == 0) {
            try {
                log.info("sleep 1s");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("sleep error", e);
            }
        }

        byte[] bytes = okHttpUtil.getForBytes(url, headerMap);

        if (bytes.length > 0) {
            String s = new String(bytes);
            EmResult<EmDataKline> emResult = null;
            try {
                emResult = JSONObject.parseObject(s, new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error("parse error:{}", s, e);
                return Collections.emptyList();
            }

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

            String dateStr = DateUtil.tradeDate();
            String key = dateStr + "-" + code;
            String path = "kline/" + dateStr + "/" + key + ".json";
            path = projectDir + "/" + path;
            if (!FileUtil.fileCheck(path)) {
                //不存在 且 在每天15:00之后
                if (LocalTime.now().isAfter(LocalTime.of(15, 0))
                        || LocalTime.now().isBefore(LocalTime.of(9, 0))) {
                    FileUtil.writeToFile(path, JSON.toJSONString(list));
                }
//                FileUtil.writeToFile(path, JSON.toJSONString(list));
            }
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
            List<EmCList> list = DATE_INDEX_ALL_MAP.get(DateUtil.today());
            if (list != null && list.size() > 0) {
                return list;
            }
        }
        List<EmCList> clist = getClist(1, 10000);
        if (clist.size() > 0) {
            List<EmCList> list = clist.stream().filter(e -> !(
                    e.getF12Code().startsWith("8")
                            || e.getF14Name().contains("ST")
                            || e.getF14Name().contains("退"))
            ).toList();

            //超过 每天 9:31 之后的数据 在放入缓存
            if (LocalTime.now().isAfter(LocalTime.of(9, 31))) {
                DATE_INDEX_ALL_MAP.put(DateUtil.today(), new ArrayList<>(list));
                return DATE_INDEX_ALL_MAP.get(DateUtil.today());
            }
            return new ArrayList<>(list);
        } else {
            return Collections.emptyList();
        }
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
    public List<EmCList> getIndex(IndexEnum type, boolean sync) {
        switch (type) {

            case ALL:
                return getClistDefaultSize(false);
            case BIXUAN:
                return must(sync);
            case EM_MA_UP:
                return xuanguCList();
            case HS300:
                return getIndex(IndexEnum.HS300.getUrl());
            case CYCF:
                return getIndex(IndexEnum.CYCF.getUrl());
            case ZZ500:
                return getIndex(IndexEnum.ZZ500.getUrl());
            case ZZ1000:
                return getIndex1000();
            case IndexAll_Filter:
                return sync ? getIndexAll() : Collections.emptyList();
//                return   Collections.emptyList();
            case IndexAll_Component:
                return getIndexAllComponent();
            case O_TP7:
                return getIndexTp7();
            case O_TP02:
                //遍历当天数据
//                return Collections.emptyList();
                return sync ? getIndexTp02() : Collections.emptyList();
            case OPEN_HIGH:
                return getIndexOpenHigh();
            case O_BK:
                List<EmCList> bkList = getIndex(IndexEnum.O_BK.getUrl());
                initBkMap(bkList);
                return bkList;

            case O_TAMT60:
            case X10:
                return getX10();
            default:
                return Collections.emptyList();
        }
    }

    private List<EmCList> getX10() {
        List<EmCList> list = getClistDefaultSize(false);
        List<EmCList> fList = list.stream().filter(em -> {
            boolean code = em.getF12Code().startsWith("6") || em.getF12Code().startsWith("0") || em.getF12Code().startsWith("3");
            boolean amt = em.getF6Amt().compareTo(B1_5Y) > 0;
            return code && amt;
        }).toList();

        ArrayList<EmCList> x10 = new ArrayList<>();
        for (EmCList emCList : fList) {
            List<EmDailyK> ks = getDailyKs(emCList.getF12Code(), LocalDate.now(), 500, false);
            if (ks.size() < 100) {
                continue;
            }
            BigDecimal[] array = ks.stream().map(EmDailyK::getAmt).toList().toArray(new BigDecimal[0]);
            BigDecimal[] ma60 = MaUtil.ma(60, array, 2);
            if (ks.get(ks.size() - 1).getAmt().compareTo(BDUtil.B10.multiply(ma60[ma60.length - 1])) > 0) {
                x10.add(emCList);
            }
        }

        return x10;
    }

    public List<EmCList> getIndexOpenHigh() {

        List<EmCList> all = getClistDefaultSize(false);
        List<EmCList> openHighList = all.stream().filter(em -> {
            BigDecimal pre = em.getF18Close();
            BigDecimal open = em.getF17Open();
            if (pre.compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }

            BigDecimal openPct = open.divide(pre, 4, RoundingMode.HALF_UP);
            return openPct.compareTo(new BigDecimal("1.005")) > 0
                    && openPct.compareTo(new BigDecimal("1.03")) < 0
                    && em.getF3Pct().compareTo(B7) > 0;
        }).toList();

        return new ArrayList<>(openHighList);
    }


    public List<EmCList> must() {
        String key = DateUtil.today();
        List<EmCList> emCLists = DATE_INDEX_BIXUAN_MAP.get(key);
        if (emCLists != null && emCLists.size() > 0) {
            return emCLists;
        }

        return must(0);
    }


    public List<EmCList> must(boolean sync) {
        if (sync) {
            return must(0);
        }
        return Collections.emptyList();
    }


    public List<EmCList> getIndex1990() {
        HashSet<EmCList> indexSet = new HashSet<>();
        indexSet.addAll(getIndex(IndexEnum.HS300.getUrl()));
        indexSet.addAll(getIndex(IndexEnum.CYCF.getUrl()));
        indexSet.addAll(getIndex(IndexEnum.ZZ500.getUrl()));
        indexSet.addAll(getIndex1000());

        return new ArrayList<>(indexSet);
    }


    public List<EmCList> must(int lastOffSet) {
        List<EmCList> index1990 = getIndex1990();
        List<String> indexCodes = index1990.stream().map(EmCList::getF12Code).toList();

        List<EmCList> list = getClistDefaultSize(false);
        List<EmCList> fList = list.stream().filter(e ->
                indexCodes.contains(e.getF12Code())
                        && e.getF6Amt().compareTo(B1_5Y) > 0
                        && e.getF8Turnover().compareTo(B10) < 0
                        && !e.getF14Name().contains("ST")
                        && !e.getF14Name().contains("退")
        ).toList();

        log.info("必选满足条件的 list size={}", fList.size());

        AtomicInteger count = new AtomicInteger(0);

        ArrayList<EmCList> result = new ArrayList<>();
        for (EmCList emCList : fList) {
            log.info("当前处理第{}/{}个", count.incrementAndGet(), fList.size());
            List<EmDailyK> ks = getDailyKs(emCList.getF12Code(), LocalDate.now(), 500, false);
            if (ks.size() < 250) {
                continue;
            }
            int last = ks.size() - 1 - lastOffSet;
            Map<String, BigDecimal[]> ma = MaUtil.ma(ks);
            BigDecimal[] ma5 = ma.get("ma5");
            BigDecimal[] ma10 = ma.get("ma10");
            BigDecimal[] ma20 = ma.get("ma20");
            BigDecimal[] ma30 = ma.get("ma30");
            BigDecimal[] ma60 = ma.get("ma60");
            BigDecimal[] ma120 = ma.get("ma120");
            BigDecimal[] ma250 = ma.get("ma250");
            //均线之上
            EmDailyK k = ks.get(last);
            BigDecimal[] ochl = k.ochl();
            BigDecimal o = ochl[0];
            BigDecimal c = ochl[1];
            BigDecimal h = ochl[2];
            BigDecimal l = ochl[3];

            if (c.compareTo(ma5[last]) <= 0) continue;
            if (c.compareTo(ma10[last]) <= 0) continue;
            if (c.compareTo(ma20[last]) <= 0) continue;
            if (c.compareTo(ma30[last]) <= 0) continue;
            if (c.compareTo(ma60[last]) <= 0) continue;
            if (c.compareTo(ma120[last]) <= 0) continue;
            if (c.compareTo(ma250[last]) <= 0) continue;
            //ma5 - ma60 之间 diff < 10%
            BigDecimal diffPct60 = MaUtil.maDiffPct60(ks);
            if (diffPct60.compareTo(B1_1) > 0) continue;

            ArrayList<BigDecimal> h5 = new ArrayList<>();
            ArrayList<BigDecimal> l5 = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                h5.add(ks.get(last - 1 - i).getHigh());
                l5.add(ks.get(last - 1 - i).getLow());
            }

            BigDecimal max = h5.stream().max(BigDecimal::compareTo).get();
            BigDecimal diff = max.subtract(c);
            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                continue;
            }

            BigDecimal min = l5.stream().min(BigDecimal::compareTo).get();
            BigDecimal diffLowPct = c.subtract(min).divide(c, 4, RoundingMode.HALF_UP);
            BigDecimal diffHighPct = max.subtract(c).divide(c, 4, RoundingMode.HALF_UP);
            if (diffHighPct.compareTo(b0_02) > 0 || diffLowPct.compareTo(b0_1) > 0) {
                //最近5天 最低点距离k 不超10%
                //最近5天 最高点距离k 不超2
                continue;
            }
            log.warn("find bixuan {}", emCList);
            result.add(emCList);
        }

        String key = DateUtil.today();
        DATE_INDEX_BIXUAN_MAP.put(key, result);
        log.info("必选满足条件的 list size={}", result.size());
        return result;
    }


    private List<EmCList> getIndexAllComponent() {
        log.info("getIndexAll 指数成份");
        List<EmCList> index = getIndex(IndexEnum.CYCF.getUrl());
        index.addAll(getIndex(IndexEnum.HS300.getUrl()));
        index.addAll(getIndex(IndexEnum.ZZ500.getUrl()));
        index.addAll(getIndex1000());
        return index;
    }

    private List<EmCList> getIndexAll() {
//        log.info("getIndexAll 指数成份");
//        List<EmCList> index =  getIndex(IndexEnum.CYCF.getUrl());
//        index.addAll(getIndex(IndexEnum.HS300.getUrl()));
//        index.addAll(getIndex(IndexEnum.ZZ500.getUrl()));
//        index.addAll(getIndex1000());
        log.info("get All 成份");

        List<EmCList> index = getClistDefaultSize(false);

        HashSet<String> codeSet = new HashSet<>(index.stream().map(EmCList::getF12Code).toList());
        List<EmCList> list = getClistDefaultSize(false);
        BigDecimal B_1 = new BigDecimal("-2");
        BigDecimal B15000_0000 = new BigDecimal("150000000");

        List<EmCList> filterList = list.stream().filter(c ->
                        codeSet.contains(c.getF12Code())
                                && c.getF3Pct().compareTo(B_1) > 0
                                && !c.getF14Name().contains("退")
                                //成交额大于1y5000万
                                && c.getF6Amt().compareTo(B15000_0000) > 0)
                .toList();

        try {
            return filterIndexTp02(filterList, IndexEnum.IndexAll_Filter);
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }
    }

    private List<EmCList> getIndexTp02() {
        //先拿到当天的缓存
        log.info("getIndexTp02 非指数成份");
        String key = DateUtil.today();
        List<EmCList> cacheList = DATE_INDEX_TP02_MAP.get(key);
        if (cacheList != null && cacheList.size() > 0) return cacheList;

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
        try {
            return filterIndexTp02(filterList, IndexEnum.O_TP02);
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }

    }

    public List<EmCList> filterIndexTp02(List<EmCList> list, IndexEnum indexEnum) throws InterruptedException {
        ArrayList<EmCList> filterList = new ArrayList<>();
        DATE_INDEX_TP02_MAP.putIfAbsent(DateUtil.today() + indexEnum.getType(), filterList);

        AtomicInteger count = new AtomicInteger(0);
        for (EmCList emCList : list) {

            if (count.addAndGet(1) % 10 == 0) {
                log.info("run {} / {}", count.get(), list.size());
            }

            List<EmDailyK> ks = null;
            try {
                ks = getDailyKs(emCList.getF12Code(), LocalDate.now(), 500, false);
            } catch (Exception e) {
                log.error("getDailyKs error", e);
            }
            if (ks == null || ks.size() < 250) {
                continue;
            }

            EmDailyK k = ks.get(ks.size() - 1);
            //过滤 均线之上

            Map<String, BigDecimal[]> ma = EmMaUtil.ma(ks);
            BigDecimal[] ma5 = ma.get("ma5");
            BigDecimal[] ma10 = ma.get("ma10");
            BigDecimal[] ma20 = ma.get("ma20");
            BigDecimal[] ma30 = ma.get("ma30");
            BigDecimal[] ma60 = ma.get("ma60");
            BigDecimal[] ma120 = ma.get("ma120");
            BigDecimal[] ma250 = ma.get("ma250");

            int len = ma5.length - 1;
            ArrayList<BigDecimal> maList = new ArrayList<>(Arrays.asList(ma5[len], ma10[len], ma20[len], ma30[len], ma60[len], ma120[len], ma250[len]));
            Collections.sort(maList);

            BigDecimal maMax = maList.get(maList.size() - 1);
            BigDecimal b098 = new BigDecimal("0.98");

            if (k.getClose().compareTo(b098.multiply(maMax)) > 0) {
                //过滤 距离均线之上最大不超过2%
                filterList.add(emCList);
            }
        }
        return filterList;
    }

    private List<EmCList> getIndexTp7() {
        List<EmCList> list = getClistDefaultSize(false);
        BigDecimal B = new BigDecimal("7");
        return list.stream()
                .filter(c -> c.getF3Pct().compareTo(B) > 0 && !c.getF14Name().contains("退"))
                .collect(Collectors.toList());
    }


    public List<EmCList> getIndex(String url) {
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
    public List<EmCList> getIndex1000() {
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


    /**
     * 选股器
     *
     * @return
     */
    public List<EmCList> xuanguCList() {
        List<EmCList> list = this.getClistDefaultSize(false);
        List<String> xuangu = xuangu();
        return new ArrayList<>(list.stream().filter(e -> xuangu.contains(e.getF12Code())).toList());
    }

    public List<String> xuangu() {
        String IS_SZ50 = "(IS_SZ50=\"是\")";
        String IS_HS300 = "(IS_HS300=\"是\")";
        String IS_ZZ500 = "(IS_ZZ500=\"是\")";
        String IS_ZZ1000 = "(IS_ZZ1000=\"是\")";
        String IS_CY50 = "(IS_CY50=\"是\")";
        String IS_KC50 = "(IS_KC50=\"是\")";

        List<String> list = List.of(IS_SZ50, IS_HS300, IS_ZZ500, IS_ZZ1000, IS_CY50, IS_KC50);
        Set<String> codes = new HashSet<>();
        for (String s : list) {
            log.info("xuangu {}", s);
            codes.addAll(xuangu(s));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return new ArrayList<>(codes);
    }

    private List<String> xuangu(String index) {
        String url = "https://data.eastmoney.com/dataapi/xuangu/list";
        Map<String, String> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();

        params.put("ps", "200");
        params.put("p", "1");
        params.put("sty", "SECUCODE,SECURITY_CODE,SECURITY_NAME_ABBR,NEW_PRICE,CHANGE_RATE,VOLUME_RATIO,HIGH_PRICE,LOW_PRICE,PRE_CLOSE_PRICE,VOLUME,DEAL_AMOUNT,TURNOVERRATE,DEAL_AMOUNT,LONG_AVG_ARRAY,INDEX");
        params.put("filter", "(DEAL_AMOUNT>=150000000)(LONG_AVG_ARRAY=\"1\")" + index);
        params.put("source", "SELECT_SECURITIES");
        params.put("client", "WEB");


        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Cache-Control", "no-cache");
        headers.put("Connection", "keep-alive");
        headers.put("Content-Type", "application/json");
        headers.put("Pragma", "no-cache");
        headers.put("Referer", "https://data.eastmoney.com/xuangu/");
        headers.put("Sec-Fetch-Dest", "empty");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("sec-ch-ua", "\"Chromium\";v=\"116\", \"Not)A;Brand\";v=\"24\", \"Google Chrome\";v=\"116\"");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("sec-ch-ua-platform", "\"macOS\"");

        byte[] bytes = okHttpUtil.getForBytes(url, headers, params);
        if (bytes.length == 0) return Collections.emptyList();

        String s = new String(bytes);
        JSONObject jsonObject = JSONObject.parseObject(s);

        if (jsonObject.getIntValue("code") != 0) {
            return Collections.emptyList();
        }

        JSONObject result = jsonObject.getJSONObject("result");
        if (result == null) {
            return Collections.emptyList();
        }

        Integer count = result.getInteger("count");
        if (count == null || count == 0) {
            return Collections.emptyList();
        }

        JSONArray dataArr = result.getJSONArray("data");
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < dataArr.size(); i++) {
            JSONObject jsonObj = dataArr.getJSONObject(i);
            String code = jsonObj.getString("SECURITY_CODE");
            codes.add(code);
        }
        return codes;
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
    public static final Map<String, List<EmCList>> DATE_INDEX_ALL_MAP = new ConcurrentHashMap<>();
    public static final Map<String, List<EmDailyK>> DATE_KLINE_MAP = new ConcurrentHashMap<>();
    public static final Map<String, List<EmCList>> DATE_INDEX_TP02_MAP = new ConcurrentHashMap<>();
    public static final Map<String, List<EmCList>> DATE_INDEX_BIXUAN_MAP = new ConcurrentHashMap<>();


}

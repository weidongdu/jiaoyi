package pro.jiaoyi.eastm.api;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.model.KPeriod;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class EmClient {

    @Autowired
    private OkHttpUtil okHttpUtil;

    public static final BigDecimal B100 = new BigDecimal("100");
    public static final BigDecimal B1000 = new BigDecimal("1000");

    //获取日线行情数据
    public List<EmDailyK> getDailyKs(String code, LocalDate end, int lmt, boolean force) {
        if (!force) {
            //查本地缓存
            String key = DateUtil.today() + "-" + code;
            List<EmDailyK> emDailyKS = DATE_KLINE_MAP.get(key);
            if (emDailyKS != null && emDailyKS.size() > 0) {
                log.info("hit local cache for code:{}", key);
                return emDailyKS;
            }
        }


        String secid = code.startsWith("6") ? "1." + code : "0." + code;
        String url = "http://push2his.eastmoney.com/api/qt/stock/kline/get?secid=" + secid
                + "&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6"
                + "&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61"
                + "&klt=101&fqt=1" + "&end=" + end.toString().replace("-", "") + "&lmt=" + lmt;

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

            log.info("获取日线行情数据 size={}\nstart=\t{}\nend=\t{}", size, size > 0 ? list.get(0) : "", size > 0 ? list.get(size - 1) : "");
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
        String url = "http://9.push2.eastmoney.com/api/qt/clist/get?pn=" + page + "&pz=" + pageSize + "&po=0&np=1&fltt=2&invt=2&wbp2u=6502094531899276|0|1|0|web&fid=f12&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048&fields=f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f14,f15,f16,f17,f18,f22,f23";
        byte[] bytes = okHttpUtil.getForBytes(url, headerMap);
        if (bytes.length > 0) {
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
                list.add(cList);
            }

            log.info("获取当日市场全部股票代码以及数据 size={}", list.size());
            return list;
        }
        return Collections.emptyList();
    }

    public List<EmCList> getClistDefault(boolean force) {
        //从本地缓存先加载
        if (!force) {
            List<EmCList> list = DATE_LIST_MAP.get(DateUtil.today());
            if (list != null && list.size() > 0) {
                return list;
            }
        }
        List<EmCList> clist = getClist(1, 10000);
        if (clist.size() > 0) {
            removeOldCache(DATE_LIST_MAP, 7);
            DATE_LIST_MAP.put(DateUtil.today(), clist);
        }
        return clist;
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

        List<EmCList> clistDefault = getClistDefault(force);
        Map<String, String> cnMap = new HashMap<>();
        for (EmCList cList : clistDefault) {
            cnMap.put(cList.getF12Code(), cList.getF14Name());
        }

        removeOldCache(cnMap, 7);
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
        List<EmCList> clistDefault = getClistDefault(force);
        Map<String, String> ncMap = new HashMap<>();
        for (EmCList cList : clistDefault) {
            ncMap.put(cList.getF14Name(), cList.getF12Code());
        }

        removeOldCache(ncMap, 7);
        DATE_CODE_NAME_MAP.put(key, ncMap);
        return ncMap;
    }


    private static void removeOldCache(Map map, int daysBefore) {
        ArrayList<String> oldList = new ArrayList<>();
        //移除前面15天的缓存
        if (DATE_CODE_NAME_MAP.size() > daysBefore * 2) {
            for (Object s : map.keySet()) {
                LocalDate localDate = DateUtil.strToLocalDate((String) s, DateUtil.PATTERN_yyyyMMdd);
                if (localDate.isBefore(LocalDate.now().minusDays(daysBefore))) {
                    oldList.add((String) s);
                }
            }
            if (oldList.size() > 0) {
                for (String s : oldList) {
                    DATE_CODE_NAME_MAP.remove(s);
                }
            }
        }
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
    public static final Map<String, List<EmCList>> DATE_LIST_MAP = new ConcurrentHashMap<>();
    public static final Map<String, List<EmDailyK>> DATE_KLINE_MAP = new ConcurrentHashMap<>();

}

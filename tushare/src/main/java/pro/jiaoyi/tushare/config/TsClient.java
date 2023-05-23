package pro.jiaoyi.tushare.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.tushare.model.TushareResult;
import pro.jiaoyi.tushare.model.kline.DailyK;
import pro.jiaoyi.tushare.model.kline.DailyKReq;
import pro.jiaoyi.tushare.model.stockbasic.StockBasic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TsClient {

    @Value("${tushare.token}")
    private String token;

    @Value("${tushare.baseUrl}")
    private String baseUrl;

    @Autowired
    private OkHttpUtil httpUtil;

    //设置stock basic 缓存 1天
    public static final Map<String, List<StockBasic>> STOCK_BASIC_MAP = new ConcurrentHashMap<>();

    /**
     * 获取 stock basic 列表
     * "fields":["ts_code","symbol","name","area","industry","market","list_date"]
     * "items":[["000001.SZ","000001","平安银行","深圳","银行","主板","19910403"]
     */
    public List<StockBasic> getStockBasicList() {
        //先检查本地缓存
        String key = LocalDate.now().toString().replaceAll("-", "");
        List<StockBasic> values = STOCK_BASIC_MAP.get(key);
        if (values != null && values.size() > 0) {
            return values;
        }

        String apiName = "stock_basic";
        JSONObject param = param(apiName);
        byte[] bytes = httpUtil.postJsonForBytes(baseUrl, null, param.toJSONString());
        if (bytes.length > 0) {
            TushareResult parse = parse(bytes);
            //{"fields":["ts_code","symbol","name","area","industry","market","list_date"],
            // "items":[["000001.SZ","000001","平安银行","深圳","银行","主板","19910403"]
            if (parse != null) {
                List<List<String>> items = parse.getData().getItems();
                if (items.size() > 0) {
                    ArrayList<StockBasic> list = new ArrayList<>(items.size());
                    items.forEach(item -> {
                        StockBasic stockBasicResp = new StockBasic();
                        stockBasicResp.setTs_code(item.get(0));
                        stockBasicResp.setSymbol(item.get(1));
                        stockBasicResp.setName(item.get(2));
                        stockBasicResp.setArea(item.get(3));
                        stockBasicResp.setIndustry(item.get(4));
                        stockBasicResp.setMarket(item.get(5));
                        stockBasicResp.setList_date(item.get(6));
                        list.add(stockBasicResp);
                    });

                    cacheClear();
                    STOCK_BASIC_MAP.put(key, list);

                    return list;
                }
            }

        }
        return Collections.emptyList();
    }

    /**
     * 获取 ts_code name map
     *
     * @return 000001.SZ -> 平安银行
     */
    public Map<String, String> tsCodeNameMap(boolean isSimple) {
        List<StockBasic> stockBasicList = getStockBasicList();
        if (stockBasicList.size() > 0) {
            //list stream 转换map
            return stockBasicList.stream().collect(
                    ConcurrentHashMap::new,
                    isSimple ? (m, v) -> m.put(v.getSymbol(), v.getName()) : (m, v) -> m.put(v.getTs_code(), v.getName()),
                    ConcurrentHashMap::putAll);
        }
        return Collections.emptyMap();
    }

    /**
     * 平安银行 -> 000001.SZ
     * 获取 name->ts_code map
     *
     * @return
     */
    public Map<String, String> nameTsCodeMap(boolean isSimple) {
        List<StockBasic> stockBasicList = getStockBasicList();
        if (stockBasicList.size() > 0) {
            //list stream 转换map
            return stockBasicList.stream().collect(
                    ConcurrentHashMap::new,
                    isSimple ? (m, v) -> m.put(v.getName(), v.getSymbol()) : (m, v) -> m.put(v.getName(), v.getTs_code()),
                    ConcurrentHashMap::putAll);
        }
        return Collections.emptyMap();
    }

    /**
     * 获取日线行情数据
     * 调取说明：120积分每分钟内最多调取500次，每次6000条数据，相当于单次提取23年历史数据。
     *
     * @return
     */
    public List<DailyK> dailyKs(DailyKReq req) {
        JSONObject param = param("daily");

        if (req != null) {
            if (req.getTrade_date() == null || req.getTrade_date().isEmpty()) {
                Optional.ofNullable(req.getTs_code())
                        .ifPresent(ts_code -> param.put("ts_code", ts_code));
                Optional.ofNullable(req.getStart_date())
                        .ifPresent(start_date -> param.put("start_date", start_date));
                Optional.ofNullable(req.getEnd_date())
                        .ifPresent(end_date -> param.put("end_date", end_date));
            } else {
                //需要获取某一个交易日的全部股票数据 只需要传入trade_date
                param.put("trade_date", req.getTrade_date());
            }
        }


        byte[] bytes = httpUtil.postJsonForBytes(baseUrl, null, param.toJSONString());
        TushareResult tr = parse(bytes);
        //vol	float	成交量 （手）
        //amount	float	成交额 （千元）
        //fields=[ts_code, trade_date, open, high, low, close, pre_close, change, pct_chg, vol, amount]
        //[605336.SH, 20230523, 17.11, 17.19, 16.71, 16.76, 17.11, -0.35, -2.0456, 22055.92, 37362.742]
        Map<String, String> tsCodeNameMap = tsCodeNameMap(false);

        if (tr != null) {
            TushareResult.DataResult data = tr.getData();
            if (data.getItems() != null && data.getItems().size() > 0) {
                ArrayList<DailyK> list = new ArrayList<>(data.getItems().size());
                data.getItems().forEach(item -> {
                    DailyK dailyK = new DailyK();

                    dailyK.setCode(item.get(0));
                    dailyK.setName(tsCodeNameMap.get(item.get(0)));

                    String trade_date = item.get(1);
                    dailyK.setTrade_date(trade_date);
                    dailyK.setTsOpen(DateUtil.toTimestamp(DateUtil.strToLocalDate(trade_date, DateUtil.PATTERN_yyyyMMdd)));
                    dailyK.setTsClose(DateUtil.toTimestamp(DateUtil.strToLocalDate(trade_date, DateUtil.PATTERN_yyyyMMdd)));

                    dailyK.setOpen(new BigDecimal(item.get(2)));
                    dailyK.setHigh(new BigDecimal(item.get(3)));
                    dailyK.setLow(new BigDecimal(item.get(4)));
                    dailyK.setClose(new BigDecimal(item.get(5)));
                    dailyK.setPre_close(new BigDecimal(item.get(6)));
                    dailyK.setChange(new BigDecimal(item.get(7)));
                    dailyK.setPct(new BigDecimal(item.get(8)));

                    dailyK.setVol(new BigDecimal(item.get(9)).multiply(new BigDecimal(100)));
                    dailyK.setAmt(new BigDecimal(item.get(10)).multiply(new BigDecimal(1000)));
                    list.add(dailyK);
                });
                return list;
            }
        }
        return Collections.emptyList();
    }

    public JSONObject param(String apiName) {
        JSONObject param = new JSONObject();
        param.put("api_name", apiName);
        param.put("token", token);
        return param;
    }

    private TushareResult parse(byte[] bytes) {
        return JSON.parseObject(bytes, TushareResult.class);
    }

    public void cacheClear() {
        if (STOCK_BASIC_MAP.size() > 50) {
            ArrayList<String> keys = new ArrayList<>(STOCK_BASIC_MAP.keySet());
            for (int i = 0; i < 10; i++) {
                //移除10天之前的缓存
                keys.remove(LocalDate.now().minusDays(i).toString().replaceAll("-", ""));
            }
            keys.forEach(STOCK_BASIC_MAP::remove);
        }
    }

}

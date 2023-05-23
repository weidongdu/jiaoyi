package pro.jiaoyi.tushare.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.tushare.model.TushareResult;
import pro.jiaoyi.tushare.model.stockbasic.StockBasic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

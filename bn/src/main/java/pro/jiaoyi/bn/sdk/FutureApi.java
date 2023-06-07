package pro.jiaoyi.bn.sdk;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 期货api
 */
@Component
@Slf4j
public class FutureApi {
    @Resource
    private OkHttpUtil okHttpUtil;
    public static final String BASE_URL = "https://fapi.binance.com";


    /**
     * 获取交易规则和交易对
     */
    public List<String> getExchangeInfo() {
        String url = BASE_URL + "/fapi/v1/exchangeInfo";
        byte[] bytes = okHttpUtil.getForBytes(url, null);
        String s = new String(bytes);
        JSONObject jsonObject = JSONObject.parseObject(s);
        JSONArray symbols = jsonObject.getJSONArray("symbols");

        if (symbols == null || symbols.size() == 0) {
            return Collections.emptyList();
        }
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < symbols.size(); i++) {
            JSONObject symbol = symbols.getJSONObject(i);
            String symbolStr = symbol.getString("symbol");
            String status = symbol.getString("status");
            if ("TRADING".equals(status)) {
                list.add(symbolStr);
            }
        }

        return list;
    }

    /**
     * kline 数据
     */
    public void kline(String symbol, String period, int limit) {

    }

    /**
     * 获取symbol 费率
     */
    public void fee(String symbol) {

    }


    /**
     * 获取持仓量
     *
     * @param symbol
     */
    public void getOI(String symbol) {

    }

    /**
     * 24hr价格变动情况
     * GET /fapi/v1/ticker/24hr
     */

    /**
     * 查询资金费率历史
     * GET /fapi/v1/fundingRate
     */
    public void fundingRate(String symbol) {
        String url = BASE_URL + "/fapi/v1/fundingRate";
        if (symbol != null) {
            url = url + "?symbol=" + symbol;
        }
        byte[] forBytes = okHttpUtil.getForBytes(url, null);
        if (forBytes.length == 0) {
            return;
        }

        String s = new String(forBytes);
        JSONObject jsonObject = JSONObject.parseObject(s);

    }



}

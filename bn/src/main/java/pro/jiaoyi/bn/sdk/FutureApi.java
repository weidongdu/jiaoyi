package pro.jiaoyi.bn.sdk;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.bn.dao.entity.PremiumIndexEntity;
import pro.jiaoyi.bn.model.BnK;
import pro.jiaoyi.bn.model.ExchangeInfo;
import pro.jiaoyi.bn.model.OpenInterestDto;
import pro.jiaoyi.bn.model.Ticker24hr;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static pro.jiaoyi.bn.service.BnAccountTradeService.SYMBOL_PRECISION_MAP;

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

    public ExchangeInfo exchangeInfo() {
        String url = BASE_URL + "/fapi/v1/exchangeInfo";
        byte[] bytes = okHttpUtil.getForBytes(url, null);
        String body = new String(bytes);
        JSONObject jsonObject = JSONObject.parseObject(body);
        return jsonObject.toJavaObject(ExchangeInfo.class);
    }

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
     * 获取symbol 费率
     */
    public void fee(String symbol) {

    }


    /**
     * 获取持仓量
     * GET /futures/data/openInterestHist
     *
     * @param symbol
     * @return
     */
    /*
    参数:

    名称	类型	是否必需	描述
    symbol	STRING	YES
    period	ENUM	YES	"5m","15m","30m","1h","2h","4h","6h","12h","1d"
    limit	LONG	NO	default 30, max 500
    startTime	LONG	NO
    endTime	LONG	NO
     */
    /*
    [
    {
         "symbol":"BTCUSDT",
          "sumOpenInterest":"20403.12345678",// 持仓总数量
          "sumOpenInterestValue": "176196512.12345678", // 持仓总价值
          "timestamp":"1583127900000"

     },

     {

         "symbol":"BTCUSDT",
         "sumOpenInterest":"20401.36700000",
         "sumOpenInterestValue":"149940752.14464448",
         "timestamp":"1583128200000"

        },

]

     */
    public List<OpenInterestDto> getOI(String symbol) {
        String url = BASE_URL + "/futures/data/openInterestHist";
        url = url + "?symbol=" + symbol + "&period=5m&limit=30";

        byte[] bytes = okHttpUtil.getForBytes(url, null);
        if (bytes.length == 0) return Collections.emptyList();

        String s = new String(bytes);
        return JSONArray.parseArray(s).toJavaList(OpenInterestDto.class);
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
        JSONArray jsonArray = JSONArray.parseArray(s);

    }

    /*
    最新标记价格和资金费率
    GET /fapi/v1/premiumIndex
    采集各大交易所数据加权平均
    [
        {
            "symbol": "BTCUSDT",            // 交易对
            "markPrice": "11793.63104562",  // 标记价格
            "indexPrice": "11781.80495970", // 指数价格
            "estimatedSettlePrice": "11781.16138815",  // 预估结算价,仅在交割开始前最后一小时有意义
            "lastFundingRate": "0.00038246",    // 最近更新的资金费率
            "nextFundingTime": 1597392000000,   // 下次资金费时间
            "interestRate": "0.00010000",       // 标的资产基础利率
            "time": 1597370495002               // 更新时间
        }
    ]
     */
    public List<PremiumIndexEntity> premiumIndex() {
        String url = BASE_URL + "/fapi/v1/premiumIndex";
        byte[] forBytes = okHttpUtil.getForBytes(url, null);
        if (forBytes.length == 0) {
            return Collections.emptyList();
        }

        String s = new String(forBytes);
        JSONArray jsonArray = JSONArray.parseArray(s);
        if (jsonArray == null || jsonArray.size() == 0) {
            return Collections.emptyList();
        }

        return jsonArray.toJavaList(PremiumIndexEntity.class);
    }


    /**
     * GET /fapi/v1/klines
     * <p>
     * 每根K线的开盘时间可视为唯一ID
     * <p>
     * 权重: 取决于请求中的LIMIT参数
     * <p>
     * 参数:
     * <p>
     * 名称	类型	是否必需	描述
     * symbol	STRING	YES	交易对
     * interval	ENUM	YES	时间间隔
     * startTime	LONG	NO	起始时间
     * endTime	LONG	NO	结束时间
     * limit	INT	NO	默认值:500 最大值:1500.
     * 缺省返回最近的数据
     */
    public List<BnK> kline(String symbol, String interval, int limit) {
        log.info("kline symbol={} interval={} limit={}", symbol, interval, limit);
        String url = BASE_URL + "/fapi/v1/klines";
        if (symbol != null) {
            url = url + "?symbol=" + symbol + "&interval=" + interval + "&limit=" + limit;
        }
        byte[] bytes = okHttpUtil.getForBytes(url, null);
        if (bytes.length == 0) {
            return Collections.emptyList();
        }

        String s = new String(bytes);
        JSONArray jsonArray = JSONArray.parseArray(s);
        if (jsonArray == null || jsonArray.size() == 0) {
            return Collections.emptyList();
        }

        ArrayList<BnK> list = new ArrayList<BnK>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONArray items = jsonArray.getJSONArray(i);
            //[
            //    1499040000000,      // 开盘时间
            //    "0.01634790",       // 开盘价
            //    "0.80000000",       // 最高价
            //    "0.01575800",       // 最低价
            //    "0.01577100",       // 收盘价(当前K线未结束的即为最新价)
            //    "148976.11427815",  // 成交量
            //    1499644799999,      // 收盘时间
            //    "2434.19055334",    // 成交额
            //    308,                // 成交笔数
            //    "1756.87402397",    // 主动买入成交量
            //    "28.46694368",      // 主动买入成交额
            //    "17928899.62484339" // 请忽略该参数
            //  ]

            BnK bnK = new BnK();

            bnK.setTsOpen(items.getLong(0));
            bnK.setOpen(items.getBigDecimal(1));
            bnK.setHigh(items.getBigDecimal(2));
            bnK.setLow(items.getBigDecimal(3));
            bnK.setClose(items.getBigDecimal(4));
            bnK.setVol(items.getBigDecimal(5));
            bnK.setTsClose(items.getLong(6));
            bnK.setAmt(items.getBigDecimal(7));

            if (i == 0) {
                bnK.setPct(BigDecimal.ZERO);
            } else {
                BigDecimal fc = list.get(list.size() - 1).getClose();
                BigDecimal pct = bnK.getClose().subtract(fc).divide(fc, 3, RoundingMode.HALF_UP);
                bnK.setPct(pct);
            }
            bnK.setCount(items.getInteger(8));
            bnK.setBuyVol(items.getBigDecimal(9));
            bnK.setBuyAmt(items.getBigDecimal(10));
            list.add(bnK);

        }

        return list;
    }


    //24小时信息 /fapi/v1/ticker/24hr
    public List<Ticker24hr> ticker24hr() {
        String url = BASE_URL + "/fapi/v1/ticker/24hr";
        try {
            byte[] bytes = okHttpUtil.getForBytes(url, null);
            String s = new String(bytes);
            log.info("ticker24hr {}", s);
            JSONArray jsonArray = JSONArray.parseArray(s);

            if (jsonArray == null || jsonArray.size() == 0) {
                return Collections.emptyList();
            }
            List<Ticker24hr> list = jsonArray.toJavaList(Ticker24hr.class);
            List<Ticker24hr> sortList = list.stream().sorted(Comparator.comparing(Ticker24hr::getQuoteVolume)).collect(Collectors.toList());
            Collections.reverse(sortList);
            return sortList;
        } catch (Exception e) {
            log.error("get ticker24hr error {} {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }


    public List<String> ticker24hrSymbol(int limit) {
        List<Ticker24hr> list = ticker24hr();
        if (list.size() > 0 && list.size() > limit) {
            return list.subList(0, limit).stream().map(Ticker24hr::getSymbol).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }


    public void symbol_precision() {
        ExchangeInfo exchangeInfo = this.exchangeInfo();
        if (exchangeInfo != null && exchangeInfo.getSymbols() != null && exchangeInfo.getSymbols().size() > 0) {
            List<ExchangeInfo.Symbol> symbols = exchangeInfo.getSymbols();
            for (ExchangeInfo.Symbol symbol : symbols) {
                String symbolName = symbol.getSymbol();
                Integer precision = symbol.getPricePrecision();
                SYMBOL_PRECISION_MAP.put(symbolName, precision);
            }
        }
    }
}

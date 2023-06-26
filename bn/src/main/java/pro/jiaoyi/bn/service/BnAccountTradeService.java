package pro.jiaoyi.bn.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.jiaoyi.bn.config.WxUtil;
import pro.jiaoyi.bn.model.trade.AccountPosition;
import pro.jiaoyi.bn.model.trade.OpenOrders;
import pro.jiaoyi.bn.sdk.FutureApi;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static pro.jiaoyi.common.util.BDUtil.*;


@Component
@Slf4j
public class BnAccountTradeService {

    public static final Map<String, Integer> SYMBOL_PRECISION_MAP = new HashMap<>();


    @Autowired
    private WxUtil wxUtil;
    /*
    需要签名的接口 (TRADE 与 USER_DATA)
    调用这些接口时，除了接口本身所需的参数外，还需要传递signature即签名参数。
    签名使用HMAC SHA256算法. API-KEY所对应的API-Secret作为 HMAC SHA256 的密钥，
    其他所有参数作为HMAC SHA256的操作对象，得到的输出即为签名。
    签名大小写不敏感。
    当同时使用query string和request body时，HMAC SHA256的输入query string在前，request body在后

    示例 1: 所有参数通过 query string 发送
    示例1:

    HMAC SHA256 签名:

        $ echo -n "symbol=BTCUSDT&side=BUY&type=LIMIT&quantity=1&price=9000&timeInForce=GTC&recvWindow=5000&timestamp=1591702613943"
        | openssl dgst -sha256 -hmac "2b5eb11e18796d12d88f13dc27dbbd02c2cc51ff7059765ed9821957d82bb4d9"
        (stdin)= 3c661234138461fcc7a7d8746c6558c9842d4e10870d2ecbedf7777cad694af9
    curl 调用:

        (HMAC SHA256)
        $ curl
        -H "X-MBX-APIKEY: dbefbc809e3e83c283a984c3a1459732ea7db1360ca80c5c2c8867408d28cc83"
        -X POST 'https://fapi.binance.com/fapi/v1/order?symbol=BTCUSDT&side=BUY&type=LIMIT&quantity=1&price=9000&timeInForce=GTC&recvWindow=5000&timestamp=1591702613943&signature= 3c661234138461fcc7a7d8746c6558c9842d4e10870d2ecbedf7777cad694af9'
    queryString:

    symbol=BTCUSDT
    &side=BUY
    &type=LIMIT
    &timeInForce=GTC
    &quantity=1
    &price=0.1
    &recvWindow=5000
    &timestamp=1499827319559
     */

    /*
    账户信息V2 (USER_DATA)
    GET /fapi/v2/account (HMAC SHA256)

    现有账户信息。 用户在单资产模式和多资产模式下会看到不同结果，响应部分的注释解释了两种模式下的不同。

    权重: 5

    参数:

    名称	类型	是否必需	描述
    recvWindow	LONG	NO
    timestamp	LONG	YES
     */

    @Autowired
    private OkHttpUtil httpUtil;

    public static final String API_KEY = "ejuJIQLyh07e42QYf0uF08t0hmLvGugDgyMKvSMDsPSqAWJnaXZTFkSN91nFOTXj";
    public static final String API_SECRET = "QLkOFH1llem536RZoYFeFB4twpUNCGf02LFaiYyy0gOslsPMOcp4W7vHlzXumz56";

    public static final Map<String, String> HEADERS = new HashMap<>();

    static {
        HEADERS.put("X-MBX-APIKEY", API_KEY);
    }


    public static final String BASE_URL_FAPI = "https://fapi.binance.com";

    public String accountInfoV2() throws Exception {
        String url = BASE_URL_FAPI + "/fapi/v2/account";
        String query = "timestamp=" + new Date().getTime();
        return sendSignatureRequest(url, query, "GET");
    }

    public List<AccountPosition> positionList() {
        String s = null;
        try {
            s = this.accountInfoV2();
        } catch (Exception e) {
            log.error("", e);
            return Collections.emptyList();
        }

        JSONObject jsonObject = JSON.parseObject(s);
        if (jsonObject == null) {
            return Collections.emptyList();
        }


        JSONArray positions = jsonObject.getJSONArray("positions");
        if (positions == null || positions.size() == 0) {
            return Collections.emptyList();
        }

        List<AccountPosition> accountPositions = positions.toJavaList(AccountPosition.class);
        List<AccountPosition> list = accountPositions.stream().filter(accountPosition -> accountPosition.getPositionAmt().compareTo(BigDecimal.ZERO) != 0).collect(Collectors.toList());
        return list;

    }

    /**
     * 获取当前挂单
     */
    /*
    查看当前全部挂单 (USER_DATA)
    响应:

    [
      {
        "avgPrice": "0.00000",              // 平均成交价
        "clientOrderId": "abc",             // 用户自定义的订单号
        "cumQuote": "0",                        // 成交金额
        "executedQty": "0",                 // 成交量
        "orderId": 1917641,                 // 系统订单号
        "origQty": "0.40",                  // 原始委托数量
        "origType": "TRAILING_STOP_MARKET", // 触发前订单类型
        "price": "0",                   // 委托价格
        "reduceOnly": false,                // 是否仅减仓
        "side": "BUY",                      // 买卖方向
        "positionSide": "SHORT", // 持仓方向
        "status": "NEW",                    // 订单状态
        "stopPrice": "9300",                    // 触发价，对`TRAILING_STOP_MARKET`无效
        "closePosition": false,   // 是否条件全平仓
        "symbol": "BTCUSDT",                // 交易对
        "time": 1579276756075,              // 订单时间
        "timeInForce": "GTC",               // 有效方法
        "type": "TRAILING_STOP_MARKET",     // 订单类型
        "activatePrice": "9020", // 跟踪止损激活价格, 仅`TRAILING_STOP_MARKET` 订单返回此字段
        "priceRate": "0.3", // 跟踪止损回调比例, 仅`TRAILING_STOP_MARKET` 订单返回此字段
        "updateTime": 1579276756075,        // 更新时间
        "workingType": "CONTRACT_PRICE", // 条件价格触发类型
        "priceProtect": false            // 是否开启条件单触发保护
      }
    ]
        GET /fapi/v1/openOrders (HMAC SHA256)

        请小心使用不带symbol参数的调用

        权重: - 带symbol 1 - 不带 40

        参数:

        名称	类型	是否必需	描述
        symbol	STRING	NO	交易对
        recvWindow	LONG	NO
        timestamp	LONG	YES
        不带symbol参数，会返回所有交易对的挂单
     */
    public List<OpenOrders> openOrders(String symbol) throws Exception {
        String url = BASE_URL_FAPI + "/fapi/v1/openOrders";
        String query = "symbol=" + symbol + "&timestamp=" + new Date().getTime();
        String orders = sendSignatureRequest(url, query, "GET");
        if (orders == null) {
            return Collections.emptyList();
        }

        List<OpenOrders> openOrders = JSON.parseArray(orders, OpenOrders.class);
        if (openOrders == null) {
            return Collections.emptyList();
        }
        return openOrders;
    }


    /*
    撤销订单 (TRADE)
        响应:

        {
            "clientOrderId": "myOrder1", // 用户自定义的订单号
            "cumQty": "0",
            "cumQuote": "0", // 成交金额
            "executedQty": "0", // 成交量
            "orderId": 283194212, // 系统订单号
            "origQty": "11", // 原始委托数量
            "price": "0", // 委托价格
            "reduceOnly": false, // 仅减仓
            "side": "BUY", // 买卖方向
            "positionSide": "SHORT", // 持仓方向
            "status": "CANCELED", // 订单状态
            "stopPrice": "9300", // 触发价，对`TRAILING_STOP_MARKET`无效
            "closePosition": false,   // 是否条件全平仓
            "symbol": "BTCUSDT", // 交易对
            "timeInForce": "GTC", // 有效方法
            "origType": "TRAILING_STOP_MARKET", // 触发前订单类型
            "type": "TRAILING_STOP_MARKET", // 订单类型
            "activatePrice": "9020", // 跟踪止损激活价格, 仅`TRAILING_STOP_MARKET` 订单返回此字段
            "priceRate": "0.3", // 跟踪止损回调比例, 仅`TRAILING_STOP_MARKET` 订单返回此字段
            "updateTime": 1571110484038, // 更新时间
            "workingType": "CONTRACT_PRICE", // 条件价格触发类型
            "priceProtect": false            // 是否开启条件单触发保护
        }
        DELETE /fapi/v1/order (HMAC SHA256)

        权重: 1

        Parameters:

        名称	类型	是否必需	描述
        symbol	STRING	YES	交易对
        orderId	LONG	NO	系统订单号
        origClientOrderId	STRING	NO	用户自定义的订单号
        recvWindow	LONG	NO
        timestamp	LONG	YES
        orderId 与 origClientOrderId 必须至少发送一个
     */
    public List<OpenOrders> cancelOpenOrders(String symbol, BigDecimal orderId) throws Exception {
        String url = BASE_URL_FAPI + "/fapi/v1/order";
        String query = "symbol=" + symbol + "&orderId=" + orderId + "&timestamp=" + new Date().getTime();
        String delete = sendSignatureRequest(url, query, "DELETE");
        if (delete == null) {
            return Collections.emptyList();
        }

        List<OpenOrders> openOrders = JSONArray.parseArray(delete, OpenOrders.class);
        if (openOrders == null) {
            return Collections.emptyList();
        }
        return openOrders;
    }

    public String sendSignatureRequest(String url, String query, String method) throws Exception {
        String signature = calculateHMac(API_SECRET, query);
        Map<String, String> map = new HashMap<>();
        map.put("X-MBX-APIKEY", API_KEY);
        Request request;
        if (method == null) {
            request = httpUtil.buildRequest(url + "?" + query + "&signature=" + signature, map);
        } else {
            request = httpUtil.buildRequest(url + "?" + query + "&signature=" + signature, map, method);
        }
        log.info("request url:{}", request.url().toString());
        byte[] bytes = httpUtil.sendDefault(request);
        if (bytes.length == 0) {
            return null;
        }
        return new String(bytes);
    }

    public static final String ALGORITHM = "HmacSHA256";

    public static String calculateHMac(String key, String data) throws Exception {
        Mac sha256_HMAC = Mac.getInstance(ALGORITHM);

        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        sha256_HMAC.init(secret_key);

        return byteArrayToHex(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }


    /**
     * 只对当前持仓进行挂单
     *
     * @param position
     */
    public void tradeOrder(AccountPosition position) throws Exception {
        log.info("持仓{}", position.getSymbol());
        BigDecimal unRealizedProfit = position.getUnrealizedProfit();
        if (unRealizedProfit.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("当前持仓{}无盈利 不进行挂单", position.getSymbol());
            return;
        }


        //有盈利 判断比例
        BigDecimal pct = unRealizedProfit.divide(position.getIsolatedWallet(), 2, RoundingMode.HALF_UP);
        if (pct.compareTo(new BigDecimal("0.1")) < 0) {
            log.info("当前持仓盈利:{}% 不进行挂单", BDUtil.p100(pct));
            return;
        }
        log.info("当前持仓{}盈利:{}%", position.getSymbol(), BDUtil.p100(pct));
        // 盈利比例大于20%  平空 检查是否有保底持仓
        List<OpenOrders> openOrders = openOrders(position.getSymbol());
        // 查看当前挂单
        BigDecimal stopPrice = calcStopPrice(position, pct);
        log.info("当前持仓{}止盈价:{}", position.getSymbol(), stopPrice);
        if (openOrders.size() == 0) {
            // 挂单保底
            log.info("当前持仓{}无挂单,挂单止盈", position.getSymbol());
            order(position, stopPrice);
            return;
        }

        // 有多个挂单
        //区分 long short
        if (position.getPositionAmt().compareTo(BigDecimal.ZERO) > 0) {
            //多单, 取最大在stop price
            BigDecimal maxPrice = openOrders.stream().max(Comparator.comparing(OpenOrders::getStopPrice)).get().getStopPrice();
            if (maxPrice.compareTo(stopPrice) < 0) {
                //挂单保底
                log.info("当前持仓{}多单,挂单止盈", position.getSymbol());
                order(position, stopPrice);
                //撤消所有 price < stopPrice 的挂单
                openOrders.forEach(openOrder -> {
                    if (openOrder.getStopPrice().compareTo(stopPrice) < 0) {
                        try {
                            cancelOpenOrders(position.getSymbol(), openOrder.getOrderId());
                        } catch (Exception e) {
                            log.info("撤单失败 {} {}", e.getMessage(), e);
                        }
                    }
                });
            }
        }


        if (position.getPositionAmt().compareTo(BigDecimal.ZERO) < 0) {
            //空单, 取最小的stop price
            BigDecimal minPrice = openOrders.stream().min(Comparator.comparing(OpenOrders::getStopPrice)).get().getStopPrice();
            if (minPrice.compareTo(stopPrice) > 0) {
                //挂单保底
                log.info("当前持仓空单,挂单保底");
                order(position, stopPrice);
                //撤消所有 price > stopPrice 的挂单
                openOrders.forEach(openOrder -> {
                    if (openOrder.getStopPrice().compareTo(stopPrice) > 0) {
                        try {
                            cancelOpenOrders(position.getSymbol(), openOrder.getOrderId());
                        } catch (Exception e) {
                            //2023-06-12 22:01:11.345 [scheduling-1] [INFO ] top.duwd.bn.service.BnAccountTradeService [331] lambda$tradeOrder$1 撤单失败 field null expect '[', but {, pos 1, line 1, column 2{"orr
                            //derId":2817496117,"symbol":"NKNUSDT","status":"CANCELED","clientOrderId":"android_MirFIbKu9zjPrbUpffUO","price":"0","avgPrice":"0.00000","origQty":"8804","executedQty":"0","cumQty":"0","cumQuote":"0","timeInForce":"GTE_GTC","type":"STOP_MARKET","reduceOnly":true,"closePosition":false,"side":"SELL","positionSide":"BOTH","stopPrice":"0.09523","workingType":"MARK_PRICE","priceProtect":false,"origType":"STOP_MARKET","updateTime":1686578471309} {}
                            //com.alibaba.fastjson.JSONException: field null expect '[', but {, pos 1, line 1, column 2{"orderId":2817496117,"symbol":"NKNUSDT","status":"CANCELED","clientOrderId":"android_MirFIbKu9zjPrbUpffUO","price":"0","avgPrice":"0.00000","origQty":"8804","executedQty":"0","cumQty":"0","cumQuote":"0","timeInForce":"GTE_GTC","type":"STOP_MARKET","reduceOnly":true,"closePosition":false,"side":"SELL","positionSide":"BOTH","stopPrice":"0.09523","workingType":"MARK_PRICE","priceProtect":false,"origType":"STOP_MARKET","updateTime":1686578471309}
                            //        at com.alibaba.fastjson.parse
                            log.info("撤单失败 {} {}", e.getMessage(), e);
                        }
                    }
                });
            }
        }


    }


    @Autowired
    private FutureApi futureApi;

    /**
     * 根据pct(盈利百分比 计算止盈价)
     *
     * @param position
     * @param pct
     * @return
     */
    public BigDecimal calcStopPrice(AccountPosition position, BigDecimal pct) {
        BigDecimal entryPrice = position.getEntryPrice();
        Integer s = SYMBOL_PRECISION_MAP.get(position.getSymbol());
        if (s == null) {
            futureApi.symbol_precision();
            s = SYMBOL_PRECISION_MAP.get(position.getSymbol());
        }
        int scale = s == null ? entryPrice.scale() : s;


        BigDecimal base = BigDecimal.ONE;
        BigDecimal holdPct = (new BigDecimal("0.001"));
        if (position.getPositionAmt().compareTo(BigDecimal.ZERO) > 0) {
            //多单
            //0.1 保本
            //0.2 保本 * 2
            //0.3 保本 * 3
            //0.4 保本 * 4
            //0.5 保本 * 10
            //0.6 保本 * 20
            //0.7 保本 * 30
            //0.8 保本 * 40
            //0.9 保本 * 50
            if (pct.compareTo(new BigDecimal(2)) > 0) {
                base = base.add(holdPct.multiply(new BigDecimal("100")));

            } else if (pct.compareTo(new BigDecimal(1)) > 0) {
                base = base.add(holdPct.multiply(new BigDecimal("60")));

            } else if (pct.compareTo(new BigDecimal("0.9")) > 0) {
                base = base.add(holdPct.multiply(new BigDecimal("50")));

            } else if (pct.compareTo(new BigDecimal("0.8")) > 0) {
                base = base.add(holdPct.multiply(new BigDecimal("40")));

            } else if (pct.compareTo(new BigDecimal("0.7")) > 0) {
                base = base.add(holdPct.multiply(new BigDecimal("30")));

            } else if (pct.compareTo(new BigDecimal("0.6")) > 0) {
                base = base.add(holdPct.multiply(new BigDecimal("20")));

            } else if (pct.compareTo(new BigDecimal("0.5")) > 0) {
                base = base.add(holdPct.multiply(new BigDecimal("10")));

            } else if (pct.compareTo(new BigDecimal("0.4")) > 0) {
                base = base.add(holdPct.multiply(new BigDecimal("7")));

            } else if (pct.compareTo(new BigDecimal("0.3")) > 0) {
                base = base.add(holdPct.multiply(new BigDecimal("5")));

            } else if (pct.compareTo(new BigDecimal("0.2")) > 0) {
                base = base.add(holdPct.multiply(new BigDecimal("3")));
            } else {
                base = base.add(holdPct.multiply(new BigDecimal("1")));
            }

        } else {
            //空单
            if (pct.compareTo(new BigDecimal(2)) > 0) {
                base = base.subtract(holdPct.multiply(new BigDecimal("100")));

            } else if (pct.compareTo(new BigDecimal(1)) > 0) {
                base = base.subtract(holdPct.multiply(new BigDecimal("60")));

            } else if (pct.compareTo(new BigDecimal("0.9")) > 0) {
                base = base.subtract(holdPct.multiply(new BigDecimal("50")));

            } else if (pct.compareTo(new BigDecimal("0.8")) > 0) {
                base = base.subtract(holdPct.multiply(new BigDecimal("40")));

            } else if (pct.compareTo(new BigDecimal("0.7")) > 0) {
                base = base.subtract(holdPct.multiply(new BigDecimal("30")));

            } else if (pct.compareTo(new BigDecimal("0.6")) > 0) {
                base = base.subtract(holdPct.multiply(new BigDecimal("20")));

            } else if (pct.compareTo(new BigDecimal("0.5")) > 0) {
                base = base.subtract(holdPct.multiply(new BigDecimal("10")));

            } else if (pct.compareTo(new BigDecimal("0.4")) > 0) {
                base = base.subtract(holdPct.multiply(new BigDecimal("7")));

            } else if (pct.compareTo(new BigDecimal("0.3")) > 0) {
                base = base.subtract(holdPct.multiply(new BigDecimal("5")));

            } else if (pct.compareTo(new BigDecimal("0.2")) > 0) {
                base = base.subtract(holdPct.multiply(new BigDecimal("3")));
            } else {
                base = base.subtract(holdPct.multiply(new BigDecimal("1")));
            }
        }

        log.info("当前持仓{} 盈利{}%, 止盈{}%",
                position.getSymbol(), BDUtil.p100(pct)
                , BDUtil.p100(position.getLeverage().multiply(base.subtract(BigDecimal.ONE).abs())));
        return entryPrice.multiply(base).setScale(scale, RoundingMode.HALF_UP);
    }

    public void order(AccountPosition position, BigDecimal stopPrice) throws Exception {
        wxUtil.send("挂单_" + position.getSymbol() + "_触发价=" + stopPrice.toPlainString());
        String url = BASE_URL_FAPI + "/fapi/v1/order";

        String side = position.getPositionAmt().compareTo(BigDecimal.ZERO) > 0 ? "SELL" : "BUY";
        String type = "STOP_MARKET";
        String reduceOnly = "true";
        BigDecimal quantity = position.getPositionAmt();
        String workingType = "MARK_PRICE";
        String query = "symbol=" + position.getSymbol() + "&side=" + side + "&type=" + type + "&reduceOnly=" + reduceOnly + "&quantity=" + quantity.abs().toPlainString() + "&stopPrice=" + stopPrice + "&workingType=" + workingType + "&recvWindow=60000" + "&timestamp=" + new Date().getTime();
        log.info("挂单 {}", url + query);
        //当前持仓多单,挂单保底
        //2023-06-12 22:01:10.530 [scheduling-1] [INFO ] top.duwd.bn.service.BnAccountTradeService [383] order 挂单 https://fapi.binance.com/fapi/v1/ordersymbol=NKNUSDT&side=SELL&type=STOP_MAA
        //RKET&reduceOnly=true&quantity=8804&stopPrice=0.0967497903475&workingType=MARK_PRICE&recvWindow=60000&timestamp=1686578470530
        //2023-06-12 22:01:10.530 [scheduling-1] [INFO ] top.duwd.bn.service.BnAccountTradeService [257] sendSignatureRequest request url:https://fapi.binance.com/fapi/v1/order?symbol=NKNUSDT&side=SELL&type=STOP_MARKET&reduceOnly=true&quantity=8804&stopPrice=0.0967497903475&workingType=MARK_PRICE&recvWindow=60000&timestamp=1686578470530&signature=b3dc5c4e116a3c867d8ceb48bec371430e0af9c9276b2174d6c48d477d97e79d

        String post = sendSignatureRequest(url, query, "POST");
        log.info("挂单成功 {}", post);
        wxUtil.send("挂单成功" + "<br>symbol=" + position.getSymbol() + "<br>side=" + side + "<br>quantity=" + quantity.abs().toPlainString());
    }

    public static void main(String[] args) {

        AccountPosition position = new AccountPosition();
        position.setSymbol("NKNUSDT");
        position.setPositionAmt(new BigDecimal("8804"));

        BigDecimal pct = new BigDecimal("0.4");

        BigDecimal base = BigDecimal.ONE;
        BigDecimal holdPct = (new BigDecimal("0.001"));
        if (position.getPositionAmt().compareTo(BigDecimal.ZERO) > 0) {
            //多单
            //0.1 保本
            //0.2 保本 * 2
            //0.3 保本 * 3
            //0.4 保本 * 4
            //0.5 保本 * 10
            //0.6 保本 * 20
            //0.7 保本 * 30
            //0.8 保本 * 40
            //0.9 保本 * 50
            if (pct.compareTo(new BigDecimal(2)) > 0) {
                base = base.add(holdPct.multiply(B100));
                System.out.println(base);
            } else if (pct.compareTo(new BigDecimal(1)) > 0) {
                base = base.add(holdPct.multiply(B60));
                System.out.println(base);
            } else if (pct.compareTo(b0_9) > 0) {
                base = base.add(holdPct.multiply(B50));
                System.out.println(base);
            } else if (pct.compareTo(b0_8) > 0) {
                base = base.add(holdPct.multiply(B40));
                System.out.println(base);
            } else if (pct.compareTo(b0_7) > 0) {
                base = base.add(holdPct.multiply(B30));
                System.out.println(base);
            } else if (pct.compareTo(b0_6) > 0) {
                base = base.add(holdPct.multiply(B20));
                System.out.println(base);
            } else if (pct.compareTo(b0_5) > 0) {
                base = base.add(holdPct.multiply(B10));
                System.out.println(base);
            } else if (pct.compareTo(b0_4) > 0) {
                base = base.add(holdPct.multiply(B7));
                System.out.println(base);
            } else if (pct.compareTo(b0_3) > 0) {
                base = base.add(holdPct.multiply(B7));
                System.out.println(base);
            } else if (pct.compareTo(b0_2) > 0) {
                base = base.add(holdPct.multiply(B5));
                System.out.println(base);
            } else {
                base = base.add(holdPct.multiply(B2));
                System.out.println(base);
            }

        } else {
            //空单
            if (pct.compareTo(B2) > 0) {
                base = base.subtract(holdPct.multiply(B100));
                System.out.println(base);
            } else if (pct.compareTo(B1) > 0) {
                base = base.subtract(holdPct.multiply(B60));
                System.out.println(base);
            } else if (pct.compareTo(b0_9) > 0) {
                base = base.subtract(holdPct.multiply(B50));
                System.out.println(base);
            } else if (pct.compareTo(b0_8) > 0) {
                base = base.subtract(holdPct.multiply(B40));
                System.out.println(base);
            } else if (pct.compareTo(b0_7) > 0) {
                base = base.subtract(holdPct.multiply(B30));
                System.out.println(base);
            } else if (pct.compareTo(b0_6) > 0) {
                base = base.subtract(holdPct.multiply(B20));
                System.out.println(base);
            } else if (pct.compareTo(b0_5) > 0) {
                base = base.subtract(holdPct.multiply(B10));
                System.out.println(base);
            } else if (pct.compareTo(b0_4) > 0) {
                base = base.subtract(holdPct.multiply(B7));
                System.out.println(base);
            } else if (pct.compareTo(b0_3) > 0) {
                base = base.subtract(holdPct.multiply(B7));
                System.out.println(base);
            } else if (pct.compareTo(b0_2) > 0) {
                base = base.subtract(holdPct.multiply(B5));
                System.out.println(base);
            } else {
                base = base.subtract(holdPct.multiply(B2));
                System.out.println(base);
            }
        }

        System.out.println(base);
    }

}

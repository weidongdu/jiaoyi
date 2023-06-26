package pro.jiaoyi.bn.model;

import lombok.Data;

import java.util.List;

@Data
public class ExchangeInfo {
    private String futuresType;
    private List<RateLimit> rateLimits;
    private List<Asset> assets;
    private List<Object> exchangeFilters;
    private String timezone;
    private long serverTime;
    private List<Symbol> symbols;

    public static class RateLimit {
        private int intervalNum;
        private int limit;
        private String interval;
        private String rateLimitType;
        // getters and setters
    }

    @Data
    public static class Asset {
        private String autoAssetExchange;
        private String asset;
        private boolean marginAvailable;
        // getters and setters
    }

    @Data
    public static class Symbol {
        private String symbol;
        private String requiredMarginPercent;
        private String contractType;
        private long onboardDate;
        private String baseAsset;
        private int settlePlan;
        private String marginAsset;
        private String maintMarginPercent;
        private String marketTakeBound;
        private long deliveryDate;
        private List<String> timeInForce;
        private String quoteAsset;
        private List<String> underlyingSubType;
        private int quantityPrecision;
        private int pricePrecision;
        private int maxMoveOrderLimit;
        private List<Filter> filters;
        private int baseAssetPrecision;
        private String pair;
        private String triggerProtect;
        private int quotePrecision;
        private String underlyingType;
        private List<String> orderTypes;
        private String liquidationFee;
        private String status;
        // getters and setters
    }

    @Data
    public static class Filter {
        private String minPrice;
        private String maxPrice;
        private String tickSize;
        private String stepSize;
        private String maxQty;
        private String minQty;
        private String notional;
        private String multiplierDown;
        private String multiplierUp;
        private int multiplierDecimal;
        private int limit;
        private String filterType;
        // getters and setters
    }
    // getters and setters
}
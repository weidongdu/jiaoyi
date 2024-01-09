package pro.jiaoyi.eastm.flow.common;

public enum TradeTimeEnum {
    PRE(-1, "盘前"),
    MID(-2, "午休"),
    POST(-3, "盘后"),
    TRADE(0, "交易中");

    private int code;
    private String desc;

    TradeTimeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

}

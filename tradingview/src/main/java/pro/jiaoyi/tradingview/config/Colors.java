package pro.jiaoyi.tradingview.config;

public enum Colors {
    ZISE("#71649C","紫色"),
    RED("#EA463C","红色"),
    GREEN("#76D770","绿色");

    private String color;
    private String desc;

    Colors(String color, String desc) {
        this.color = color;
        this.desc = desc;
    }


    public String getColor() {
        return color;
    }

    public String getDesc() {
        return desc;
    }


}

package pro.jiaoyi.tradingview.config;

public enum Colors {
    ZISE("#71649C","紫色"),
    //黄色
    YELLOW("#FFFF00","黄色"),
    YELLOWT8("#FFFF00","黄色"),
    YELLOWT7("#FFFF33","黄色"),
    YELLOWT6("#FFFF66","黄色"),
    YELLOWT5("#FFFF99","黄色"),
    YELLOWT4("#FFFFCC","黄色"),

    BLUE("#00c2ff","黄色"),
    BLUET8("#0000FF","黄色"),
    BLUET7("#0066FF","黄色"),
    BLUET6("#0099FF","黄色"),
    BLUET5("#0099CC","黄色"),
    BLUET4("#00CCFF","黄色"),

    WHITE("#FFFFFF","白色"),
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

package pro.jiaoyi.tradingview.config;

public enum Colors {
    RED("DE5E57","红色"),
    GREEN("52A49A","绿色");

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

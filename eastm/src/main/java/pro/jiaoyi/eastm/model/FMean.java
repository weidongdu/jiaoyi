package pro.jiaoyi.eastm.model;

public enum FMean {
    f2("最新价"),
    f3("涨跌幅"),
    f4("涨跌额"),
    f5("成交量(手)"),
    f6("成交额"),
    f7("振幅"),
    f8("换手率"),
    f9("市盈率(动态)"),
    f10("量比"),
    f12("代码"),
    f14("名称"),
    f15("最高"),
    f16("最低"),
    f17("今开"),
    f18("昨收"),
    f22("涨速"),
    f23("市净率"),
    f100("行业板块"),
    ;

    private String mean;

    FMean(String mean) {
        this.mean = mean;
    }

    public static final String EMPTY_VALUE = "-";
    public String getMean() {
        return mean;
    }
}

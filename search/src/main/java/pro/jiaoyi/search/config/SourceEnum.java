package pro.jiaoyi.search.config;

public enum SourceEnum {
    BAIDU("百度");

    private String desc;

    SourceEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}

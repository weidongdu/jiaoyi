package pro.jiaoyi.search.config;

public enum SearchTypeEnum {
    RESULT("正文"),
    RELATED("相关搜索"),
    DROPDOWN("下拉框");

    private String desc;

    SearchTypeEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}

package pro.jiaoyi.search.config;

public enum CutSourceEnum {
    BAIDU("baidu","关键词提取"),
    JIEBA("jieba","");

    private String name;
    private String url;

    CutSourceEnum(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

}

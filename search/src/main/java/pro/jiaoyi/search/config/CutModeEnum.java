package pro.jiaoyi.search.config;

public enum CutModeEnum {

    cut_for_search("搜索引擎模式"),
    cut_all_True("全模式"),
    cut_all_False("默认模式");

    private String name;

    CutModeEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}

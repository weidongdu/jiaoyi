package pro.jiaoyi.search.config;

public enum CutModeEnum {

    cut_for_search("cut_for_search","搜索引擎模式"),
    cut_all_True("cut_all_True","全模式"),
    cut_all_False("cut_all_False","默认模式");

    private String name;
    private String desc;

    CutModeEnum(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public static CutModeEnum getByName(String name) {
        for (CutModeEnum value : CutModeEnum.values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        return null;
    }

}

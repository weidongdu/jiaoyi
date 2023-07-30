package pro.jiaoyi.eastm.config;

public enum VipIndexEnum {


    //    index_899050("0.899050", "北证50", ""),
//    index_399005("0.399005", "中小100", ""),
//    index_000010("1.000010", "上证180", ""),
//    index_000009("1.000009", "上证380", ""),
//    index_000132("1.000132", "上证100", ""),
//    index_000133("1.000133", "上证150", ""),
//    index_000003("1.000003", "Ｂ股指数", ""),
//    index_000012("1.000012", "国债指数", ""),
//    index_000013("1.000013", "企债指数", ""),
//    index_000011("1.000011", "基金指数", ""),
//    index_399002("0.399002", "深成指R", ""),
//    index_399003("0.399003", "成份Ｂ指", ""),
//    index_399106("0.399106", "深证综指", ""),
//    index_399004("0.399004", "深证100R", ""),
//    index_399007("0.399007", "深证300", ""),
//    index_399008("0.399008", "中小300", ""),
//    index_399293("0.399293", "创业大盘", ""),
//    index_399100("0.399100", "新指数", ""),
//    index_399550("0.399550", "央视50", ""),
//    index_000903("1.000903", "中证100", ""),
//    index_000906("1.000906", "中证800", ""),
    index_000001("1.000001", "上证指数", ""),
    index_399001("0.399001", "深证成指", ""),
    index_000300("1.000300", "沪深300", ""),
    index_399006("0.399006", "创业板指", ""),
    index_000016("1.000016", "上证50", ""),
    index_000905("1.000905", "中证500", ""),
    index_000688("1.000688", "科创50", ""),
    index_000852("1.000852", "中证1000", "");



    private String name;
    private String type;
    private String url;

    private String code;


    VipIndexEnum(String type, String name, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.code = "index" + type;
    }


    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getCode(){
        return code;
    }

    //get by name
    public static VipIndexEnum getByName(String name) {
        for (VipIndexEnum indexEnum : VipIndexEnum.values()) {
            if (indexEnum.getName().equals(name)) {
                return indexEnum;
            }
        }
        return null;
    }

    //get by type
    public static VipIndexEnum getByType(String type) {
        for (VipIndexEnum indexEnum : VipIndexEnum.values()) {
            if (indexEnum.getType().equals(type)) {
                return indexEnum;
            }
        }
        return null;
    }
}

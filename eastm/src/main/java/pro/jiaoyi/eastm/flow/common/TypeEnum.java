package pro.jiaoyi.eastm.flow.common;

public enum TypeEnum {

    AMT("amt", "0.成交量策略", ""),
    EM_MA_UP("east_ma_up", "0.东财多排", ""),
    MA("ma", "0.均线策略", ""),
    FENSHI("fenshi", "0.分时策略", ""),
    RANGE("range", "0.区间策略", ""),
    HIGHP("highp", "0.大涨", ""),
    FENSHI_P("fenshi_p", "0.高占比", ""),
    CYCF("cycf", "1.创业成分", "http://25.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=100&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&wbp2u=|0|0|0|web&fid=f3&fs=b:BK0638+f:!50&fields=f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f14,f15,f16,f17,f18,f22,f23,f100"),
    HS300("hs300", "2.沪深300", "http://34.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=300&po=1&np=1&fltt=2&invt=2&wbp2u=|0|0|0|web&fid=f3&fs=b:BK0500+f:!50&fields=f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f14,f15,f16,f17,f18,f22,f23,f100"),
    ZZ500("zz500", "3.中证500", "http://17.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=500&po=1&np=1&fltt=2&invt=2&wbp2u=|0|0|0|web&fid=f3&fs=b:BK0701+f:!50&fields=f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f14,f15,f16,f17,f18,f22,f23,f100"),
    ZZ1000("zz1000", "4.中证1000", "https://csi-web-dev.oss-cn-shanghai-finance-1-pub.aliyuncs.com/static/html/csindex/public/uploads/file/autofile/cons/000852cons.xls"),
    INDEX_INCLUDE("INDEX_INCLUDE", "4.指数成份(包括)", ""),
    INDEX_EXCLUDE("INDEX_EXCLUDE", "4.指数成份(不包括)", ""),
    ALL("all", "全部", "");


    private String name;
    private String type;
    private String url;


    TypeEnum(String type, String name, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
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

    //get by name
    public static TypeEnum getByName(String name) {
        for (TypeEnum indexEnum : TypeEnum.values()) {
            if (indexEnum.getName().equals(name)) {
                return indexEnum;
            }
        }
        return null;
    }

    //get by type
    public static TypeEnum getByType(String type) {
        for (TypeEnum indexEnum : TypeEnum.values()) {
            if (indexEnum.getType().equals(type)) {
                return indexEnum;
            }
        }
        return null;
    }
}

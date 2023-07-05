package pro.jiaoyi.eastm.config;

public enum IndexEnum {


    BIXUAN("BIXUAN", "0.必选", ""),
    CYCF("cycf", "1.创业成分", "http://25.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=100&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&wbp2u=|0|0|0|web&fid=f3&fs=b:BK0638+f:!50&fields=f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f14,f15,f16,f17,f18,f22,f23,f100"),
    HS300("hs300", "2.沪深300", "http://34.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=300&po=1&np=1&fltt=2&invt=2&wbp2u=|0|0|0|web&fid=f3&fs=b:BK0500+f:!50&fields=f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f14,f15,f16,f17,f18,f22,f23,f100"),
    ZZ500("zz500", "3.中证500", "http://17.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=500&po=1&np=1&fltt=2&invt=2&wbp2u=|0|0|0|web&fid=f3&fs=b:BK0701+f:!50&fields=f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f14,f15,f16,f17,f18,f22,f23,f100"),
    ZZ1000("zz1000", "4.中证1000", "https://csi-web-dev.oss-cn-shanghai-finance-1-pub.aliyuncs.com/static/html/csindex/public/uploads/file/autofile/cons/000852cons.xls"),
    IndexAll_Filter("indexAll", "4.指数成份(过滤)", ""),
    IndexAll_Component("IndexAll_Filter", "4.指数成份(全)", ""),
    O_TP02("tp02", "5.2个点之上", ""),
    O_TP7("tp7", "6.7个点之上", ""),
    OPEN_HIGH("open_high", "高开0.5-3", ""),
    ALL("all", "全部", ""),
    O_BK("bk", "板块", "http://92.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=100&po=1&np=1&fltt=2&invt=2&wbp2u=6502094531899276|0|1|0|web&fid=f3&fs=m:90+t:2+f:!50&fields=f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f14,f15,f16,f17,f18,f22,f23,f100"),
    O_TAMT60("tamt60", "7.成交额超过amt60", "");


    private String name;
    private String type;
    private String url;


    IndexEnum(String type, String name, String url) {
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
    public static IndexEnum getByName(String name) {
        for (IndexEnum indexEnum : IndexEnum.values()) {
            if (indexEnum.getName().equals(name)) {
                return indexEnum;
            }
        }
        return null;
    }

    //get by type
    public static IndexEnum getByType(String type) {
        for (IndexEnum indexEnum : IndexEnum.values()) {
            if (indexEnum.getType().equals(type)) {
                return indexEnum;
            }
        }
        return null;
    }
}

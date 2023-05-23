package pro.jiaoyi.tushare.model.stockbasic;

import lombok.Data;

@Data
public class StockBasic {
    //["ts_code","symbol","name","area","industry","market","list_date"],

    private String ts_code;
    private String symbol;
    private String name;
    private String area;
    private String industry;
    private String market;
    private String list_date;
}

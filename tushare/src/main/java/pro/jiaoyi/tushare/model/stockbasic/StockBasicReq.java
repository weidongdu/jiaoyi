package pro.jiaoyi.tushare.model.stockbasic;

import lombok.Data;

@Data
public class StockBasicReq {
    /*
    is_hs	    str	N	是否沪深港通标的，N否 H沪股通 S深股通
    list_status	str	N	上市状态 L上市 D退市 P暂停上市，默认是L
    exchange	str	N	交易所 SSE上交所 SZSE深交所 BSE北交所
    ts_code	    str	N	TS股票代码
    market	    str	N	市场类别 （主板/创业板/科创板/CDR/北交所）
    limit	    int	N
    offset	    int	N
    name	    str	N	名称
     */
    private String is_hs;
    private String list_status;
    private String exchange;
    private String ts_code;
    private String market;
    private int limit;
    private int offset;
    private String name;
}

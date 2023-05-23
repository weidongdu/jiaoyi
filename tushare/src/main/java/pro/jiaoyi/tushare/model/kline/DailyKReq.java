package pro.jiaoyi.tushare.model.kline;

import lombok.Data;

/*
输入参数

名称	类型	必选	描述
ts_code 	str	N	股票代码（支持多个股票同时提取，逗号分隔）
trade_date	str	N	交易日期（YYYYMMDD）
start_date	str	N	开始日期(YYYYMMDD)
end_date	str	N	结束日期(YYYYMMDD)
注：日期都填YYYYMMDD格式，比如20181010
 */
@Data
public class DailyKReq {
    private String ts_code;//股票代码
    private String trade_date;//交易日期
    private String start_date;//开始日期
    private String end_date;//结束日期
    private String offset;//开始行数
    private String limit;//最大行数

}

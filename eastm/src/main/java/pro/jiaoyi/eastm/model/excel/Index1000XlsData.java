package pro.jiaoyi.eastm.model.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class Index1000XlsData {
    // 0 日期Date
    // 1 指数代码 Index Code
    // 2 指数名称 Index Name
    // 3 指数英文名称Index Name(Eng)
    // 4 成分券代码Constituent Code
    // 5 成分券名称Constituent Name
    // 6 成分券英文名称Constituent Name(Eng)
    // 7 交易所Exchange
    // 8 交易所英文名称Exchange(Eng)
    @ExcelProperty(index = 4)
    private String code;
    @ExcelProperty(index = 5)
    private String name;



}

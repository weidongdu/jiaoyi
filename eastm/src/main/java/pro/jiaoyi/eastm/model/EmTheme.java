package pro.jiaoyi.eastm.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/*
{
    "version": "1b814dce77fe573cea891870538bcee1",
    "result": {
        "pages": 1,
        "data": [
            {
                "SECUCODE": "002584.SZ",
                "SECURITY_CODE": "002584",
                "SECURITY_NAME_ABBR": "西陇科学",
                "NEW_BOARD_CODE": "BK1062",
                "BOARD_NAME": "新冠检测",
                "SELECTED_BOARD_REASON": "2022年1月13日回复称公司旗下艾克韦生物公司研发的新冠检测试剂盒目前在国内销售。",
                "IS_PRECISE": "1",
                "BOARD_RANK": 5,
                "BOARD_YIELD": 0.44,
                "DERIVE_BOARD_CODE": "BI1062"
            }
        ],
        "count": 18
    },
    "success": true,
    "message": "ok",
    "code": 0
}
 */
@Data
public class EmTheme {
    //定义数据模型
    private String version;
    private Result result;
    private boolean success;
    private String message;
    private int code;

    @Data
    public static class Result {
        private int pages;
        private List<Theme> data;
        private int count;
    }

    @Data
    public static class Theme {
        private String SECUCODE;
        private String SECURITY_CODE;
        private String SECURITY_NAME_ABBR;
        private String NEW_BOARD_CODE;
        private String BOARD_NAME;
        private String SELECTED_BOARD_REASON;
        private String IS_PRECISE;
        private int BOARD_RANK;
        private BigDecimal BOARD_YIELD;
        private String DERIVE_BOARD_CODE;
    }
}

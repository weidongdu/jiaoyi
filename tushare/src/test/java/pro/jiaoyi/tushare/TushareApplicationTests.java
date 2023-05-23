package pro.jiaoyi.tushare;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.tushare.config.TsClient;

import java.util.Map;

@SpringBootTest
class TushareApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private TsClient tsClient;
    @Test
    public void testTsClient() {

//        List<StockBasicResp> list = JSON.parseArray(null, StockBasicResp.class);
//        System.out.println(list);

//        List<StockBasic> list =
//                tsClient.getStockBasicList();
//
//        System.out.println(list);
//
//
//        Map<String, String> tsCodeNameMap = tsClient.tsCodeNameMap(true);
//        System.out.println(tsCodeNameMap);
//
        Map<String, String> map = tsClient.nameTsCodeMap(true);
        System.out.println(map);
    }
}

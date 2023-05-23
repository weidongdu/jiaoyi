package pro.jiaoyi.tushare;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.tushare.config.TuShareClient;
import pro.jiaoyi.tushare.model.kline.DailyK;
import pro.jiaoyi.tushare.model.kline.DailyKReq;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
class TushareApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private TuShareClient tsClient;
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
//        Map<String, String> map = tsClient.nameTsCodeMap(true);
//        System.out.println(map);

        DailyKReq req = new DailyKReq();
        req.setTrade_date(LocalDate.now().toString().replaceAll("-",""));
        List<DailyK> dailyKS =
                tsClient.dailyKs(new DailyKReq());

        System.out.println(dailyKS);
    }
}

package pro.jiaoyi.tradingview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.tradingview.service.TvService;
import pro.jiaoyi.tradingview.service.TvTransUtil;

import java.util.List;
import java.util.Map;

@SpringBootTest
class TradingviewApplicationTests {

    @Test
    void contextLoads() {
    }


    @Autowired
    private TvTransUtil tvTransUtil;
    @Autowired
    private EmClient emClient;

    @Autowired
    private TvService tvService;

    @Test
    void test() {
//        List<EmDailyK> dailyKs = emClient.getDailyKs("002422", LocalDate.now(), 500, false);
//        TvChart tvChart = tvTransUtil.tranEmDailyKLineToTv(dailyKs);
//        System.out.println(tvChart);

//        tvService.getLists("hs300",false).forEach(System.out::println);

        Map<String, List<String>> allIndex =
                tvService.getAllIndex();

        allIndex.forEach((t,list)->{
            System.out.println(t + " "+ list);
        });
    }


}

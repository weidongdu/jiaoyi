package pro.jiaoyi.tradingview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.tradingview.model.TvChart;
import pro.jiaoyi.tradingview.service.TvTransUtil;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
class TradingviewApplicationTests {

    @Test
    void contextLoads() {
    }


    @Autowired
    private TvTransUtil tvTransUtil;
    @Autowired
    private EmClient emClient;
    @Test
    void test() {
        List<EmDailyK> dailyKs = emClient.getDailyKs("002422", LocalDate.now(), 500);
        TvChart tvChart = tvTransUtil.tranEmDailyKLineToTv(dailyKs);
        System.out.println(tvChart);
    }


}

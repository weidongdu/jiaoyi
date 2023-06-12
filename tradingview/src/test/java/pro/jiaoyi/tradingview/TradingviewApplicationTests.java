package pro.jiaoyi.tradingview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.tradingview.model.TvChart;
import pro.jiaoyi.tradingview.service.TvService;
import pro.jiaoyi.tradingview.service.TvTransUtil;

import java.time.LocalDate;
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
                tvService.getAllIndex(false);

        allIndex.forEach((t,list)->{
            System.out.println(t + " "+ list);
        });
    }


    /**
     * 回测数据 指定之日 指定向前数量
     */

    public void backTest(){
        String code = "600283";
        String date = "20230525";

        int preDays = 60;

        TvChart tvChart = tvService.getTvChart(code, LocalDate.now(), 500);

    }

}

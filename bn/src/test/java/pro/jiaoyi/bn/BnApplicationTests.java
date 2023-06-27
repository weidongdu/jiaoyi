package pro.jiaoyi.bn;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.bn.model.BnK;
import pro.jiaoyi.bn.sdk.FutureApi;
import pro.jiaoyi.common.strategy.BreakOutStrategy;

import java.util.List;

@SpringBootTest
class BnApplicationTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private FutureApi futureApi;

    @Test
    public void test() {
//        futureApi.fundingRate("BTCUSDT");
        List<BnK> kavausdt = futureApi.kline("KAVAUSDT", "5m", 1000);

        for (int i = 0; i < kavausdt.size(); i++) {
            int end = i + 250;
            if (end == kavausdt.size() - 1) {
                break;
            }

            int days = 60;
//            boolean b = BreakOutStrategy.breakOut(kavausdt.subList(i, end), days, days, days, 0.4f);
//            if (b){
//                System.out.println("break out");
//            }
        }

    }
}

package pro.jiaoyi.bn;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.bn.sdk.FutureApi;

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
//        List<String> symbols
//                = futureApi.getExchangeInfo();
//        System.out.println(symbols);
//
        futureApi.fundingRate("BTCUSDT");

    }
}

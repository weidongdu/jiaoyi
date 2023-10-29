package pro.jiaoyi.bn;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.bn.model.trade.AccountPosition;
import pro.jiaoyi.bn.service.BnAccountTradeService;

import java.util.List;

@SpringBootTest
class BnTradeTests {

    @Test
    void contextLoads() {
    }



    @Resource
    private BnAccountTradeService bnAccountTradeService;


    @Test
    public void trade() throws Exception {
        String s = bnAccountTradeService.accountInfoV2();
        System.out.println(s);

        List<AccountPosition> accountPositions = bnAccountTradeService.positionList();
        System.out.println(accountPositions);

    }
}

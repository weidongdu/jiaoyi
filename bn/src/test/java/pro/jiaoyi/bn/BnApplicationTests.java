package pro.jiaoyi.bn;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.bn.controller.FrController;
import pro.jiaoyi.bn.dao.entity.PremiumIndexEntity;
import pro.jiaoyi.bn.dao.repo.PremiumIndexRepo;
import pro.jiaoyi.bn.model.BnK;
import pro.jiaoyi.bn.sdk.FutureApi;
import pro.jiaoyi.bn.service.BnPremiumIndexService;
import pro.jiaoyi.common.strategy.BreakOutStrategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@SpringBootTest
class BnApplicationTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private FutureApi futureApi;

    @Resource
    private PremiumIndexRepo premiumIndexRepo;

    @Resource
    private BnPremiumIndexService bnPremiumIndexService;

    @Resource
    private FrController frController;
    @Test
    public void test() {
//        bnPremiumIndexService.deleteByDays(0);

//        for (int i = 0; i < 5; i++) {
//            bnPremiumIndexService.savePremiumIndex();
//        }

        Map<String, BigDecimal> fr =
                frController.getFr();
        System.out.println(fr);


//        List<PremiumIndexEntity> list = futureApi.premiumIndex();
//        System.out.println(list.get(0));
//        premiumIndexRepo.save(list.get(0));



//        futureApi.fundingRate("BNTUSDT");
//        List<BnK> kavausdt = futureApi.kline("KAVAUSDT", "5m", 1000);
//
//        for (int i = 0; i < kavausdt.size(); i++) {
//            int end = i + 250;
//            if (end == kavausdt.size() - 1) {
//                break;
//            }
//
//            int days = 60;
////            boolean b = BreakOutStrategy.breakOut(kavausdt.subList(i, end), days, days, days, 0.4f);
////            if (b){
////                System.out.println("break out");
////            }
//        }

    }

    @Test
    public void oi(){
        futureApi.getOI("BTCUSDT");
    }
}

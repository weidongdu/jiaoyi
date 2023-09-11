package pro.jiaoyi.eastm;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.dao.entity.KLineEntity;
import pro.jiaoyi.eastm.dao.entity.UserEntity;
import pro.jiaoyi.eastm.dao.repo.KLineRepo;
import pro.jiaoyi.eastm.dao.repo.UserRepo;
import pro.jiaoyi.eastm.model.EmCList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@Slf4j
class EastmApplicationDaoTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private EmClient emClient;

    @Autowired
    private KLineRepo kLineRepo;

    @Test
    public void userTest() throws InterruptedException {
        FileUtil.writeToFile("low.csv", "code" + "," +"name" + "," + "k0.td" + "," + "k0amt" + "," + "k0close" + "," + "间隔" + "," + "kk.td" + "," + "kk.high" + "," + "最高涨幅" + "," + "成交量比" + "," + "kk成交量" + "\n");

        //设置一个线程池
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        //设置策略

        List<EmCList> emCLists = emClient.getClistDefaultSize(false);
//        List<EmCList> emCLists = new ArrayList<>();
//        EmCList emCList = new EmCList();
//        emCList.setF12Code("601336");
//        emCList.setF14Name("新华保险");
//        emCLists.add(emCList);


        for (EmCList emc : emCLists) {
            //多线程处理
            executorService.execute(() -> {
                try {
                    run(emc);
                } catch (Exception e) {
                    log.error("error run again", e);
                    run(emc);

                }
            });
        }

        Thread.sleep(1000000000);
        executorService.shutdown();
    }

    private void run(EmCList emc) {
        log.info("run code={}",emc.getF12Code());
        List<KLineEntity> list = kLineRepo.findByCode(emc.getF12Code());
        if (list.size() == 0) {
            return;
        }

        for (int i = 2; i < list.size(); i++) {
            KLineEntity k0 = list.get(i);
            if (       k0.getMa5().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa10().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa20().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa30().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa60().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa120().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa250().compareTo(BigDecimal.ZERO) <= 0

                    || k0.getClose().compareTo(k0.getMa5()) < 0
                    || k0.getClose().compareTo(k0.getMa10()) < 0
                    || k0.getClose().compareTo(k0.getMa20()) < 0
                    || k0.getClose().compareTo(k0.getMa30()) < 0
                    || k0.getClose().compareTo(k0.getMa60()) < 0
                    || k0.getClose().compareTo(k0.getMa120()) < 0
                    || k0.getClose().compareTo(k0.getMa250()) < 0
            ) {
                continue;
            }


            KLineEntity k1 = list.get(i - 1);
            KLineEntity k2 = list.get(i - 2);

            if (k2.getHigh().compareTo(k1.getHigh()) > 0
                    && k2.getLow().compareTo(k1.getLow()) > 0
                    && k2.getAmt().compareTo(k1.getAmt()) > 0

                    && k1.getHigh().compareTo(k0.getHigh()) > 0
                    && k1.getLow().compareTo(k0.getLow()) > 0
                    && k1.getAmt().compareTo(k0.getAmt()) > 0
            ) {
                log.info("{}-{},p={},td={},amt={}", k0.getCode(), k0.getName(), k0.getClose(), k0.getTradeDate(), BDUtil.amtHuman(k0.getAmt()));
                //kan后面5天的涨幅
                for (int j = 2; j <= 5; j++) {
                    if (i + j > list.size() - 1) {
                        break;
                    }
                    KLineEntity kk = list.get(i + j);
                    BigDecimal rate = kk.getHigh().subtract(k0.getClose()).divide(k0.getClose(), 4, RoundingMode.HALF_UP);
                    BigDecimal amtRate = kk.getAmt().subtract(k0.getAmt()).divide(k0.getAmt(), 4, RoundingMode.HALF_UP);
                    log.info("code={},date={},rate={},amtRate={}", kk.getCode(), kk.getTradeDate(), BDUtil.p100(rate), BDUtil.p100(amtRate));

                    FileUtil.writeToFile("low.csv", k0.getCode() + "," + k0.getName() + "," + k0.getTradeDate() + "," + BDUtil.amtHuman(k0.getAmt()) + "," + k0.getClose() + "," + j + "," + kk.getTradeDate() + "," + kk.getHigh() + "," + BDUtil.p100(rate) + "," + BDUtil.p100(amtRate) + "," + BDUtil.amtHuman(k0.getAmt()) + "\n");
                }
            }
        }
    }

}

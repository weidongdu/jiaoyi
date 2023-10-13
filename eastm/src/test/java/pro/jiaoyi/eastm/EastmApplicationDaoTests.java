package pro.jiaoyi.eastm;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.dao.entity.KLineEntity;
import pro.jiaoyi.eastm.dao.repo.KLineRepo;
import pro.jiaoyi.eastm.model.EmCList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
//        FileUtil.writeToFile("low.csv", "code" + "," + "name" + "," + "k0.td" + "," + "k0amt" + "," + "k0close" + "," + "间隔" + "," + "kk.td" + "," + "kk.high" + "," + "最高涨幅" + "," + "成交量比" + "," + "kk成交量" + "\n");

        //设置一个线程池
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        //设置策略

        List<EmCList> emCLists = emClient.getClistDefaultSize(false);
//        List<EmCList> emCLists = new ArrayList<>();
//        EmCList emCList = new EmCList();
//        emCList.setF12Code("603087");
//        emCList.setF14Name("甘李药业");
//        emCLists.add(emCList);


        for (EmCList emc : emCLists) {
            //多线程处理
            executorService.execute(() -> {
                try {
                    runDefault(emc);
                } catch (Exception e) {
                    log.error("error run again", e);
                    runDefault(emc);
                }
            });
        }

        Thread.sleep(1000 * 60 * 60);
        executorService.shutdown();
    }

    private void runDefault(EmCList emc) {
        run(emc, 300, 3, BDUtil.B1Y, BDUtil.B10);
    }

    /**
     * @param days   单个code 从今天往前推多少个 start=size - (1+window) 天
     * @param window 连续满足条件的天数
     * @param minAmt 最小成交量
     */

    private void run(EmCList emc, int days, int window, BigDecimal minAmt, BigDecimal price) {
        log.info("run code={}", emc.getF12Code());
        List<KLineEntity> list = kLineRepo.findByCodeLimit5(emc.getF12Code(), DateUtil.toTimestamp(LocalDate.now().minusDays(7 + days)));
        if (list.size() == 0) {
            return;
        }

        for (int i = Math.max(0, list.size() - 1 - days); i < list.size(); i++) {
            KLineEntity k0 = list.get(i);
            if (k0.getAmt().compareTo(minAmt) < 0 || k0.getClose().compareTo(price) < 0) {
                continue;
            }

            if (k0.getMa5().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa10().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa20().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa30().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa60().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa120().compareTo(BigDecimal.ZERO) <= 0
                    || k0.getMa250().compareTo(BigDecimal.ZERO) <= 0

                    || k0.getClose().compareTo(k0.getMa5()) <= 0
                    || k0.getClose().compareTo(k0.getMa10()) <= 0
                    || k0.getClose().compareTo(k0.getMa20()) <= 0
                    || k0.getClose().compareTo(k0.getMa30()) <= 0
                    || k0.getClose().compareTo(k0.getMa60()) <= 0
                    || k0.getClose().compareTo(k0.getMa120()) <= 0
                    || k0.getClose().compareTo(k0.getMa250()) <= 0
            ) {
                continue;
            }


            AtomicInteger count = new AtomicInteger(0);
            for (int j = 1; j < window; j++) {


                if (i - j < 0) {
                    break;
                }
                KLineEntity k = list.get(i - j + 1);
                KLineEntity k1 = list.get(i - j);

                if (j == 1) {
                    //要求均线多头排列
                    if (k1.getMa5().compareTo(k.getMa5()) > 0
                            || k1.getMa10().compareTo(k.getMa10()) > 0
                            || k1.getMa20().compareTo(k.getMa20()) > 0
                            || k1.getMa30().compareTo(k.getMa30()) > 0
                            || k1.getMa60().compareTo(k.getMa60()) > 0
                            || k1.getMa120().compareTo(k.getMa120()) > 0
                            || k1.getMa250().compareTo(k.getMa250()) > 0
                    ) {
                        continue;
                    }
                }

                if (k1.getHigh().compareTo(k.getHigh()) > 0
                        && k1.getLow().compareTo(k.getLow()) > 0
                        && k1.getAmt().compareTo(k.getAmt()) > 0) {
                    count.incrementAndGet();
                }
            }


            if (count.get() == window - 1) {
                log.info("[low3]{}-{},p={},td={},amt={}", k0.getCode(), k0.getName(), k0.getClose(), k0.getTradeDate(), BDUtil.amtHuman(k0.getAmt()));
                //kan后面5天的涨幅
                for (int j = 2; j <= 5; j++) {
                    if (i + j > list.size() - 1) {
                        break;
                    }
                    KLineEntity kk = list.get(i + j);
                    BigDecimal rate = kk.getHigh().subtract(k0.getClose()).divide(k0.getClose(), 4, RoundingMode.HALF_UP);
                    BigDecimal amtRate = kk.getAmt().subtract(k0.getAmt()).divide(k0.getAmt(), 4, RoundingMode.HALF_UP);
                    log.info("后{} code={},date={},rate={},amtRate={}", j, kk.getCode(), kk.getTradeDate(), BDUtil.p100(rate), BDUtil.p100(amtRate));

//                    FileUtil.writeToFile("low.csv", k0.getCode() + "," + k0.getName() + "," + k0.getTradeDate() + "," + BDUtil.amtHuman(k0.getAmt()) + "," + k0.getClose() + "," + j + "," + kk.getTradeDate() + "," + kk.getHigh() + "," + BDUtil.p100(rate) + "," + BDUtil.p100(amtRate) + "," + BDUtil.amtHuman(k0.getAmt()) + "\n");
                }
            }
        }
    }

}

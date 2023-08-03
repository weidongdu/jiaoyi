package pro.jiaoyi.tradingview;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.dao.entity.KLineEntity;
import pro.jiaoyi.eastm.dao.repo.KLineRepo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.tradingview.model.TvChart;
import pro.jiaoyi.tradingview.service.TvService;
import pro.jiaoyi.tradingview.service.TvTransUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
@Slf4j
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

        allIndex.forEach((t, list) -> {
            System.out.println(t + " " + list);
        });
    }


    /**
     * 回测数据 指定之日 指定向前数量
     */

    public void backTest() {
        String code = "600283";
        String date = "20230525";

        int preDays = 60;

        TvChart tvChart = tvService.getTvChart(code, LocalDate.now(), 500);

    }

    @Test
    public void getAll() {
        List<EmCList> list = emClient.getClistDefaultSize(false);

        LocalDate end = LocalDate.now();
        long endTs = DateUtil.toTimestamp(end);

        for (EmCList emCList : list) {
            List<EmDailyK> dailyKs = emClient.getDailyKs(emCList.getF12Code(), LocalDate.now(), 500, false);
            int size = dailyKs.size();
            if (size == 0) {
                continue;
            }

            int last = size - 1;
            if (dailyKs.get(last).getTsOpen() != endTs) {
                log.warn("停牌 {}", dailyKs.get(last));
                continue;
            }

            market(dailyKs);
        }

    }


    @Test
    public void getAll_x10() {
        List<EmCList> list = emClient.getClistDefaultSize(false);

        for (EmCList emCList : list) {
            List<EmDailyK> ks = emClient.getDailyKs(emCList.getF12Code(), LocalDate.now(), 500, false);

            if (ks.size() < 100 ) {
                continue;
            }

            int lastIndex = ks.size() - 1;
            EmDailyK lastK = ks.get(lastIndex);
            LocalDate localDate = DateUtil.tsToLocalDate(lastK.getTsOpen());
            if (localDate.isBefore(LocalDate.now())) {
                continue;
            }

            BigDecimal[] amtArr = ks.stream().map(EmDailyK::getAmt).toList().toArray(new BigDecimal[0]);
            BigDecimal[] ma60 = MaUtil.ma(60, amtArr, 2);
            if (lastK.getAmt().compareTo(BDUtil.B10.multiply(ma60[lastIndex])) > 0){
                System.out.println("x10=" + lastK.getTradeDate()+ " " + emCList.getF12Code() + " " + emCList.getF14Name() + " " + BDUtil.amtHuman(lastK.getAmt()) + " " + BDUtil.amtHuman(ma60[lastIndex]));
            }

        }

    }


    @Autowired
    private KLineRepo kLineRepo;
    private void market(List<EmDailyK> dailyKs) {

        BigDecimal[] amtArr = dailyKs.stream().map(EmDailyK::getAmt).toList().toArray(new BigDecimal[0]);
        BigDecimal[] amtArr_ma5 = MaUtil.ma(5, amtArr, 3);
        BigDecimal[] amtArr_ma10 = MaUtil.ma(10, amtArr, 3);
        BigDecimal[] amtArr_ma20 = MaUtil.ma(20, amtArr, 3);
        BigDecimal[] amtArr_ma30 = MaUtil.ma(30, amtArr, 3);
        BigDecimal[] amtArr_ma60 = MaUtil.ma(60, amtArr, 3);
        BigDecimal[] amtArr_ma120 = MaUtil.ma(120, amtArr, 3);
        BigDecimal[] amtArr_ma250 = MaUtil.ma(250, amtArr, 3);

        BigDecimal[] closeArr = dailyKs.stream().map(EmDailyK::getClose).toList().toArray(new BigDecimal[0]);
        BigDecimal[] ma5 = MaUtil.ma(5, closeArr, 3);
        BigDecimal[] ma10 = MaUtil.ma(10, closeArr, 3);
        BigDecimal[] ma20 = MaUtil.ma(20, closeArr, 3);
        BigDecimal[] ma30 = MaUtil.ma(30, closeArr, 3);
        BigDecimal[] ma60 = MaUtil.ma(60, closeArr, 3);
        BigDecimal[] ma120 = MaUtil.ma(120, closeArr, 3);
        BigDecimal[] ma250 = MaUtil.ma(250, closeArr, 3);


        ArrayList<KLineEntity> list = new ArrayList<>(dailyKs.size());
        for (int i = 0; i < dailyKs.size(); i++) {
            EmDailyK dk = dailyKs.get(i);
            String s = JSON.toJSONString(dk);
            KLineEntity entity = JSON.toJavaObject(JSON.parseObject(s), KLineEntity.class);
            list.add(entity);


            entity.setTradeDate(DateUtil.strToLocalDate(dk.getTradeDate(), DateUtil.PATTERN_yyyyMMdd));
            entity.setTradeDateStr(dk.getTradeDate());

            entity.setMa5(ma5[i]);
            entity.setMa10(ma10[i]);
            entity.setMa20(ma20[i]);
            entity.setMa30(ma30[i]);
            entity.setMa60(ma60[i]);
            entity.setMa120(ma120[i]);
            entity.setMa250(ma250[i]);

            entity.setVma5(amtArr_ma5[i]);
            entity.setVma10(amtArr_ma10[i]);
            entity.setVma20(amtArr_ma20[i]);
            entity.setVma30(amtArr_ma30[i]);
            entity.setVma60(amtArr_ma60[i]);
            entity.setVma120(amtArr_ma120[i]);
            entity.setVma250(amtArr_ma250[i]);

            if (i < 250) {
                entity.setVl5(BDUtil.BN1);
                entity.setVl10(BDUtil.BN1);
                entity.setVl20(BDUtil.BN1);
                entity.setVl30(BDUtil.BN1);
                entity.setVl60(BDUtil.BN1);
                entity.setVl120(BDUtil.BN1);
                entity.setVl250(BDUtil.BN1);
            } else {
                BigDecimal amt = entity.getAmt();
                List<BigDecimal> l5 = dailyKs.subList(i+1 - 5, i+1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l10 = dailyKs.subList(i+1 - 10, i+1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l20 = dailyKs.subList(i+1 - 20, i+1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l30 = dailyKs.subList(i+1 - 30, i+1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l60 = dailyKs.subList(i+1 - 60, i+1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l120 = dailyKs.subList(i+1 - 120, i+1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l250 = dailyKs.subList(i+1 - 250, i+1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);

                entity.setVl5(new BigDecimal(l5.indexOf(amt)).divide(new BigDecimal(l5.size() - 1), 4, RoundingMode.HALF_UP));
                entity.setVl10(new BigDecimal(l10.indexOf(amt)).divide(new BigDecimal(l10.size() - 1), 4, RoundingMode.HALF_UP));
                entity.setVl20(new BigDecimal(l20.indexOf(amt)).divide(new BigDecimal(l20.size() - 1), 4, RoundingMode.HALF_UP));
                entity.setVl30(new BigDecimal(l30.indexOf(amt)).divide(new BigDecimal(l30.size() - 1), 4, RoundingMode.HALF_UP));
                entity.setVl60(new BigDecimal(l60.indexOf(amt)).divide(new BigDecimal(l60.size() - 1), 4, RoundingMode.HALF_UP));
                entity.setVl120(new BigDecimal(l120.indexOf(amt)).divide(new BigDecimal(l120.size() - 1), 4, RoundingMode.HALF_UP));
                entity.setVl250(new BigDecimal(l250.indexOf(amt)).divide(new BigDecimal(l250.size() - 1), 4, RoundingMode.HALF_UP));

            }

            log.debug("entity {}", entity);

        }
        kLineRepo.saveAll(list);
    }


}

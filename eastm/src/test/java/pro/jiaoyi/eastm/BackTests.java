package pro.jiaoyi.eastm;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.dao.entity.EmDailyKEntity;
import pro.jiaoyi.eastm.dao.repo.EmDailyKRepo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@Slf4j
class BackTests {

    @Test
    void contextLoads() {
    }


    @Autowired
    private EmDailyKRepo emDailyKRepo;
    @Autowired
    private EmClient emClient;


    /**
     * 涨停板 低换手
     */
    @Test
    public void highStop_lowHsl() {
        List<EmCList> list = emClient.getClistDefaultSize(false);

        for (EmCList emCList : list) {
            String code = emCList.getF12Code();
            String name = emCList.getF14Name();

            //先查db
            List<EmDailyK> ks = getFromDb(code);

            for (int i = 60; i < ks.size() - 5; i++) {
                EmDailyK k_1 = ks.get(i - 1);

                EmDailyK k = ks.get(i);

                if (k.getOpen().compareTo(k.getClose()) == 0
                        && k.getOpen().compareTo(k.getHigh()) == 0) {

                } else {
                    continue;
                }

                EmDailyK k1 = ks.get(i + 1);
                EmDailyK k2 = ks.get(i + 2);
                EmDailyK k3 = ks.get(i + 3);
                EmDailyK k4 = ks.get(i + 4);
                EmDailyK k5 = ks.get(i + 5);

                if (k1.getOpen().compareTo(BigDecimal.ZERO) <= 0 ||
                        k.getClose().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                //hsl > 3  pass
                if (k.getHsl().compareTo(BDUtil.B3) > 0) {
                    continue;
                }

                //hsl > pre hsl , pass
                if (k.getHsl().compareTo(k_1.getHsl()) > 0) {
                    continue;
                }


                BigDecimal f = new BigDecimal("1.1");
                if (code.startsWith("68") || code.startsWith("69") || code.startsWith("30")) {
                    f = new BigDecimal("1.2");
                }
                if (code.startsWith("8") || code.startsWith("4")) {
                    f = new BigDecimal("1.3");
                }

                BigDecimal stopPrice = k.getPreClose().multiply(f).setScale(2, RoundingMode.HALF_UP);
                //not涨停 pass
                if (!(k.getPct().compareTo(BDUtil.B9) > 0
                        && k.getClose().compareTo(stopPrice) >= 0)) {
                    continue;
                }

                //满足 涨停低换手 条件
                //判断k1 , k1不能开盘涨停

                BigDecimal k1OpenPct = k1.getOpen().subtract(k1.getPreClose()).divide(k1.getPreClose(), 4, RoundingMode.HALF_UP);
                if (k1OpenPct.compareTo(new BigDecimal("0.095")) > 0) {
                    //k1开盘涨停 pass
                    continue;
                }
                //满足 涨停低换手 条件 , k1开盘不涨停
                boolean up60 = true;
                BigDecimal buy = k1.getOpen();
                for (int j = 1; j < 60; j++) {
                    int index = i - j;
                    if (buy.compareTo(ks.get(index).getHigh()) < 0) {
                        up60 = false;
                        break;
                    }
                }

                if (!up60) {
                    continue;
                }

                BigDecimal buyPct = buy.subtract(k.getClose()).divide(k.getClose(), 4, RoundingMode.HALF_UP);


                BigDecimal k2Pct = k2.getHigh().subtract(buy).divide(buy, 4, RoundingMode.HALF_UP);
                BigDecimal k3Pct = k3.getHigh().subtract(buy).divide(buy, 4, RoundingMode.HALF_UP);
                BigDecimal k4Pct = k4.getHigh().subtract(buy).divide(buy, 4, RoundingMode.HALF_UP);
                BigDecimal k5Pct = k5.getHigh().subtract(buy).divide(buy, 4, RoundingMode.HALF_UP);

                BigDecimal pctH = BigDecimal.ZERO;
                for (int j = 1; j < 10; j++) {
                    int index = i - j;
                    EmDailyK k_i = ks.get(index);
                    BigDecimal p = buy.subtract(k_i.getClose()).divide(k_i.getClose(), 4, RoundingMode.HALF_UP);
                    if (p.compareTo(pctH) > 0) {
                        pctH = p;
                    }
                }

                log.info("code={} ,pre_HSL={} ,td_HSL={}, buy day={}, buy={}, buyPct={},k10={}, k2Pct={}, k3Pct={}, k4Pct={}, k5Pct={}",
                        code + name, k_1.getHsl(), k.getHsl(), k1.getTradeDate(), k1.getClose(), BDUtil.p100(buyPct), BDUtil.p100(pctH), BDUtil.p100(k2Pct), BDUtil.p100(k3Pct), BDUtil.p100(k4Pct), BDUtil.p100(k5Pct));

            }

        }


    }


    public List<EmDailyK> getFromDb(String code) {

        List<EmDailyKEntity> dbs = emDailyKRepo.findByCode(code);

        List<EmDailyK> ks = new ArrayList<>();
        for (EmDailyKEntity db : dbs) {
            EmDailyK k = new EmDailyK();
            BeanUtils.copyProperties(db, k);
            ks.add(k);
        }

        return ks;
    }


    /**
     * 区间测量
     */
    @Test
    public void rangePct() {
//        List<EmCList> list = emClient.getClistDefaultSize(false);
//        for (EmCList em : list) {
//            String code = em.getF12Code();
//            rangePct(code);
//        }
//        rangePct("601985");
//        rangePct("688981");
        rangePct("002432");
    }

    public void rangePct(String code) {


//        String code = "600985";

        //先查db
//        List<EmDailyK> ks = getFromDb(code);
        List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now().minusDays(1), 500, true);

        if (ks.size() < 260) return;

        Map<String, BigDecimal> rangeMap = new HashMap<>();
        //倒序
        String startTd = "";
        int range = 0;
        BigDecimal hh = BigDecimal.ZERO;
        BigDecimal ll = BigDecimal.ZERO;
        BigDecimal cc = BigDecimal.ZERO;
        BigDecimal oscc = BigDecimal.ZERO;
        BigDecimal ccUpPct = BigDecimal.ZERO;
        String name = "";

//        for (int i = 0; i < ks.size(); i++) {
        for (int i = 0; i < 1; i++) {

            int end = ks.size() - 1 - i;
            EmDailyK endK = ks.get(end);
            // 一直往前走, 取 最大值和最小值, 计算 pct, 然后除 length
            name = endK.getName();

            BigDecimal h = endK.getHigh();
            BigDecimal l = endK.getLow();
            for (int j = 1; j < Math.min(250, end); j++) {
                int start = end - j;
                EmDailyK startK = ks.get(start);
                if (startK.getHigh().compareTo(h) > 0) {
                    h = startK.getHigh();
                }
                if (startK.getLow().compareTo(l) < 0) {
                    l = startK.getLow();
                }

                if (l.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal osc = h.subtract(l).divide(l, 4, RoundingMode.HALF_UP);
                BigDecimal pct = osc.divide(new BigDecimal(j), 4, RoundingMode.HALF_UP);
                BigDecimal p = rangeMap.get(endK.getTradeDate());
                if (p == null || pct.compareTo(p) <= 0) {
                    rangeMap.put(endK.getTradeDate(), pct);
                    startTd = startK.getTradeDate();
                    range = j;
                    hh = h;
                    ll = l;
                    cc = endK.getClose();
                    ccUpPct = hh.subtract(cc).divide(cc, 4, RoundingMode.HALF_UP);
                    oscc = osc;
                }

            }

            if (range > 15
                    && oscc.compareTo(new BigDecimal("0.2")) < 0
                    && ccUpPct.compareTo(new BigDecimal("0.025")) <= 0) {
                Map<String, BigDecimal[]> ma = MaUtil.ma(ks.subList(0, end + 1));
                BigDecimal[] last = ma.get("last");

                boolean maUp = true;
                for (BigDecimal m : last) {
                    if (cc.compareTo(m) < 0) {
                        maUp = false;
                        break;
                    }
                }
                if (maUp) {
                    log.warn("code,{},end,{},start,{},h,{},l,{},区间振幅,{},周期,{},平均每日,{},距离高点,{}",
                            code + name, endK.getTradeDate(), startTd, hh, ll, BDUtil.p100(oscc, 4), range, BDUtil.p100(rangeMap.get(endK.getTradeDate()), 4), BDUtil.p100(ccUpPct));
                }
            }
        }
    }
}

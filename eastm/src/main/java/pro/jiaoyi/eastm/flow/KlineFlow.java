package pro.jiaoyi.eastm.flow;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.flow.common.FlowNo;
import pro.jiaoyi.eastm.flow.common.TradeTimeEnum;
import pro.jiaoyi.eastm.dao.entity.KLineEntity;
import pro.jiaoyi.eastm.dao.repo.KLineRepo;
import pro.jiaoyi.eastm.flow.common.CommonInfo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.service.KlineService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class KlineFlow implements BaseFlow {

    @Override
    public int getNo() {
        return FlowNo.KLINE_INFO;
    }

    @Resource
    private EmClient emClient;
    @Resource
    private KLineRepo kLineRepo;
    @Resource
    private KlineService klineService;

    @Override
    public void runByDay() {

        if (isTradeDay() && !isTradeTime().equals(TradeTimeEnum.POST)) {
            log.info("trade day , not 盘后 return");
            return;
        }

        run();
    }

    @Override
    public void run() {

        log.info("{} run {}", this.getClass().getSimpleName(), getNo());

        //准备基础数据
        List<EmCList> emList = CommonInfo.EM_LIST;

        List<EmCList> list = emList.stream().filter(
                em -> em.getF6Amt().compareTo(BDUtil.B1_5Y) > 0
                        && em.getF3Pct().compareTo(BDUtil.BN3) > 0
                        && em.getF3Pct().compareTo(BDUtil.B5) < 0
        ).toList();

        log.info("emList size={}, amt > 15000w size={}", emList.size(), list.size());

        for (EmCList em : list) {
            String code = em.getF12Code();
            String name = em.getF14Name();

            //checkDB 根据code 查询 tradeDate 最大值 的数据
            KLineEntity dbk = kLineRepo.findByCodeLast(code);
            if (dbk != null) {
                if (em.getF17Open().compareTo(dbk.getOpen()) == 0 &&
                        em.getF2Close().compareTo(dbk.getClose()) == 0 &&
                        em.getF15High().compareTo(dbk.getHigh()) == 0 &&
                        em.getF16Low().compareTo(dbk.getLow()) == 0 &&
                        em.getF6Amt().compareTo(dbk.getAmt()) == 0 &&
                        em.getF5Vol().compareTo(dbk.getVol()) == 0) {
                    log.info("pass, code={} name={} dbk={} em={}", code, name, dbk, em);

                    CommonInfo.CODE_K_MAP.put(code, dbk);
                    continue;
                }
            }

            //如何比较两个enum 是否相等
            if (isTradeTime().equals(TradeTimeEnum.TRADE) && isTradeDay()) {
                log.info("只有盘中不保存");
                continue;
            }

            List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now(), 500, true);
            if (ks == null || ks.size() < 260) {
                log.warn("pass, code={} name={} ks size={}", code, name, ks == null ? 0 : ks.size());
                continue;
            }

            List<KLineEntity> kLineEntities = dkToKLineEntity(ks);
            log.debug("kLineEntities size={}", kLineEntities.size());

            //更新common info
            KLineEntity k = kLineEntities.get(kLineEntities.size() - 1);
            CommonInfo.CODE_K_MAP.put(k.getCode(), k);

//            kListToSave.add(k);
            klineService.updateDB(code, name, k);
        }

//        klineService.updateDB(kListToSave);
    }


    public List<KLineEntity> dkToKLineEntity(List<EmDailyK> ks) {

        BigDecimal[] amtArr = ks.stream().map(EmDailyK::getAmt).toList().toArray(new BigDecimal[0]);
        BigDecimal[] amtArr_ma5 = MaUtil.ma(5, amtArr, 3);
        BigDecimal[] amtArr_ma10 = MaUtil.ma(10, amtArr, 3);
        BigDecimal[] amtArr_ma20 = MaUtil.ma(20, amtArr, 3);
        BigDecimal[] amtArr_ma30 = MaUtil.ma(30, amtArr, 3);
        BigDecimal[] amtArr_ma60 = MaUtil.ma(60, amtArr, 3);
        BigDecimal[] amtArr_ma120 = MaUtil.ma(120, amtArr, 3);
        BigDecimal[] amtArr_ma250 = MaUtil.ma(250, amtArr, 3);

        BigDecimal[] closeArr = ks.stream().map(EmDailyK::getClose).toList().toArray(new BigDecimal[0]);
        BigDecimal[] ma5 = MaUtil.ma(5, closeArr, 3);
        BigDecimal[] ma10 = MaUtil.ma(10, closeArr, 3);
        BigDecimal[] ma20 = MaUtil.ma(20, closeArr, 3);
        BigDecimal[] ma30 = MaUtil.ma(30, closeArr, 3);
        BigDecimal[] ma60 = MaUtil.ma(60, closeArr, 3);
        BigDecimal[] ma120 = MaUtil.ma(120, closeArr, 3);
        BigDecimal[] ma250 = MaUtil.ma(250, closeArr, 3);


        List<KLineEntity> kLineEntities = new ArrayList<>(ks.size());
        for (int i = 0; i < ks.size(); i++) {
            EmDailyK dk = ks.get(i);
            String s = JSON.toJSONString(dk);
//                KLineEntity entity = JSON.toJavaObject(JSON.parseObject(s), KLineEntity.class);
            KLineEntity entity = JSON.parseObject(s, KLineEntity.class);
            kLineEntities.add(entity);

            if (dk.getHsl().compareTo(BigDecimal.ZERO) == 0) {
                log.warn("{} hsl is zero ==> set 100", dk.getCode() + dk.getName());
                dk.setHsl(new BigDecimal(100));
            }

            BigDecimal mv = dk.getAmt().multiply(BDUtil.B100).divide(dk.getHsl(), 0, RoundingMode.HALF_UP);
            entity.setMv(mv);
            entity.setTradeDate(DateUtil.strToLocalDate(dk.getTradeDate(), DateUtil.PATTERN_yyyyMMdd));
            entity.setTradeDateStr(dk.getTradeDate());

            entity.setMa5(ma5[i]);
            entity.setMa10(ma10[i]);
            entity.setMa20(ma20[i]);
            entity.setMa30(ma30[i]);
            entity.setMa60(ma60[i]);
            entity.setMa120(ma120[i]);
            entity.setMa250(ma250[i]);

            //成交额 均线值
            entity.setVma5(amtArr_ma5[i]);
            entity.setVma10(amtArr_ma10[i]);
            entity.setVma20(amtArr_ma20[i]);
            entity.setVma30(amtArr_ma30[i]);
            entity.setVma60(amtArr_ma60[i]);
            entity.setVma120(amtArr_ma120[i]);
            entity.setVma250(amtArr_ma250[i]);

            if (i > 250) {
                //包含今天
                entity.setPct5(dk.getClose().subtract(ks.get(i + 1 - 5).getClose()).divide(ks.get(i + 1 - 5).getClose(), 4, RoundingMode.HALF_UP));
                entity.setPct10(dk.getClose().subtract(ks.get(i + 1 - 10).getClose()).divide(ks.get(i + 1 - 10).getClose(), 4, RoundingMode.HALF_UP));
                entity.setPct20(dk.getClose().subtract(ks.get(i + 1 - 20).getClose()).divide(ks.get(i + 1 - 20).getClose(), 4, RoundingMode.HALF_UP));
                entity.setPct30(dk.getClose().subtract(ks.get(i + 1 - 30).getClose()).divide(ks.get(i + 1 - 30).getClose(), 4, RoundingMode.HALF_UP));
                entity.setPct60(dk.getClose().subtract(ks.get(i + 1 - 60).getClose()).divide(ks.get(i + 1 - 60).getClose(), 4, RoundingMode.HALF_UP));
                entity.setPct120(dk.getClose().subtract(ks.get(i + 1 - 120).getClose()).divide(ks.get(i + 1 - 120).getClose(), 4, RoundingMode.HALF_UP));
                entity.setPct250(dk.getClose().subtract(ks.get(i + 1 - 250).getClose()).divide(ks.get(i + 1 - 250).getClose(), 4, RoundingMode.HALF_UP));
            }

            BigDecimal hsl = entity.getHsl();
            //sublist [) 左闭右开
            // i = 499 ,  i +1 -5 = 495, i + 1 = 499,  [495, 496, 497, 498, 499], 也就是包含了当日(i)数据
            // vl=> vol Low position , 0 - 1 , 越小代表成交量越小, 越大代表成交量越大
            //vl5 , 0 代表最近5天最小成交量, 1 代表最近5天最大成交量

            if (i < 5 - 1) {
                entity.setVl5(BDUtil.BN1);
            } else {
                List<BigDecimal> l5 = ks.subList(i + 1 - 5, i + 1).stream().map(EmDailyK::getHsl).sorted().toList();
                entity.setVl5(new BigDecimal(l5.indexOf(hsl)).divide(new BigDecimal(l5.size() - 1), 4, RoundingMode.HALF_UP));
            }

            if (i < 10 - 1) {
                entity.setVl10(BDUtil.BN1);
            } else {
                List<BigDecimal> l10 = ks.subList(i + 1 - 10, i + 1).stream().map(EmDailyK::getHsl).sorted().toList();
                entity.setVl10(new BigDecimal(l10.indexOf(hsl)).divide(new BigDecimal(l10.size() - 1), 4, RoundingMode.HALF_UP));
            }

            if (i < 20 - 1) {
                entity.setVl20(BDUtil.BN1);
            } else {
                List<BigDecimal> l20 = ks.subList(i + 1 - 20, i + 1).stream().map(EmDailyK::getHsl).sorted().toList();
                entity.setVl20(new BigDecimal(l20.indexOf(hsl)).divide(new BigDecimal(l20.size() - 1), 4, RoundingMode.HALF_UP));
            }

            if (i < 30 - 1) {
                entity.setVl30(BDUtil.BN1);
            } else {
                List<BigDecimal> l30 = ks.subList(i + 1 - 30, i + 1).stream().map(EmDailyK::getHsl).sorted().toList();
                entity.setVl30(new BigDecimal(l30.indexOf(hsl)).divide(new BigDecimal(l30.size() - 1), 4, RoundingMode.HALF_UP));
            }

            if (i < 60 - 1) {
                entity.setVl60(BDUtil.BN1);
            } else {
                List<BigDecimal> l60 = ks.subList(i + 1 - 60, i + 1).stream().map(EmDailyK::getHsl).sorted().toList();
                entity.setVl60(new BigDecimal(l60.indexOf(hsl)).divide(new BigDecimal(l60.size() - 1), 4, RoundingMode.HALF_UP));
            }

            if (i < 120 - 1) {
                entity.setVl120(BDUtil.BN1);
            } else {
                List<BigDecimal> l120 = ks.subList(i + 1 - 120, i + 1).stream().map(EmDailyK::getHsl).sorted().toList();
                entity.setVl120(new BigDecimal(l120.indexOf(hsl)).divide(new BigDecimal(l120.size() - 1), 4, RoundingMode.HALF_UP));
            }

            if (i < 250 - 1) {
                entity.setVl250(BDUtil.BN1);
            } else {
                List<BigDecimal> l250 = ks.subList(i + 1 - 250, i + 1).stream().map(EmDailyK::getHsl).sorted().toList();
                entity.setVl250(new BigDecimal(l250.indexOf(hsl)).divide(new BigDecimal(l250.size() - 1), 4, RoundingMode.HALF_UP));
            }
        }

        return kLineEntities;
    }
}

package pro.jiaoyi.eastm;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.config.IndexEnum;
import pro.jiaoyi.eastm.config.VipIndexEnum;
import pro.jiaoyi.eastm.dao.entity.KLineEntity;
import pro.jiaoyi.eastm.dao.repo.KLineRepo;
import pro.jiaoyi.eastm.model.EastSpeedInfo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.model.excel.Index1000XlsData;
import pro.jiaoyi.eastm.model.fenshi.DetailTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiVo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pro.jiaoyi.common.strategy.BreakOutStrategy.SIDE_MAP;

@SpringBootTest
@Slf4j
class EastmKlineTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private EmClient emClient;

    @Autowired
    private KLineRepo kLineRepo;


    @Test
    public void getAll() {
        //获取 成份股
        HashSet<String> indexSet = new HashSet<>();
        indexSet.addAll(emClient.getIndex(IndexEnum.HS300.getUrl()).stream().map(EmCList::getF12Code).toList());
        indexSet.addAll(emClient.getIndex(IndexEnum.CYCF.getUrl()).stream().map(EmCList::getF12Code).toList());
        indexSet.addAll(emClient.getIndex(IndexEnum.ZZ500.getUrl()).stream().map(EmCList::getF12Code).toList());
        indexSet.addAll(emClient.getIndex1000().stream().map(EmCList::getF12Code).toList());

        int daysBefore = 0;//0 当天 1 昨天 2 前天
        getAllIndex(indexSet, daysBefore);

    }


    public void getAllIndex(HashSet<String> containSet, int daysBefore) {

        List<EmCList> srclist = emClient.getClistDefaultSize(false);
        List<EmCList> list = srclist.stream().filter(emCList -> containSet.contains(emCList.getF12Code())).toList();

        LocalDate end = LocalDate.now().minusDays(daysBefore);
        long endTs = DateUtil.toTimestamp(end);

        for (EmCList emCList : list) {
            List<EmDailyK> dailyKs = emClient.getDailyKs(emCList.getF12Code(), end, 500, false);
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
                List<BigDecimal> l5 = dailyKs.subList(i + 1 - 5, i + 1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l10 = dailyKs.subList(i + 1 - 10, i + 1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l20 = dailyKs.subList(i + 1 - 20, i + 1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l30 = dailyKs.subList(i + 1 - 30, i + 1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l60 = dailyKs.subList(i + 1 - 60, i + 1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l120 = dailyKs.subList(i + 1 - 120, i + 1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);
                List<BigDecimal> l250 = dailyKs.subList(i + 1 - 250, i + 1).stream().map(EmDailyK::getAmt).sorted().toList();//.toArray(new BigDecimal[0]);

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


    @Test
    public void xuangu(){
        List<String> codes = emClient.xuangu();
        Map<String, String> codeNameMap = emClient.getCodeNameMap(false);
        for (String code : codes) {
            log.info("{} {}",code,codeNameMap.get(code));
        }
    }


    @Test
    public void guba(){
        List<String> guba = emClient.guba("601088");
        System.out.println(guba);

    }





}

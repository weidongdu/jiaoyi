package pro.jiaoyi.eastm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.config.IndexEnum;
import pro.jiaoyi.eastm.config.VipIndexEnum;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.dao.entity.FenshiSimpleEntity;
import pro.jiaoyi.eastm.dao.entity.KLineEntity;
import pro.jiaoyi.eastm.dao.repo.FenshiSimpleRepo;
import pro.jiaoyi.eastm.dao.repo.KLineRepo;
import pro.jiaoyi.eastm.job.MarketJob;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiVo;
import pro.jiaoyi.eastm.service.FenshiService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void xuangu() {
        List<String> codes = emClient.xuangu();
        Map<String, String> codeNameMap = emClient.getCodeNameMap(false);
        for (String code : codes) {
            log.info("{} {}", code, codeNameMap.get(code));
        }
    }


    @Test
    public void guba() {
        List<String> guba = emClient.guba("601088");
        System.out.println(guba);

    }


    @Resource
    private EmRealTimeClient emRealTimeClient;

    @Test
    public void fenShi() {
//        String code = "688016";

        //这里做一个测试
        //1, 开盘放量
//        List<EmCList> index = emClient.getIndex(IndexEnum.EM_MA_UP, true);
        List<EmCList> index = emClient.getClistDefaultSize(true);
        for (EmCList emCList : index) {
            if (emCList.getF3Pct().compareTo(BDUtil.B7) < 0) continue;

            String code = emCList.getF12Code();
            EastGetStockFenShiVo fEastGetStockFenShiVo = emRealTimeClient.getFenshiByCode(code);
            if (fEastGetStockFenShiVo == null) continue;
            EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(fEastGetStockFenShiVo);
            if (trans == null) continue;
            log.info("开盘 open {} vol={} amt={}", trans.getOpenPrice(), trans.getOpenVol(), trans.getOpenAmt());
            if (trans.getOpenAmt() == null) continue;

            List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now(), 200, true);
            if (ks.size() < 100) continue;
            ks.remove(ks.size() - 1);
            //计算 amt
            BigDecimal dayAmtTop10 = emClient.amtTop10p(ks);
            BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
            BigDecimal fAmt = BDUtil.b0_1.multiply(hourAmt);
            //成交量放大倍数
            BigDecimal fx = trans.getOpenAmt().divide(hourAmt, 4, RoundingMode.HALF_UP);


            //code=301085 name=亚康股份 openPct=1.0404 amt top10p=709323427 hourAmt=177330857 fAmt=17733085.7 openAmt=14602884.000 fx=0.0823 pct=7.54
            //code=301095 name=广立微 openPct=1.0187 amt top10p=667663826 hourAmt=166915957 fAmt=16691595.7 openAmt=3205190.000 fx=0.0192 pct=10.51
            //条件 fx > 0.01
//            if (fx.compareTo(BDUtil.b0_01) < 0) continue;
            BigDecimal diff = emCList.getF2Close().subtract(emCList.getF17Open()).divide(emCList.getF17Open(), 4, RoundingMode.HALF_UP);
            log.info("code={} name={} openPct={} amt top10p={} hourAmt={} fAmt={} openAmt={} fx={} pct={}", code, emCList.getF14Name()
                    , trans.getOpenPrice().divide(trans.getClosePre(), 4, RoundingMode.HALF_UP), dayAmtTop10, hourAmt, fAmt, trans.getOpenAmt(), fx, emCList.getF3Pct());
            log.error("match fx={} close-open={} {}", fx, diff.multiply(BDUtil.B100), emCList);

            fenshiService.saveOrUpdate(trans, null);
        }

    }

    @Resource
    private FenshiService fenshiService;

    @Test
    public void save() {

        String dir = "/Users/dwd/Downloads/search/mj/20220610";
        FileUtil.readDirectoryFilesAbsPath(dir);
        List<EmCList> list = emClient.getClistDefaultSize(true);
        for (EmCList emCList : list) {
            if (emCList.getF3Pct().compareTo(BDUtil.B7) < 0) {

                if (emCList.getF17Open().compareTo(BigDecimal.ZERO) > 0
                        && emCList.getF18Close().subtract(emCList.getF17Open())
                        .divide(emCList.getF17Open(), 4, RoundingMode.HALF_UP).compareTo(BDUtil.b0_05) > 0) {
                    //例外情况 低开高走 5% 以上
                } else {
                    continue;
                }
            }

            String code = emCList.getF12Code();


            try {
                EastGetStockFenShiVo fEastGetStockFenShiVo = emRealTimeClient.getFenshiByCode(code);
                if (fEastGetStockFenShiVo == null) continue;
                EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(fEastGetStockFenShiVo);
                if (trans == null) continue;
                if (trans.getOpenAmt() == null
                        || trans.getOpenAmt().compareTo(BDUtil.B1Y) < 0) continue;


                fenshiService.saveOrUpdate(trans, null);
            } catch (Exception e) {
                log.error("save error {}", e.getMessage(), e);
            }

        }


    }



    @Resource
    private FenshiSimpleRepo fenshiSimpleRepo;

    @Test
    public void fenshiSimple() {
        String td = LocalDate.now().toString().replaceAll("-", "");
        HashSet<EmCList> set = new HashSet<>();
        List<EmCList> emList = emClient.getClistDefaultSize(true);

        AtomicInteger counter = new AtomicInteger(0);

        for (EmCList e : emList) {
            try {
                System.out.println(counter.get());
                if (counter.getAndIncrement() % 10 == 0) Thread.sleep(1000);
            } catch (InterruptedException ex) {
                log.error("sleep 1s");
            }
            String code = e.getF12Code();
            int i = fenshiSimpleRepo.countByCodeAndTradeDate(code, td);
            if (i > 0) continue;

            EastGetStockFenShiVo fEastGetStockFenShiVo = emRealTimeClient.getFenshiByCode(code);
            if (fEastGetStockFenShiVo == null) {
                set.add(e);
                continue;
            }

            EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(fEastGetStockFenShiVo);
            if (trans == null) {
                set.add(e);
                continue;
            }

            saveToFenshiSimple(td, e, trans);
        }


        for (EmCList e : set) {
            try {
                if (counter.getAndIncrement() % 10 == 0) Thread.sleep(1000);
            } catch (InterruptedException ex) {
                log.error("sleep 1s");
            }
            String code = e.getF12Code();
            int i = fenshiSimpleRepo.countByCodeAndTradeDate(code, td);
            if (i > 0) continue;

            EastGetStockFenShiVo fEastGetStockFenShiVo = emRealTimeClient.getFenshiByCode(code);
            if (fEastGetStockFenShiVo == null) {
                continue;
            }

            EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(fEastGetStockFenShiVo);
            if (trans == null) {
                continue;
            }

            saveToFenshiSimple(td, e, trans);
        }


    }

    private void saveToFenshiSimple(String td, EmCList e, EastGetStockFenShiTrans trans) {

        try {
            FenshiSimpleEntity fenshiSimpleEntity = new FenshiSimpleEntity();
            BeanUtils.copyProperties(e, fenshiSimpleEntity);
            fenshiSimpleEntity.setData(JSON.toJSONString(trans));

            fenshiSimpleEntity.setId(null);//Long id;
            fenshiSimpleEntity.setCode(trans.getCode());//String code;
            fenshiSimpleEntity.setName(trans.getName());//String name;
            fenshiSimpleEntity.setClosePre(trans.getClosePre());//BigDecimal open;//当前价
            fenshiSimpleEntity.setOpen(trans.getOpenPrice());//BigDecimal open;//当前价
            fenshiSimpleEntity.setOpenVol(trans.getOpenVol());//BigDecimal openVol;//开盘量
            fenshiSimpleEntity.setOpenAmt(trans.getOpenAmt());//BigDecimal openAmt;//开盘额
            fenshiSimpleEntity.setCreateTime(LocalDateTime.now());//LocalDateTime createTime;//创建时间
            fenshiSimpleEntity.setTradeDate(td);
            fenshiSimpleRepo.saveAndFlush(fenshiSimpleEntity);
        } catch (Exception ex) {
            log.error("{}", ex.getMessage());
        }
    }


    @Test
    public void update1() {
//        String td = "20231031";
//        String tdPre = "20231030";
        String td = DateUtil.tdPre(0);
        String tdPre = DateUtil.tdPre(3);

        List<EmCList> list = emClient.getClistDefaultSize(true);
        boolean flag = false;
        for (EmCList emCList : list) {
            log.info(emCList.getF12Code());
//            if ("301023".equals(emCList.getF12Code())){
//                flag = true;
//            }
//            if (!flag){
//                continue;
//            }
            FenshiSimpleEntity dbEntity = fenshiSimpleRepo.findByCodeAndTradeDate(emCList.getF12Code(), td);
            if (dbEntity == null) continue;

            FenshiSimpleEntity dbEntityPre = fenshiSimpleRepo.findByCodeAndTradeDate(emCList.getF12Code(), tdPre);
            if (dbEntityPre == null) continue;

            if (dbEntity.getOpenAmt() == null
                    || dbEntityPre.getOpenAmt() == null
                    || dbEntityPre.getOpenAmt().compareTo(BigDecimal.ZERO) == 0) {
                dbEntity.setOpenAmtPre(BigDecimal.ZERO);
                dbEntity.setOpenAmtPreFx(BigDecimal.ZERO);
            }else {

                dbEntity.setOpenAmtPre(dbEntityPre.getOpenAmt());
                dbEntity.setOpenAmtPreFx(dbEntity.getOpenAmt()
                        .divide(dbEntityPre.getOpenAmt(), 4, RoundingMode.HALF_UP));
            }


            try {
                BigDecimal openPct = dbEntity.getF17Open().subtract(dbEntity.getClosePre()).divide(dbEntity.getClosePre(), 4, RoundingMode.HALF_UP);
                BigDecimal holdPct = dbEntity.getF2Close().subtract(dbEntity.getF17Open()).divide(dbEntity.getF17Open(), 4, RoundingMode.HALF_UP);
                dbEntity.setOpenPct(openPct);//openPct;//开盘幅度 (open - closePre )/ closePre
                dbEntity.setHoldPct(holdPct);//holdPct;//开盘幅度 (close - open )/ open
            } catch (Exception e) {

                dbEntity.setOpenPct(BigDecimal.valueOf(-100));//openPct;//开盘幅度 (open - closePre )/ closePre
                dbEntity.setHoldPct(BigDecimal.valueOf(-100));//holdPct;//开盘幅度 (close - open )/ open
            }

            //计算 amt
            try {
                List<EmDailyK> ks = emClient.getDailyKs(dbEntity.getCode(), LocalDate.now(), 100, false);
                ks.remove(ks.size()-1);
                ks.remove(ks.size()-1);

                BigDecimal dayAmtTop10 = emClient.amtTop10p(ks);
                BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
                BigDecimal fAmt = BDUtil.b0_1.multiply(hourAmt);
                //成交量放大倍数
                dbEntity.setFAmt(fAmt);
                dbEntity.setFAmtFx(dbEntity.getOpenAmt().divide(fAmt, 4, RoundingMode.HALF_UP));
            } catch (Exception e) {
                dbEntity.setFAmt(BigDecimal.ZERO);
                dbEntity.setFAmtFx(BigDecimal.ZERO);
            }

            fenshiSimpleRepo.saveAndFlush(dbEntity);
        }
    }


    @Test
    public void filter() {
        List<EmCList> list = emClient.getClistDefaultSize(true);
        List<String> dbLists = fenshiSimpleRepo.findCodesByOpenAmt(BDUtil.B5000W);

        HashSet<String> set = new HashSet<>(dbLists);

        ArrayList<EmCList> filterList = new ArrayList<>();
        for (EmCList emCList : list) {
            if (!set.contains(emCList.getF12Code())){
                continue;
            }

            List<FenshiSimpleEntity> dbList = fenshiSimpleRepo.findByCode(emCList.getF12Code());
            if (dbList == null || dbList.size() < 2) continue;

            List<FenshiSimpleEntity> sortList = dbList.stream().sorted(Comparator.comparingLong(FenshiSimpleEntity::getId)).toList();
            int size = sortList.size();

            for (int i = size - 1; i > 0; i--) {
                FenshiSimpleEntity item = sortList.get(i);
                FenshiSimpleEntity item1 = sortList.get(i - 1);
                if (item.getOpenAmt() != null && item1.getOpenAmt() != null) {
                    if (item.getOpenAmt().compareTo(BDUtil.B5000W) > 0
                            && item.getOpenAmt().compareTo(item1.getOpenAmt()) > 0) {
                        filterList.add(emCList);
                    }
                }
            }
        }

        System.out.println("filter list");
        for (EmCList emCList : filterList) {
            System.out.println(JSON.toJSONString(emCList));
        }

    }


    @Test
    public void famt(){
        String code = "600520";
        List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now(), 500, true);
        ks.remove(ks.size()-1);

        //计算 amt
        BigDecimal dayAmtTop10 = emClient.amtTop10p(ks);
        BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
        BigDecimal fAmt = BDUtil.b0_1.multiply(hourAmt);

        System.out.println(fAmt);
//        if (fAmt.compareTo(BDUtil.B5000W) > 0 || fAmt.compareTo(BDUtil.B1000W) < 0) {
//            continue;
//        }
    }


    @Resource
    private WxUtil wxUtil;

    @Test
    public void sCodeRunOpen(){
        {
            //判断昨天集合竞价

            String code = "300291";
            //判断fx
            List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now(), 300, true);
            if (ks.size() < 300) {
                log.info("runOpen code {} size < 300", code);
                return;
            }


            //不清楚这个时候 是否出了最新的k
            EmDailyK k = ks.get(ks.size() - 1);

            String td = LocalDate.now().toString().replaceAll("-", "");

            ks.remove(ks.size() - 1);

            Map<String, BigDecimal[]> ma = MaUtil.ma(ks);
            BigDecimal[] ma5 = ma.get("ma5");
            BigDecimal[] ma10 = ma.get("ma10");
            BigDecimal[] ma20 = ma.get("ma20");
            BigDecimal[] ma30 = ma.get("ma30");
            BigDecimal[] ma60 = ma.get("ma60");
            BigDecimal[] ma120 = ma.get("ma120");
            BigDecimal[] ma250 = ma.get("ma250");

            int last = ks.size() - 1;
            if (k.getClose().compareTo(ma5[last]) < 0
                    || k.getClose().compareTo(ma10[last]) < 0
                    || k.getClose().compareTo(ma20[last]) < 0
                    || k.getClose().compareTo(ma30[last]) < 0
                    || k.getClose().compareTo(ma60[last]) < 0
                    || k.getClose().compareTo(ma120[last]) < 0
                    || k.getClose().compareTo(ma250[last]) < 0) {
                log.info("runOpen code {} 不满足均线之上", code);
            }


            //计算 amt
            BigDecimal dayAmtTop10 = emClient.amtTop10p(ks);
            BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
            BigDecimal fAmt = BDUtil.b0_1.multiply(hourAmt);
            if (fAmt.compareTo(BDUtil.B1000W) < 0) {
                log.info("runOpen code {} 不满足条件(约成交额 fAmt<500w)", code);
                return;
            }


            //成交量放大倍数

//            BigDecimal fx = k.getAmt().divide(fAmt, 4, RoundingMode.HALF_UP);
            BigDecimal fx = BigDecimal.valueOf(65000000).divide(fAmt, 4, RoundingMode.HALF_UP);
            if (fx.compareTo(BDUtil.B5) > 0) {
                String content = k.getCode() + k.getName()
                        + "<br>bk=" + k.getBk()
                        + "<br>p=" + k.getClose()
                        + "<br>pct=" + k.getPct() + "%"
                        + "<br>fx=" + fx
                        + "<br>amt=" + BDUtil.amtHuman(k.getAmt())
                        + "<br>td=" + LocalDateTime.now().toString().substring(0, 16);
                try {
                    String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);
                    wxUtil.send(encodedContent);

                } catch (Exception ex) {
                    log.error("{}", ex.getMessage(), ex);
                }

            }
            log.info("runOpen code {} 不满足条件 fx > 5 fx={}", code,fx);
        }
    }


@Resource
private MarketJob marketJob;
    @Test
    public void tmp1(){

//        marketJob.con();

//        List<String> list = emClient.coreTheme("002584");
//        System.out.println(list);
//        List<String> listD = emClient.coreThemeDetail("002584");
//        System.out.println(listD);


        List<String> list = emClient.crossMa();

        System.out.println(list);


    }



}

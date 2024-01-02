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
import org.springframework.web.bind.annotation.GetMapping;
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
import pro.jiaoyi.eastm.dao.entity.*;
import pro.jiaoyi.eastm.dao.repo.*;
import pro.jiaoyi.eastm.job.MarketJob;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiVo;
import pro.jiaoyi.eastm.service.FenshiService;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketOption;
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


    @Autowired
    private EmDailyKRepo emDailyKRepo;

    @Autowired
    private OpenEmCListRepo openEmCListRepo;
    @Autowired
    private StopRepo stopRepo;

    @Test
    public void t() {
        List<EmCList> list = emClient.getClistDefaultSize(false);
        boolean flag = false;
        for (EmCList emCList : list) {
            String code = emCList.getF12Code();
            String name = emCList.getF14Name();

            //先查db
            List<EmDailyKEntity> dbs = emDailyKRepo.findByCode(code);

            List<EmDailyK> ks = new ArrayList<>();

            if (dbs == null || dbs.size() == 0) {
//                ks = emClient.getDailyKs(code, LocalDate.now(), 2000, false);
//                if (ks == null ) {
//                    continue;
//                }
//
//                for (EmDailyK k : ks) {
//                    EmDailyKEntity entity = new EmDailyKEntity();
//                    BeanUtils.copyProperties(k, entity);
//                    entity.setId(null);
//                    emDailyKRepo.save(entity);
//                }


            } else {

                for (EmDailyKEntity db : dbs) {
                    EmDailyK k = new EmDailyK();
                    BeanUtils.copyProperties(db, k);
                    ks.add(k);
                }
            }


            for (int i = 2; i < ks.size() - 5; i++) {
                EmDailyK preK = ks.get(i - 1);
                EmDailyK k = ks.get(i);

                //preK 涨停

                //k T 字或 一 字

                if (k.getPct().compareTo(BigDecimal.ONE) > 0
                        && k.getClose().compareTo(k.getOpen()) == 0
                        && k.getClose().compareTo(k.getHigh()) == 0
                        && k.getLow().compareTo(new BigDecimal("0.95").multiply(k.getClose())) > 0) {
                    //判断pre 是否为涨停
//                    EmDailyK k1 = ks.get(i + 1);
//                    BigDecimal openDiff = k1.getOpen().subtract(k1.getPreClose());
//                    BigDecimal k1openPct = openDiff.divide(k1.getPreClose(), 4, RoundingMode.HALF_UP);
//                    if (k1openPct.compareTo(new BigDecimal("0.015")) <= 0
//                            && k1openPct.compareTo(new BigDecimal("-0.01")) > 0) {
//                    } else {
//                        continue;
//                    }

//                    if (highBreak(preK) && k.getAmt().compareTo(new BigDecimal("0.5").multiply(preK.getAmt())) < 0) {
                    if (highBreak(preK)) {

                        //判断当前是第几个板
                        int count = 1;

                        for (int j = 1; j < 20; j++) {
                            int index = i - j;
                            if (index < 0) {
                                break;
                            }
                            if (highBreak(ks.get(index))) {
                                count++;
                            } else {
                                break;
                            }
                        }

                        if (k.getClose().compareTo(k.getLow()) == 0) {
                            log.info("满足条件 一字板 {}板 且缩量1/2 name={} td={} amt={} close={} {}", count, name + code, k.getTradeDate(), BDUtil.amtHuman(k.getAmt()), k.getClose(), JSON.toJSONString(k));
                        } else {
                            log.info("满足条件 T字板 {}板 且缩量1/2 name={} td={} amt={} close={} {}", count, name + code, k.getTradeDate(), BDUtil.amtHuman(k.getAmt()), k.getClose(), JSON.toJSONString(k));
                        }

                        //判断 post 5 天open close 幅度
                        StopEntity stopEntity = new StopEntity();
                        stopEntity.setCode(code);
                        stopEntity.setName(name);
                        stopEntity.setTradeDate(k.getTradeDate());
                        stopEntity.setOpen(k.getOpen());
                        stopEntity.setClose(k.getClose());
                        stopEntity.setHigh(k.getHigh());
                        stopEntity.setLow(k.getLow());
                        stopEntity.setPct(k.getPct());
                        stopEntity.setAmt(k.getAmt());
                        stopEntity.setStopCount(new BigDecimal(count));

                        stopEntity.setPreAmt(preK.getAmt());
                        stopEntity.setPreAmtRate(k.getAmt().divide(preK.getAmt(), 4, RoundingMode.HALF_UP));


                        for (int j = 1; j <= 5; j++) {
                            EmDailyK postK = ks.get(i + j);
                            BigDecimal openPct = (postK.getOpen().subtract(k.getClose())).divide(k.getClose(), 4, RoundingMode.HALF_UP);
                            BigDecimal closePct = (postK.getClose().subtract(k.getClose())).divide(k.getClose(), 4, RoundingMode.HALF_UP);
                            BigDecimal entityPct = postK.getClose().subtract(postK.getOpen()).divide(postK.getPreClose(), 4, RoundingMode.HALF_UP);

                            if (j == 2) {
                                log.error("post {}K td={} amt={} open={} openPct={} close={} closePct={} 实体={} {}", j, postK.getTradeDate(), BDUtil.amtHuman(postK.getAmt()), postK.getOpen(), BDUtil.p100(openPct), postK.getClose(), BDUtil.p100(closePct), BDUtil.p100(entityPct), JSON.toJSONString(postK));
                            } else {
                                log.info("post {}K td={} amt={} open={} openPct={} close={} closePct={} 实体={} {}", j, postK.getTradeDate(), BDUtil.amtHuman(postK.getAmt()), postK.getOpen(), BDUtil.p100(openPct), postK.getClose(), BDUtil.p100(closePct), BDUtil.p100(entityPct), JSON.toJSONString(postK));
                            }

                            if (j == 1) {
                                stopEntity.setPostK1Open(postK.getOpen());
                                stopEntity.setPostK1Close(postK.getClose());
                                stopEntity.setPostK1Open(postK.getOpen());
                                stopEntity.setPostK1OpenPct(openPct);
                                stopEntity.setPostK1ClosePct(closePct);
                                stopEntity.setPostK1Amt(postK.getAmt());
                                stopEntity.setPostK1TradeDate(postK.getTradeDate());
                            }


                            if (j == 2) {
                                stopEntity.setPostK2Open(postK.getOpen());
                                stopEntity.setPostK2Close(postK.getClose());
                                stopEntity.setPostK2OpenPct(openPct);
                                stopEntity.setPostK2ClosePct(closePct);
                                stopEntity.setPostK2Amt(postK.getAmt());
                                stopEntity.setPostK2TradeDate(postK.getTradeDate());
                            }


                            if (j == 3) {
                                stopEntity.setPostK3Open(postK.getOpen());
                                stopEntity.setPostK3Close(postK.getClose());
                                stopEntity.setPostK3OpenPct(openPct);
                                stopEntity.setPostK3ClosePct(closePct);
                                stopEntity.setPostK3Amt(postK.getAmt());
                                stopEntity.setPostK3TradeDate(postK.getTradeDate());
                            }


                            if (j == 4) {
                                stopEntity.setPostK4Open(postK.getOpen());
                                stopEntity.setPostK4Close(postK.getClose());
                                stopEntity.setPostK4OpenPct(openPct);
                                stopEntity.setPostK4ClosePct(closePct);
                                stopEntity.setPostK4Amt(postK.getAmt());
                                stopEntity.setPostK4TradeDate(postK.getTradeDate());
                            }


                            if (j == 5) {
                                stopEntity.setPostK5Open(postK.getOpen());
                                stopEntity.setPostK5Close(postK.getClose());
                                stopEntity.setPostK5OpenPct(openPct);
                                stopEntity.setPostK5ClosePct(closePct);
                                stopEntity.setPostK5Amt(postK.getAmt());
                                stopEntity.setPostK5TradeDate(postK.getTradeDate());
                            }

                        }


                        stopRepo.save(stopEntity);
                    }

                }


            }

        }
    }


    public boolean highBreak(EmDailyK k) {
        BigDecimal f = new BigDecimal("1.095");

        if (k.getPct().compareTo(BigDecimal.ONE) > 0
                && k.getHigh().compareTo(k.getClose()) == 0
                && k.getOpen().compareTo(k.getClose()) == 0
                && k.getHigh().compareTo(f.multiply(k.getPreClose())) >= 0) {
            return true;
        } else {
            return false;
        }
    }


    @Test
    public void tOpen() {
        List<EmCList> list = emClient.getClistDefaultSize(false);
        StringBuffer b = new StringBuffer();
        b.append("name,code,stop,td,开盘额,hsl,开盘amt/ma60,开盘amt/昨日,昨日amt/ma60,openPct,closePct,open2Pct\n");
        for (EmCList emCList : list) {
            String code = emCList.getF12Code();
            String name = emCList.getF14Name();

//            //先查db
            List<EmDailyKEntity> dbs = emDailyKRepo.findByCode(code);

            List<EmDailyK> ks = new ArrayList<>();

            for (EmDailyKEntity db : dbs) {
                EmDailyK k = new EmDailyK();
                BeanUtils.copyProperties(db, k);
                ks.add(k);
            }

            BigDecimal[] amtArr = ks.stream().map(EmDailyK::getAmt).toList().toArray(new BigDecimal[0]);
            List<String> tds = ks.stream().map(EmDailyK::getTradeDate).toList();

            BigDecimal[] ma60s = MaUtil.ma(60, amtArr, 2);

            HashMap<String, BigDecimal> map = new HashMap<>();
            HashMap<String, EmDailyK> mapK = new HashMap<>();
            for (int i = 1; i < ma60s.length; i++) {
                String tradeDate = ks.get(i).getTradeDate();
                map.put(tradeDate, ma60s[i - 1]);
                mapK.put(tradeDate, ks.get(i));
            }

            List<OpenEmCListEntity> openList = openEmCListRepo.findByF12Code(code);
            for (int i = 1; i < openList.size() - 1; i++) {
                OpenEmCListEntity o = openList.get(i);
                String tradeDate = o.getTradeDate();

                int tdi = tds.indexOf(tradeDate);
                if (tdi < 1 || tdi + 1 > tds.size() - 1) {
                    continue;
                }


                String tradeDate_1 = tds.get(tdi - 1);
                EmDailyK k1 = mapK.get(tradeDate_1);


                if (k1 == null) continue;
                if (k1.getHigh().compareTo(k1.getClose()) == 0
                        && k1.getOpen().compareTo(k1.getClose()) == 0
                        && k1.getPct().compareTo(new BigDecimal("9.5")) > 0) {

                } else {
                    continue;
                }

                int stopT = 1;
                for (int j = 2; j < 10; j++) {
                    //判断当前是几板
                    int index = i - j;
                    if (index < 0) {
                        break;
                    }


                    OpenEmCListEntity o_j = openList.get(index);
                    EmDailyK kj = mapK.get(o_j.getTradeDate());

                    if (kj != null) {
                        if (kj.getHigh().compareTo(kj.getClose()) == 0
                                && kj.getPct().compareTo(new BigDecimal("9.5")) > 0) {
                            stopT++;
                        }
                    }

                }


                BigDecimal ma60 = map.get(tradeDate);
                if (ma60 == null || ma60.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                BigDecimal f6Amt = o.getF6Amt();
                if (f6Amt.compareTo(BDUtil.B1000W) < 0) {
                    continue;
                }

                EmDailyK k = mapK.get(tradeDate);
                BigDecimal openPct = k.getOpen().subtract(k.getPreClose()).divide(k.getPreClose(), 4, RoundingMode.HALF_UP);
                o.setF3Pct(openPct.multiply(BDUtil.B100));
                if (o.getF3Pct().compareTo(new BigDecimal("9.5")) > 0 || o.getF3Pct().compareTo(BigDecimal.ZERO) < 0) {
                    continue;
                }


                BigDecimal openAmt60Pct = f6Amt.divide(ma60, 4, RoundingMode.HALF_UP);
                BigDecimal openK1AmtPct = f6Amt.divide(k1.getAmt(), 4, RoundingMode.HALF_UP);


                EmDailyK k2 = mapK.get(tds.get(tdi + 1));
                if (k2 == null) {
                    continue;
                }
                BigDecimal o2Pct = (k2.getOpen().subtract(o.getF2Close())).divide(o.getF2Close(), 4, RoundingMode.HALF_UP);
                log.info("\nname={} td={} openAmt={} openAmtPct={} openPct={} closePct={} open2Pct={} ", name + code, tradeDate, BDUtil.amtHuman(o.getF6Amt()), BDUtil.p100(openAmt60Pct), o.getF3Pct(), k.getPct(), BDUtil.p100(o2Pct));

                BigDecimal openKr = openK1AmtPct.compareTo(BigDecimal.ZERO) == 0 ? BDUtil.B100 : openAmt60Pct.divide(openK1AmtPct, 4, RoundingMode.HALF_UP);
                b.append(name)
                        .append(",").append(code)
                        .append(",").append(stopT)
                        .append(",").append(tradeDate)
                        .append(",").append((o.getF6Amt()))
                        .append(",").append(o.getF8Turnover())
                        .append(",").append(BDUtil.p100(openAmt60Pct))
                        .append(",").append(BDUtil.p100(openK1AmtPct))
                        .append(",").append(openKr)
                        .append(",").append((o.getF3Pct()))
                        .append(",").append((k.getPct()))
                        .append(",").append(BDUtil.p100(o2Pct)).append("\n");
            }

        }
        FileUtil.writeToFile("openPct.csv", b.toString());
    }


    @Test
    public void to() {
        List<EmCList> list = emClient.getClistDefaultSize(false);
        StringBuffer b = new StringBuffer();
        b.append("name").append(",").append("code").append(",").append("td").append(",")
                .append("amt").append(",")
                .append("ma5Pct_1").append(",")
                .append("ma60Pct_1").append(",")
                .append("ma5Pct").append(",")
                .append("ma60Pct").append(",")
                .append("open").append(",")
                .append("high").append(",")
                .append("low").append(",")
                .append("close").append(",")
                .append("pct").append(",").append("次开").append("\n");
        for (EmCList emCList : list) {
            String code = emCList.getF12Code();
            String name = emCList.getF14Name();

//            //先查db
            List<EmDailyKEntity> dbs = emDailyKRepo.findByCode(code);

            if (dbs.size() < 70){
                continue;
            }

            List<EmDailyK> ks = new ArrayList<>();

            for (EmDailyKEntity db : dbs) {
                EmDailyK k = new EmDailyK();
                BeanUtils.copyProperties(db, k);
                ks.add(k);
            }

            BigDecimal[] amtArr = ks.stream().map(EmDailyK::getAmt).toList().toArray(new BigDecimal[0]);

            BigDecimal[] ma5s = MaUtil.ma(5, amtArr, 2);
            BigDecimal[] ma60s = MaUtil.ma(60, amtArr, 2);



            for (int i = 60; i < ks.size() - 1; i++) {
                EmDailyK k = ks.get(i);
                EmDailyK k_1 = ks.get(i - 1);
                EmDailyK k1 = ks.get(i + 1);

                if (k.getPreClose().compareTo(BigDecimal.ZERO)==0){
                    continue;
                }

                if (k_1.getHigh().compareTo(k_1.getClose()) == 0
                        && k_1.getOpen().compareTo(k_1.getClose()) == 0
                        && k_1.getPct().compareTo(new BigDecimal("9.5")) > 0) {

                    if (k.getHigh().compareTo(k.getClose()) == 0
                            && k.getOpen().compareTo(k.getClose()) == 0
                            && k.getLow().compareTo(k.getClose()) == 0
                            && k.getPct().compareTo(new BigDecimal("9.5")) > 0) {

                    } else {
                        BigDecimal k1OpenPct = k1.getOpen().subtract(k.getClose()).divide(k.getClose(), 4, RoundingMode.HALF_UP);

                        BigDecimal ma5 = ma5s[i];
                        BigDecimal ma60 = ma60s[i];
                        BigDecimal ma5Pct = k.getAmt().divide(ma5, 4, RoundingMode.HALF_UP);
                        BigDecimal ma60Pct = k.getAmt().divide(ma60, 4, RoundingMode.HALF_UP);

                        BigDecimal ma5_1 = ma5s[i-1];
                        BigDecimal ma60_1 = ma60s[i-1];
                        BigDecimal ma5Pct_1 = k_1.getAmt().divide(ma5_1, 4, RoundingMode.HALF_UP);
                        BigDecimal ma60Pct_1 = k_1.getAmt().divide(ma60_1, 4, RoundingMode.HALF_UP);

                        b.append(name).append(",").append(code).append(",").append(k.getTradeDate()).append(",")
                                .append(BDUtil.amtHuman(k.getAmt())).append(",")

                                .append(BDUtil.p100(ma5Pct_1)).append(",")
                                .append(BDUtil.p100(ma60Pct_1)).append(",")

                                .append(BDUtil.p100(ma5Pct)).append(",")
                                .append(BDUtil.p100(ma60Pct)).append(",")
                                .append(BDUtil.p100((k.getOpen().subtract(k.getPreClose())).divide(k.getPreClose(),4,RoundingMode.HALF_UP))).append(",")
                                .append(BDUtil.p100((k.getHigh().subtract(k.getPreClose())).divide(k.getPreClose(),4,RoundingMode.HALF_UP))).append(",")
                                .append(BDUtil.p100((k.getLow().subtract(k.getPreClose())).divide(k.getPreClose(),4,RoundingMode.HALF_UP))).append(",")
                                .append(BDUtil.p100((k.getClose().subtract(k.getPreClose())).divide(k.getPreClose(),4,RoundingMode.HALF_UP))).append(",")
                                .append((k.getPct())).append(",").append(BDUtil.p100(k1OpenPct)).append("\n");
                    }
                }
            }
        }

        FileUtil.writeToFile("t_" + new Date().getTime() + ".csv", b.toString());
    }

    @Test
    public void openmarket() {
        String path = "/Users/dwd/Downloads/east/";
        List<String> list = FileUtil.readDirectoryDirsAbsPath(path);

        HashMap<String, List<String>> codeTdList = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            String dir = list.get(i);
            String td = dir.substring(dir.length() - 8);
            LocalDate ltd = DateUtil.strToLocalDate(td, "yyyyMMdd");
            if (ltd.getDayOfWeek().getValue() > 5) {
                continue;
            }
            List<String> files = FileUtil.readDirectoryFilesAbsPath(dir);
            for (String file : files) {
                if (file.startsWith(".")) {
                    continue;
                }
                //取 filename 最后 10个字符
                String code = file.substring(file.length() - 10, file.length() - 4);
                List<String> tdList = codeTdList.computeIfAbsent(code, k -> new ArrayList<>());
                tdList.add(td);
            }
        }

        for (String code : codeTdList.keySet()) {

            List<String> tdList = codeTdList.get(code);
            Collections.sort(tdList);
//            List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now(), 2000, false);

//            //先查db
            List<EmDailyKEntity> dbs = emDailyKRepo.findByCode(code);

            List<EmDailyK> ks = new ArrayList<>();

            for (EmDailyKEntity db : dbs) {
                EmDailyK k = new EmDailyK();
                BeanUtils.copyProperties(db, k);
                ks.add(k);
            }


            HashMap<String, EmDailyK> mapK = new HashMap<>();
            for (EmDailyK k : ks) {
                mapK.put(k.getTradeDate(), k);
            }

            for (String t : tdList) {
                String f = path + t + "/" + code + ".txt";
                EastGetStockFenShiVo vo = getFenshiByCodeFromLocal(code, f, true);
                if (vo == null) {
                    continue;
                }
                EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(vo);
                if (trans == null) {
                    continue;
                }

                if (trans.getOpenPrice() == null || trans.getOpenAmt() == null) {
                    continue;
                }

                EmDailyK k = mapK.get(t);
                if (k == null) {
                    continue;
                }
                if (trans.getOpenPrice().compareTo(k.getOpen()) != 0) {
                    continue;
                }

                OpenEmCListEntity entity = new OpenEmCListEntity();
                OpenEmCListEntity db = openEmCListRepo.findByF12CodeAndTradeDate(code, t);
                if (db != null) {
                    continue;
                }

                entity.setId(null);
                entity.setCreateTime(LocalDateTime.now());//  `create_time` datetime(6) DEFAULT NULL,
                entity.setF100bk(k.getBk());//  `f100bk` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                entity.setF10VolRatio(BigDecimal.ZERO);//  `f10vol_ratio` decimal(38,2) DEFAULT NULL,
                entity.setF12Code(code);//  `f12code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                entity.setF14Name(k.getName());//  `f14name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                entity.setF15High(trans.getOpenPrice());//  `f15high` decimal(38,2) DEFAULT NULL,
                entity.setF16Low(trans.getOpenPrice());//  `f16low` decimal(38,2) DEFAULT NULL,
                entity.setF17Open(trans.getOpenPrice());//  `f17open` decimal(38,2) DEFAULT NULL,
                entity.setF18Close(trans.getOpenPrice());//  `f18close` decimal(38,2) DEFAULT NULL,
                entity.setF1Amt(BigDecimal.ZERO);//  `f1amt` decimal(38,2) DEFAULT NULL,
                entity.setF22Speed(BigDecimal.ZERO);//  `f22speed` decimal(38,2) DEFAULT NULL,
                entity.setF23Pb(BigDecimal.ZERO);//  `f23pb` decimal(38,2) DEFAULT NULL,
                entity.setF2Close(trans.getOpenPrice());//  `f2close` decimal(38,2) DEFAULT NULL,
                BigDecimal od = k.getOpen().subtract(k.getPreClose());
                BigDecimal openPct = (od).divide(k.getPreClose(), 4, RoundingMode.HALF_UP);
                entity.setF3Pct(openPct);//  `f3pct` decimal(38,2) DEFAULT NULL,
                entity.setF4Chg(od);//  `f4chg` decimal(38,2) DEFAULT NULL,
                entity.setF5Vol(trans.getOpenVol());//  `f5vol` decimal(38,2) DEFAULT NULL,
                entity.setF6Amt(trans.getOpenAmt());//  `f6amt` decimal(38,2) DEFAULT NULL,
                entity.setF7Amp(BigDecimal.ZERO);//  `f7amp` decimal(38,2) DEFAULT NULL,
                entity.setF8Turnover(BigDecimal.ZERO);//  `f8turnover` decimal(38,2) DEFAULT NULL,
                entity.setF9Pe(BigDecimal.ZERO);//  `f9pe` decimal(38,2) DEFAULT NULL,
                entity.setOpenX(BigDecimal.ZERO);//  `openx` decimal(38,2) DEFAULT NULL,
                entity.setTradeDate(t);//  `trade_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                entity.setUpdateTime(LocalDateTime.now());//  `update_time` datetime(6) DEFAULT NULL,
                openEmCListRepo.save(entity);
            }

        }


    }

    public EastGetStockFenShiVo getFenshiByCodeFromLocal(String code, String path, boolean absPath) {

        try {
            String abs = "";
            if (absPath) {
                abs = path;
            } else {
                abs = path + "/" + code + ".txt";
            }
            String body = FileUtil.readFromFile(abs);
            JSONObject data = JSONObject.parseObject(body).getJSONObject("data");
            log.info("body length={}", body.length());

            if (data != null) {
                EastGetStockFenShiVo vo = data.toJavaObject(EastGetStockFenShiVo.class);
                return vo;
            }

        } catch (Exception e) {
            log.warn("get fenshi exception {} {}", e.getMessage(), e);
        }

        return null;
    }


    @Test
    public void testD() {
//        OpenEmCListEntity db = openEmCListRepo.findByF12CodeAndTradeDate("000004", "20231116");
//
//        System.out.println(db);

        ArrayList<String> list = new ArrayList<>();
        list.add("20220111");
        list.add("20230112");
        list.add("20231112");
        list.add("20231113");

        int i = list.indexOf("20230112");
        System.out.println(i);

        i = list.indexOf("202301121");
        System.out.println(i);

    }


}

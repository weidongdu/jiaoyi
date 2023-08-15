package pro.jiaoyi.eastm;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.model.K;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
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
import java.util.stream.Stream;

import static pro.jiaoyi.common.strategy.BreakOutStrategy.SIDE_MAP;

@SpringBootTest
@Slf4j
class EastmApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private EmClient emClient;

    @Test
    public void test() {
//        System.out.println("hello world");

//        List<DailyK> dailyKs = emClient.getDailyKs("002422", LocalDate.now(), 500);
//
//        List<CList> lists = emClient.getClistDefault(false);
//
//        for (CList list : lists) {
//            System.out.println(list);
//        }

        Map<String, String> nameCodeMap = emClient.getNameCodeMap(false);
        System.out.println(nameCodeMap);

        nameCodeMap = emClient.getNameCodeMap(true);
        System.out.println(nameCodeMap);

        for (int i = 0; i < 10; i++) {
            long l = System.currentTimeMillis();
            nameCodeMap = emClient.getNameCodeMap(false);
            long l2 = System.currentTimeMillis();
            System.out.println("use ms = " + (l2 - l));

        }


    }


    @Test
    public void simpleRead(String absPath, List<EmCList> list, List<EmCList> all) {
        // 写法2：
        EasyExcel.read(absPath, Index1000XlsData.class, new ReadListener<Index1000XlsData>() {

            private Set<String> set = new HashSet<>(1000);

            @Override
            public void invoke(Index1000XlsData data, AnalysisContext context) {
                set.add(data.getCode());
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                //遍历 all  , 将匹配到cache 的数据 存入list
                for (EmCList emCList : all) {
                    if (set.contains(emCList.getF12Code())) {
                        list.add(emCList);
                    }
                }
                set.clear();
            }

        }).sheet().doRead();


    }

    @Autowired
    private OkHttpUtil okHttpUtil;

    @Test
    public void down() {
//        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateUtil.PATTERN_yyyyMMdd_HHmm));
//        String dir = "/Users/dwd/Desktop/";
//        String filePath = dir + "zz1000_" + time + ".xls";
//        okHttpUtil.downloadFile(IndexEnum.ZZ1000.getUrl(), null, filePath);
//        List<EmCList> list = new ArrayList<>();
//        simpleRead(filePath, list, emClient.getClistDefaultSize(false));
//        System.out.println(list.size());
//        System.out.println(list);

        String nullStr = null;
        switch (nullStr) {
            default -> System.out.println("default");
        }
    }

//    @Test
//    public void zz1000(){
//        List<EmCList> index1000 = emClient.getIndex1000();
//        System.out.println(index1000);
//    }


    @Test
    public void backTest() {
        String code = "600283";

        String date = "20230525";
        List<EmDailyK> dailyKs = emClient.getDailyKs(code, LocalDate.now(), 500, false);
        BigDecimal[] closeArr = dailyKs.stream().map(EmDailyK::getClose).toList().toArray(new BigDecimal[0]);
        BigDecimal[] ma5 = MaUtil.ma(5, closeArr, 3);
        BigDecimal[] ma10 = MaUtil.ma(10, closeArr, 3);
        BigDecimal[] ma20 = MaUtil.ma(20, closeArr, 3);
        BigDecimal[] ma30 = MaUtil.ma(30, closeArr, 3);
        BigDecimal[] ma60 = MaUtil.ma(60, closeArr, 3);

        int size = dailyKs.size();
        for (int i = size - 1; i >= 0; i--) {
            int index = i;
            EmDailyK k = dailyKs.get(index);
            if (k.getTradeDate().equals(date)) {
                log.info("match k {} ", k.toString());

                if (k.getClose().compareTo(ma5[index]) > 0
                        && k.getClose().compareTo(ma10[index]) > 0
                        && k.getClose().compareTo(ma20[index]) > 0
                        && k.getClose().compareTo(ma30[index]) > 0
                        && k.getClose().compareTo(ma60[index]) > 0) {
                    log.info("MA UP");

                    log.info("seek for some days high");
                    int count = 0;
                    for (int j = 1; j < index - 1; j++) {
                        if (index - j == 1) {
                            count = j;
                            break;
                        }
                        BigDecimal high = dailyKs.get(index - j).getHigh();
                        if (high.compareTo(k.getClose()) >= 0) {
                            count = j;
                            break;
                        }
                    }
                    log.info("seek for some days high , count = {} high", count);

                    log.info("seek for box days high");
                    int countBox = 0;//箱体计数
                    for (int j = 1; j < count; j++) {
                        //1, 高点超过高点
                        //2, 低点低于高点
                        int tmpIndex = index - j;
                        BigDecimal high = dailyKs.get(tmpIndex).getHigh();
                        if (k.getLow().compareTo(high) < 0 && k.getHigh().compareTo(high) > 0) {
                            countBox++;
                        }
                    }
                    log.info("over box high , count = {} high", countBox);
                }
            }
        }


    }

    @Autowired
    private EmRealTimeClient emRealTimeClient;

    @Test
    public void test1() {
        List<EastSpeedInfo> speedTop = emRealTimeClient.getSpeedTop(50);
        for (EastSpeedInfo eastSpeedInfo : speedTop) {
            System.out.println(eastSpeedInfo);
        }
    }


    @Test
    public void codeName() {
        List<EmCList> clistDefaultSize = emClient.getClistDefaultSize(false);
        for (EmCList emCList : clistDefaultSize) {
            String content = (emCList.getF12Code() + " " + emCList.getF14Name());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("code_name.txt", true))) {
                writer.write(content);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void tu() throws InterruptedException {
        List<EmCList> list = emClient.getClistDefaultSize(false);

        String code = "601336";
        for (int i = 0; i < list.size(); i++) {
//            if (!list.get(i).getF12Code().equals(code)) {
//                continue;
//            }

            Thread.sleep(2000);

            List<EmDailyK> dailyKs = emClient.getDailyKs(list.get(i).getF12Code(), LocalDate.now(), 5000, false);

            if (dailyKs.size() < 70) {
                continue;
            }

            for (int j = 1; j < dailyKs.size() - 70; j++) {
                int end = dailyKs.size() - 1 - j;
                int tu = emRealTimeClient.tu(dailyKs.subList(Math.max(0, end - 100), end), 60, 60, 0.4d);
                if (tu != 0) {
                    BigDecimal bHigh = (dailyKs.get(end).getHigh().subtract(dailyKs.get(end - 1).getClose()))
                            .divide(dailyKs.get(end - 1).getClose(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);

                    BigDecimal bOpen = (dailyKs.get(end).getOpen().subtract(dailyKs.get(end - 1).getClose()))
                            .divide(dailyKs.get(end - 1).getClose(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);


                    //结论 就是 开盘价
                    // >0 可以
                    // <0 扔掉
                    String side = SIDE_MAP.get(tu);
                    log.info("{} 突破日{} 收盘{} 次日={} 开盘={} 最高={}", side, dailyKs.get(end - 1).getTradeDate() +
                                    " " + dailyKs.get(end - 1).getName() + dailyKs.get(end - 1).getCode() +
                                    " " + dailyKs.get(end - 1).getPct(),
                            dailyKs.get(end - 1).getPct(),
                            dailyKs.get(end).getPct(),
                            bOpen,
                            bHigh
                    );
                }
            }

        }

    }

    public static void main(String[] args) {
        ArrayList<Integer> integers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            integers.add(i + 1);
        }

        System.out.println(integers.subList(0, integers.size() - 1));


    }


    @Test
    public void vol() {
        String[] codes = {"300161", "301046", "600739", "600848"};
        vol(codes);
    }

    public void vol(String[] codes) {

        JSONObject map = new JSONObject();
        for (int i = 0; i < codes.length; i++) {
            String code = codes[i];
            List<EmDailyK> dailyKs = emClient.getDailyKs(code, LocalDate.now(), 100, false);
            List<BigDecimal> volList = new ArrayList<>(dailyKs.stream().map(EmDailyK::getAmt).toList());
            Collections.sort(volList);
            BigDecimal volTop10 = BigDecimal.ZERO;
            for (int j = 0; j < 7; j++) {
                int index = volList.size() - 1 - j;
                volTop10 = volTop10.add(volList.get(index));
            }
            volTop10 = volTop10.divide(new BigDecimal(7), 0, RoundingMode.HALF_UP);
            BigDecimal volAvgHour = volTop10.divide(new BigDecimal(4), 0, RoundingMode.HALF_UP);

            log.info("code {} vol one hour {}", code, volAvgHour);
            if (code.length() == 5) {
                code += " HK";
            }
            map.put(code, volAvgHour);
        }

        String base = "http://8.142.9.14:20808";
        String url = base + "/stock/vol/avg/code/set";
        try {
            byte[] bytes = okHttpUtil.postJsonForBytes(url, null, map.toJSONString());
            String s = new String(bytes);
            JSONObject js = JSONObject.parseObject(s);
            log.info("js {}", js);
        } catch (Exception e) {

            log.error("exception {} {}", e.getMessage(), e);
        }

    }

    @Test
    public void fenshi() {
        String code = "300928";
        List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now(), 100, false);
        BigDecimal bigDecimal = emClient.amtTop10p(ks);
        BigDecimal amtHour = bigDecimal.divide(new BigDecimal(4), 0, RoundingMode.HALF_UP);
        BigDecimal a60 = emRealTimeClient.getFenshiAmt(code, 60);
        BigDecimal a70 = emRealTimeClient.getFenshiAmt(code, 70);
        System.out.println(a60 + " " + a60.divide(amtHour, 3, RoundingMode.HALF_UP));
        System.out.println(a70 + " " + a70.divide(amtHour, 3, RoundingMode.HALF_UP));

    }


    @Test
    public void tBack() throws InterruptedException {
        List<EmCList> list = emClient.getClistDefaultSize(false);

        String fileName = "上影线" + DateUtil.today() + ".csv";
        String sb1 = "code" + "," +
                "name" + "," +
                "date" + "," +
                "high" + "," +
                "max" + "," +
                "min" + "\n";
        FileUtil.writeToFile(fileName, sb1);


        String code = "605366";
        for (int i = 0; i < list.size(); i++) {
//            if (!list.get(i).getF12Code().equals(code)) {
//                continue;
//            }
            Thread.sleep(2000);
            List<EmDailyK> dailyKs = emClient.getDailyKs(list.get(i).getF12Code(), LocalDate.now(), 5000, false);
            if (dailyKs.size() < 70) {
                continue;
            }

            if (dailyKs.get(0).getPct().compareTo(BigDecimal.ZERO) < 0) {
                continue;
            }
            Map<String, BigDecimal[]> ma = MaUtil.ma(dailyKs);
            BigDecimal[] ma5 = ma.get("ma5");
            BigDecimal[] ma10 = ma.get("ma10");
            BigDecimal[] ma20 = ma.get("ma20");
            BigDecimal[] ma30 = ma.get("ma30");
            BigDecimal[] ma60 = ma.get("ma60");
            BigDecimal[] ma120 = ma.get("ma120");
            BigDecimal[] ma250 = ma.get("ma250");

            //up T

            for (int j = 250; j < dailyKs.size(); j++) {
                EmDailyK k = dailyKs.get(j);
                BigDecimal[] ochl = k.ochl();
                BigDecimal o = ochl[0];
                BigDecimal c = ochl[1];
                BigDecimal h = ochl[2];
                BigDecimal l = ochl[3];

                if (o.compareTo(BigDecimal.ZERO) < 0) continue;
                if (c.compareTo(BigDecimal.ZERO) < 0) continue;
                if (h.compareTo(BigDecimal.ZERO) < 0) continue;
                if (l.compareTo(BigDecimal.ZERO) < 0) continue;

                //1 pct > 0 , pct< 3
                if (k.getPct().compareTo(BigDecimal.ZERO) <= 0 || k.getPct().compareTo(new BigDecimal(3)) >= 0) {
                    continue;
                }

                if (k.getClose().compareTo(ma5[j]) < 0) continue;
                if (k.getClose().compareTo(ma10[j]) < 0) continue;
                if (k.getClose().compareTo(ma20[j]) < 0) continue;
                if (k.getClose().compareTo(ma30[j]) < 0) continue;
                if (k.getClose().compareTo(ma60[j]) < 0) continue;
                if (k.getClose().compareTo(ma120[j]) < 0) continue;
                if (k.getClose().compareTo(ma250[j]) < 0) continue;

                if (k.getOpen().compareTo(k.getClose()) >= 0) {
                    continue;
                }

                BigDecimal shadow = h.subtract(c);
                BigDecimal body = c.subtract(o);
                if (shadow.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                BigDecimal bodyPct = body.divide(shadow, 3, RoundingMode.HALF_UP);
                if (bodyPct.compareTo(BDUtil.b0_2) > 0) {
                    continue;
                }
                if (o.subtract(l).compareTo(h.subtract(c)) > 0) {
                    continue;
                }
//                后续涨幅


                int highDays = 0;

                //查看创了多少日新高
                for (int m = 0; m < j; m++) {
                    int index = j - m;
                    EmDailyK preK = dailyKs.get(index);
                    if (preK.getHigh().compareTo(k.getHigh()) > 0 || index == 1) {
                        highDays = m;
                        log.info("满足上影线{}，创了{}日新高", k, m);

                        ArrayList<BigDecimal> pcts = new ArrayList<>();
                        for (int mp = 1; mp < 30; mp++) {
                            int indexMp = Math.min(j + mp, dailyKs.size() - 1);
                            EmDailyK afterK = dailyKs.get(indexMp);
                            BigDecimal diff = afterK.getHigh().subtract(c);
                            BigDecimal pct = diff.divide(c, 3, RoundingMode.HALF_UP);
                            pcts.add(pct);
                        }
                        Collections.sort(pcts);
                        log.warn("满足上影线{}，{}新高, 后续30日涨幅 max={} min={}", k.getTradeDate(), highDays, BDUtil.p100(pcts.get(pcts.size() - 1)), BDUtil.p100(pcts.get(0)));
                        String sb = k.getCode() + "," +
                                k.getName() + "," +
                                k.getTradeDate() + "," +
                                highDays + "," +
                                BDUtil.p100(pcts.get(pcts.size() - 1)) + "," +
                                BDUtil.p100(pcts.get(0)) + "\n";

                        FileUtil.writeToFile(fileName, sb);
                        break;
                    }

                }


            }


        }

    }


    @Test
    public List<String[]> getCsv(String[] head, String file) {

//        String[] head = {"code", "name", "date", "high", "max", "min"};
        ArrayList<String[]> csv = new ArrayList<>();
        csv.add(head);
//        Path path = Paths.get("/Users/dwd/dev/GitHub/jiaoyi/eastm/上影线20230627.csv");
//        String file = "/Users/dwd/dev/GitHub/jiaoyi/eastm/上影线20230627.csv";
        Path path = Paths.get(file);
        try (Stream<String> lines = Files.lines(path)) {
            StringBuilder stringBuilder = new StringBuilder();
//            lines.forEach(stringBuilder::append);
            lines.forEach(line -> {
                if (line.contains(head[0])) {
                    return;
                }
                String[] split = line.split(",");
                csv.add(split);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return csv;
    }


    @Test
    public void must() {
        List<EmCList> must = emClient.must();
        log.info("{}", must.size());
    }


    @Test
    public void fenshi1() {
//        String code = "000002";

        List<EmCList> all = emClient.getClistDefaultSize(false);
        List<String> openHighList = all.stream().filter(em -> {
            BigDecimal pre = em.getF18Close();
            BigDecimal open = em.getF17Open();
            if (pre.compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }

            BigDecimal openPct = open.divide(pre, 4, RoundingMode.HALF_UP);
            return openPct.compareTo(new BigDecimal("1.005")) > 0 &&
                    openPct.compareTo(new BigDecimal("1.03")) < 0;
        }).map(EmCList::getF12Code).toList();


        for (String code : openHighList) {

            EastGetStockFenShiVo f = emRealTimeClient.getFenshiByCode(code);
            LocalDateTime openTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 30));
            if (f != null) {
                EastGetStockFenShiTrans trans = EastGetStockFenShiTrans.trans(f);
                assert trans != null;
                List<DetailTrans> list = trans.getData();

                List<DetailTrans> open60S = new ArrayList<>();
                List<DetailTrans> open60SOver = new ArrayList<>();

                for (int i = 0; i < list.size(); i++) {
                    DetailTrans detailTrans = list.get(i);
                    if (detailTrans.getTs() >= DateUtil.toTimestamp(openTime)
                            && detailTrans.getTs() <= DateUtil.toTimestamp(openTime.plusSeconds(60))) {
                        //开盘 判断是否高开
                        open60S.add(detailTrans);
                    }
                }

                System.out.println("open30S size " + open60S.size());
            }
        }


    }


    @Test
    public void x10() {
        List<EmCList> list = emClient.getClistDefaultSize(false);
        int size = list.size();
        LocalDate now = LocalDate.now();
        String head = "TradeDate" + "," + "Code" + "," + "Name" + "," + "收盘" + "," + "收盘P" + "," + "amt" + "," + "ma60" + "," + "次开P" + "," + "次开" + "," + "次收P" + "," + "次收" + "," + "收-开" + "," + "最大" + "," + "几日" + "," + "最小" + "," + "几日" + "\n";
        FileUtil.writeToFile("x10_5.csv", head);


        for (int i = 0; i < list.size(); i++) {
            EmCList emCList = list.get(i);
            log.info("{}/{} code={} name={}", i, size, emCList.getF12Code(), emCList.getF14Name());
            List<EmDailyK> ks = emClient.getDailyKs(emCList.getF12Code(), now, 1000, false);
            if (ks.size() < 100) {
                log.info("code={} name={} size={}", emCList.getF12Code(), emCList.getF14Name(), ks.size());
                continue;
            }

            BigDecimal[] amtArray = ks.stream().map(EmDailyK::getAmt).toList().toArray(new BigDecimal[0]);
            BigDecimal[] amtMa60 = MaUtil.ma(60, amtArray, 2);

            for (int j = 60; j < ks.size() - 5; j++) {
                EmDailyK k = ks.get(j);
                BigDecimal ma60 = amtMa60[j];
                if (k.getAmt().compareTo(ma60.multiply(new BigDecimal("10"))) > 0) {
                    log.info("match amt x10 amt={} ma60={} {}", BDUtil.amtHuman(k.getAmt()), BDUtil.amtHuman(ma60), JSON.toJSONString(k));
                    //计算后续60日涨幅
                    List<BigDecimal> priceList = new ArrayList<>();
                    for (int l = j + 1; l < Math.min(j + 30, ks.size()); l++) {
                        priceList.add(ks.get(l).getClose());
                    }
                    BigDecimal[] priceArray = priceList.toArray(new BigDecimal[0]);

                    Collections.sort(priceList);
                    BigDecimal min = priceList.get(0);
                    BigDecimal max = priceList.get(priceList.size() - 1);

                    BigDecimal pctMax = max.subtract(k.getClose()).divide(k.getClose(), 3, RoundingMode.HALF_UP);
                    BigDecimal pctMin = min.subtract(k.getClose()).divide(k.getClose(), 3, RoundingMode.HALF_UP);

                    int maxI = -1;
                    int minI = -1;
                    for (int l = 0; l < priceArray.length; l++) {
                        if (priceArray[l].compareTo(max) == 0) {
                            maxI = l;
                        }
                        if (priceArray[l].compareTo(min) == 0) {
                            minI = l;
                        }
                    }

                    log.info("price={} pctMax={} maxI={} pctMin={} minI={}",
                            k.getTradeDate() + "_" + k.getClose(), BDUtil.p100(pctMax) + "_" + max, maxI, BDUtil.p100(pctMin) + "_" + min, minI);


                    EmDailyK k1 = ks.get(j + 1);
                    BigDecimal openP = k1.getOpen().subtract(k.getClose()).divide(k.getClose(), 2, RoundingMode.HALF_UP);
                    BigDecimal closeP = k1.getClose().subtract(k.getClose()).divide(k.getClose(), 2, RoundingMode.HALF_UP);
                    String content = k.getTradeDate() + "," + k.getCode() + "," + k.getName() + "," + k.getClose() + "," + k.getPct() + "," + BDUtil.amtHuman(k.getAmt()) + "," + BDUtil.amtHuman(ma60) + "," + BDUtil.p100(openP) + "," + k1.getOpen() + "," + BDUtil.p100(closeP) + "," + BDUtil.p100(k1.getClose()) + "," + BDUtil.p100(closeP.subtract(openP).setScale(4, RoundingMode.HALF_UP)) + "," + BDUtil.p100(pctMax) + "," + maxI + "," + BDUtil.p100(pctMin) + "," + minI + "\n";
                    FileUtil.writeToFile("x10_5.csv", content);

                    j += 5;
                }
            }
        }
    }

    @Test
    public void testVipIndex() {
        String code = VipIndexEnum.index_000001.getCode();
        List<EmDailyK> dailyKs = emClient.getDailyKs(code, LocalDate.now(), 500, false);
        for (EmDailyK dailyK : dailyKs) {
            log.info("{}", dailyK);
        }
    }


    @Autowired
    private KLineRepo kLineRepo;


    @Test
    public void getAll() {

        List<EmCList> list = emClient.getClistDefaultSize(false);

        LocalDate end = LocalDate.now();//.minusDays(1);
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

}

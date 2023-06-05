package pro.jiaoyi.eastm;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.api.EmRealTimeClient;
import pro.jiaoyi.eastm.model.EastSpeedInfo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.model.excel.Index1000XlsData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

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
    public void tu() throws InterruptedException {
        List<EmCList> list = emClient.getClistDefaultSize(false);
        for (int i = 0; i < list.size(); i++) {
            Thread.sleep(2000);
            List<EmDailyK> dailyKs = emClient.getDailyKs(list.get(i).getF12Code(), LocalDate.now(), 500, false);

            if (dailyKs.size() < 70) {
                continue;
            }

            for (int j = 1; j < dailyKs.size() - 70; j++) {
                int end = dailyKs.size() - 1 - j;
                boolean tu = emRealTimeClient.tu(dailyKs.subList(0, end), 60, 60, 0.4d);
                if (tu) {
                    BigDecimal bHigh = (dailyKs.get(end).getHigh().subtract(dailyKs.get(end - 1).getClose()))
                            .divide(dailyKs.get(end - 1).getClose(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);

                    BigDecimal bOpen = (dailyKs.get(end).getOpen().subtract(dailyKs.get(end - 1).getClose()))
                            .divide(dailyKs.get(end - 1).getClose(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);


                    //结论 就是 开盘价
                    // >0 可以
                    // <0 扔掉
                    log.info("突破日 {} 次日={} 开盘={} 最高={}", dailyKs.get(end - 1).getTradeDate() +
                                    " " + dailyKs.get(end - 1).getName() + dailyKs.get(end - 1).getCode() +
                                    " " + dailyKs.get(end - 1).getPct(),
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
        String[] codes = {"300025","600212","600597","600458","300856","300573","301153","603038","300580","002028"};
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
}

package pro.jiaoyi.eastm;

import com.alibaba.fastjson2.JSONArray;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pro.jiaoyi.common.indicator.TDUtil;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.dao.entity.OpenEmCListEntity;
import pro.jiaoyi.eastm.dao.repo.EmCListSimpleEntityRepo;
import pro.jiaoyi.eastm.dao.repo.OpenEmCListRepo;
import pro.jiaoyi.eastm.flow.job.DailyJob;
import pro.jiaoyi.eastm.flow.KlineFlow;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.service.FenshiAmtSummaryService;
import pro.jiaoyi.eastm.service.SpeedService;
import pro.jiaoyi.eastm.util.TradeTimeUtil;
import pro.jiaoyi.eastm.util.sina.SinaTicktimeDataUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@Slf4j
class FlowTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private EmClient emClient;
    @Resource
    private SpeedService speedService;

    @Resource
    private FenshiAmtSummaryService fenshiAmtSummaryService;

    @Test
    public void test() {
        fenshiAmtSummaryService.executeSummary();
    }

    @Resource
    private SinaTicktimeDataUtil sinaTicktimeDataUtil;

    @Test
    public void testSinaSummary() {
//        String file = "/Users/dwd/dev/GitHub/jiaoyi/eastm/ticktime/2024-03-11/sz300750.json";
        String file = "/Users/dwd/dev/GitHub/jiaoyi/eastm/ticktime/2024-03-11/sh600941.json";
        String s = FileUtil.readFromFile(file);
        JSONArray array = JSONArray.parseArray(s);

        sinaTicktimeDataUtil.timeGo(array, "2024-03-11");
////        sinaTicktimeDataUtil.getTicktimeData("000001", "2024-03-10");
//

//        String dir = "ticktime";
//        List<String> dirs = FileUtil.readDirectoryDirsAbsPath(dir);
//        Collections.sort(dirs);
//        for (int i = 0; i < dirs.size(); i++) {
//            log.info("{}/{}", i, dirs.size());
//            String d = dirs.get(i);
//
//
//            String day = d.substring(d.lastIndexOf("/") + 1);
//            List<String> files = FileUtil.readDirectoryFilesAbsPath(d);
//            Collections.sort(files);
//            int count = 0;
//            for (String file : files) {
//                log.info("file index:{},size:{}", count++, files.size());
//                try {
//                    String s = FileUtil.readFromFile(file);
//                    JSONArray array = JSONArray.parseArray(s);
//                    sinaTicktimeDataUtil.timeGo(array, day);
//                } catch (Exception e) {
//                    log.error("{}", e);
//                }
//
//            }
//        }
    }

    @Test
    public void testSina() {
        List<EmCList> list = emClient.getClistDefaultSize(true);
        boolean flag = false;
        for (EmCList em : list) {
            //时间 start 2024-02-19 -> now
            //时间 end now

//            String s = "000650";
//            if (s.equals(em.getF12Code())) {
//                flag = true;
//            }
//            if (!flag) {
//                continue;
//            }

            if (em.getF5Vol().compareTo(BigDecimal.ONE) <= 0
                    || em.getF2Close().compareTo(BigDecimal.ONE) <= 0
                    || em.getF15High().compareTo(BigDecimal.ONE) <= 0
            ) {
                log.info("pass code:{},name:{},vol:{},close:{}", em.getF12Code(), em.getF14Name(), em.getF5Vol(), em.getF2Close());
                continue;
            }


            String symbol = em.getF12Code();
            for (int i = 0; i < 30; i++) {
                LocalDate start = LocalDate.of(2024, 2, 20);
                LocalDate limit = LocalDate.of(2024, 3, 9);
                start = start.plusDays(i);
                if (!TradeTimeUtil.isTradeDay(start) || start.isAfter(limit)) {
                    log.info("{} 非交易日", start);
                    continue;
                }

                String day = start.toString();
                try {
                    sinaTicktimeDataUtil.getTicktimeData(symbol, day);
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.error("{}", e);
                }
            }

        }
    }

    @Resource
    private OpenEmCListRepo openEmCListRepo;

    @Test
    public void testVolOpen() {
        //高开计算
        List<OpenEmCListEntity> openHigh = openEmCListRepo.findOpenHigh();
        int x = 30;
        for (OpenEmCListEntity em : openHigh) {
            log.info("\n\n\n{}", em);
            List<EmDailyK> ks = emClient.getDailyKs(em.getF12Code(), LocalDate.now(), 500, false);
            int index = 0;
            for (int i = x; i < ks.size(); i++) {
                EmDailyK k = ks.get(i);
                if (k.getTradeDate().equals(em.getTradeDate())) {
                    log.info("hit index={} {} ", i, k);
                    index = i;
                    break;
                }
            }
            if (index < x) {
                continue;
            }

            List<EmDailyK> subList = new ArrayList<>();
            int start = Math.max(0, index - x);
            BigDecimal high60 = BigDecimal.ZERO;
            for (int i = start; i < index; i++) {
                EmDailyK k = ks.get(i);
                subList.add(k);

                BigDecimal close = k.getClose();
                if (close.compareTo(high60) >= 0) {
                    high60 = close;
                }
            }

            BigDecimal amtDay = emClient.amtTop10p(subList);
            BigDecimal hourAmt = amtDay.divide(new BigDecimal(4), 2, RoundingMode.HALF_UP);
            BigDecimal fAmt = hourAmt.multiply(BDUtil.b0_1);

            if (em.getF2Close().compareTo(high60) > 0) {
                log.info("高开{} {} 60日新高 code:{},name:{},tradeDate:{},close:{},high60:{}", em.getF3Pct(), BDUtil.amtHuman(em.getF6Amt()), em.getF12Code(), em.getF14Name(), em.getTradeDate(), em.getF2Close(), high60);
//                前面5天的最高价涨幅(前面5天的最高价涨幅);
                boolean day5Flag = false;
                for (int i = 1; i < 5; i++) {
                    EmDailyK k = ks.get(index - i);
                    BigDecimal pct = em.getF2Close().subtract(k.getClose()).divide(k.getClose(), 4, RoundingMode.HALF_UP);
                    if (pct.compareTo(new BigDecimal("0.1")) > 0){
                        day5Flag = true;
                        break;
                    }
                }
                if (day5Flag){
                    log.info("前面5天 涨幅过大");
                    continue;
                }

                for (int i = 1; i < 5; i++) {
                    EmDailyK k = ks.get(index - i);
                    BigDecimal pct = em.getF2Close().subtract(k.getClose()).divide(k.getClose(), 4, RoundingMode.HALF_UP);
                    log.info("<= {} code:{},name:{},tradeDate:{},pct:{}", i, k.getCode(), k.getName(), k.getTradeDate(), BDUtil.p100(pct));
                }

                //后面5天的最高价涨幅
                BigDecimal fx = em.getF6Amt().divide(fAmt, 4, RoundingMode.HALF_UP);
                if (fx.compareTo(BigDecimal.valueOf(20)) > 0) {
                    log.warn("高开{} {} 60日新高 且放量{} code:{},name:{},tradeDate:{},close:{},high60:{}", em.getF3Pct(), BDUtil.amtHuman(em.getF6Amt()), fx, em.getF12Code(), em.getF14Name(), em.getTradeDate(), em.getF2Close(), high60);
                    //后面5天的最高价涨幅
                    for (int i = 0; i < 5; i++) {
                        if (index + i >= ks.size()) {
                            break;
                        }
                        EmDailyK k = ks.get(index + i);
                        BigDecimal pct = k.getClose().subtract(em.getF2Close()).divide(em.getF2Close(), 4, RoundingMode.HALF_UP);
                        log.warn("=> {} code:{},name:{},tradeDate:{},pct:{}", i, k.getCode(), k.getName(), k.getTradeDate(), BDUtil.p100(pct));
                    }
                } else {
                    for (int i = 0; i < 5; i++) {
                        if (index + i >= ks.size()) {
                            break;
                        }
                        EmDailyK k = ks.get(index + i);
                        BigDecimal pct = k.getClose().subtract(em.getF2Close()).divide(em.getF2Close(), 4, RoundingMode.HALF_UP);
                        log.info("code:{},name:{},tradeDate:{},pct:{}", k.getCode(), k.getName(), k.getTradeDate(), BDUtil.p100(pct));
                    }
                }

            }


        }
    }


//    public void td(){
//        List<EmCList> list = emClient.getClistDefaultSize(false);
//        for (EmCList em : list) {
//            List<EmDailyK> ks = emClient.getDailyKs(em.getF12Code(), LocalDate.now(), 100, false);
//            if (ks.size() < 100) {
//                continue;
//            }
//            System.out.println();
//            TDUtil.tds(ks);
//        }
//    }

//    public void run() {
//        List<EmCList> list = emClient.getClistDefaultSize(false);
//        log.info("{}", list.size());
//        //写入csv
//        String file = "yi.csv";
//
//        StringBuilder stringBuffer = new StringBuilder();
//        stringBuffer.append("code,code,name,name,tradeDate,tradeDate,open,open,close,close,postOpen,postOpen,postClose,postClose\n");
//        for (EmCList em : list) {
//            String code = em.getF12Code();
//            if (em.getF14Name().contains("ST")) {
//                continue;
//            }
//            if (em.getF12Code().startsWith("8") || em.getF12Code().startsWith("4")) {
//                continue;
//            }
//
//            List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now(), 500, false);
//            if (ks.size() < 250) {
//                continue;
//            }
//
//            for (int i = 1; i < ks.size() - 1; i++) {
//                EmDailyK k = ks.get(i);
//                EmDailyK pre = ks.get(i - 1);
//                EmDailyK post = ks.get(i + 1);
//                if (
//                        pre.getClose().compareTo(BigDecimal.ONE) > 0
//                                && pre.getClose().compareTo(pre.getOpen()) == 0
//                                && pre.getClose().compareTo(pre.getHigh()) == 0
//                                && pre.getClose().compareTo(pre.getLow()) == 0
//                                && pre.getHsl().compareTo(BDUtil.B3) < 0
//                                && pre.getPct().compareTo(BDUtil.B3) > 0
//                ) {
//                    BigDecimal openPct = k.getOpen().subtract(pre.getClose()).divide(pre.getClose(), 4, RoundingMode.HALF_UP);
//                    BigDecimal closePct = k.getClose().subtract(pre.getClose()).divide(pre.getClose(), 4, RoundingMode.HALF_UP);
//
//                    BigDecimal postOpenPct = post.getOpen().subtract(pre.getClose()).divide(pre.getClose(), 4, RoundingMode.HALF_UP);
//                    BigDecimal postClosePct = post.getClose().subtract(pre.getClose()).divide(pre.getClose(), 4, RoundingMode.HALF_UP);
//
//
//                    stringBuffer.append(code).append(",")
//                            .append(em.getF14Name()).append(",")
//                            .append(k.getTradeDate()).append(",")
//                            .append(openPct).append(",")
//                            .append(closePct).append("\n");
//                    log.info("code,{},name,{},date,{},openPct,{},closePct,{},postOpenPct,{},postClosePct,{}",
//                            k.getCode(), k.getName(), k.getTradeDate(),
//                            BDUtil.p100(openPct), BDUtil.p100(closePct)
//                            , BDUtil.p100(postOpenPct), BDUtil.p100(postClosePct)
//                    );
//
//                    int gap = 5;
//                    if (i + gap < ks.size()) {
//                        //这里是为了过滤出第一次满足条件的
//                        i = i + gap;
//                    }else {
//                        // 结束本次循环
//                        break;
//                    }
//                }
//            }
//        }
//        FileUtil.writeToFile(file, stringBuffer.toString());
//    }


//    public static void main(String[] args) throws IOException, InterruptedException {
//        HashSet<String> set = new HashSet<>();
//
//
//        System.out.println("set.size()"+set.size());
//
//        int count = 0;
//        for (String s : set) {
//            count++;
//            System.out.println("count:"+count);
//            String url = "http://10.0.96.145:18823/api/sid/delete" ;
//            url = url + "?sessionId=" + s;
//            System.out.println(url);
//            Thread.sleep(500);
//            System.out.println("Sending 'GET' request to URL : " + url);
//            // 创建 URL 对象
//            URL obj = null;
//            try {
//                obj = new URL(url);
//            } catch (MalformedURLException e) {
//                throw new RuntimeException(e);
//            }
//
//            // 打开连接
//            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
//
//            // 设置请求方法为 GET
//            con.setRequestMethod("GET");
//
//            // 获取响应码
//            int responseCode = con.getResponseCode();
//            System.out.println("GET Response Code :: " + responseCode);
//            // 成功获取到响应数据
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//                String inputLine;
//                StringBuilder response = new StringBuilder();
//
//                while ((inputLine = in.readLine()) != null) {
//                    response.append(inputLine);
//                }
//
//                in.close();
//
//                System.out.println("Response body: " + response.toString());
//            } else {
//                System.out.println("GET request failed with response code: " + responseCode);
//            }
//        }
//    }


}

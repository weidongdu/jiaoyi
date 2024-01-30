package pro.jiaoyi.eastm;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.dao.repo.EmCListSimpleEntityRepo;
import pro.jiaoyi.eastm.flow.job.DailyJob;
import pro.jiaoyi.eastm.flow.KlineFlow;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.service.SpeedService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    @Test
    public void run() {
        List<EmCList> list = emClient.getClistDefaultSize(false);
        log.info("{}", list.size());
        //写入csv
        String file = "yi.csv";

        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append("code,code,name,name,tradeDate,tradeDate,open,open,close,close,postOpen,postOpen,postClose,postClose\n");
        for (EmCList em : list) {
            String code = em.getF12Code();
            if (em.getF14Name().contains("ST")) {
                continue;
            }
            if (em.getF12Code().startsWith("8") || em.getF12Code().startsWith("4")) {
                continue;
            }

            List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now(), 500, false);
            if (ks.size() < 250) {
                continue;
            }

            for (int i = 1; i < ks.size() - 1; i++) {
                EmDailyK k = ks.get(i);
                EmDailyK pre = ks.get(i - 1);
                EmDailyK post = ks.get(i + 1);
                if (
                        pre.getClose().compareTo(BigDecimal.ONE) > 0
                                && pre.getClose().compareTo(pre.getOpen()) == 0
                                && pre.getClose().compareTo(pre.getHigh()) == 0
                                && pre.getClose().compareTo(pre.getLow()) == 0
                                && pre.getHsl().compareTo(BDUtil.B3) < 0
                                && pre.getPct().compareTo(BDUtil.B3) > 0
                ) {
                    BigDecimal openPct = k.getOpen().subtract(pre.getClose()).divide(pre.getClose(), 4, RoundingMode.HALF_UP);
                    BigDecimal closePct = k.getClose().subtract(pre.getClose()).divide(pre.getClose(), 4, RoundingMode.HALF_UP);

                    BigDecimal postOpenPct = post.getOpen().subtract(pre.getClose()).divide(pre.getClose(), 4, RoundingMode.HALF_UP);
                    BigDecimal postClosePct = post.getClose().subtract(pre.getClose()).divide(pre.getClose(), 4, RoundingMode.HALF_UP);


                    stringBuffer.append(code).append(",")
                            .append(em.getF14Name()).append(",")
                            .append(k.getTradeDate()).append(",")
                            .append(openPct).append(",")
                            .append(closePct).append("\n");
                    log.info("code,{},name,{},date,{},openPct,{},closePct,{},postOpenPct,{},postClosePct,{}",
                            k.getCode(), k.getName(), k.getTradeDate(),
                            BDUtil.p100(openPct), BDUtil.p100(closePct)
                            , BDUtil.p100(postOpenPct), BDUtil.p100(postClosePct)
                    );

                    int gap = 5;
                    if (i + gap < ks.size()) {
                        //这里是为了过滤出第一次满足条件的
                        i = i + gap;
                    }else {
                        // 结束本次循环
                        break;
                    }
                }
            }
        }
        FileUtil.writeToFile(file, stringBuffer.toString());
    }


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

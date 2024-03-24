package pro.jiaoyi.eastm;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.dao.entity.OpenEmCListEntity;
import pro.jiaoyi.eastm.dao.repo.OpenEmCListRepo;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.service.FenshiAmtSummaryService;
import pro.jiaoyi.eastm.service.SpeedService;
import pro.jiaoyi.eastm.util.TradeTimeUtil;
import pro.jiaoyi.eastm.util.sina.SinaTicktimeDataUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@Slf4j
class SinaTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private EmClient emClient;

    @Resource
    private SinaTicktimeDataUtil sinaTicktimeDataUtil;


    @Test
    public void testSinaSummary() {
        String dir = "/Users/dwd/dev/GitHub/jiaoyi/eastm/ticktime/";
        List<String> dirs = FileUtil.readDirectoryDirsAbsPath(dir);

        for (String s : dirs) {
            String day = s.substring(s.lastIndexOf("/") + 1);
            List<String> files = FileUtil.readDirectoryFilesAbsPath(s);

            for (String file : files) {
                testSinaSummary(file, day);
            }
        }

    }

    public void testSinaSummary(String file, String day) {
        day = day.replace("-", "");
//        String file = "/Users/dwd/dev/GitHub/jiaoyi/eastm/ticktime/2024-03-11/sz300750.json";
//        String file = "/Users/dwd/dev/GitHub/jiaoyi/eastm/ticktime/2024-03-11/sh600941.json";
        String s = FileUtil.readFromFile(file);
        JSONArray array = JSONArray.parseArray(s);

        ArrayList<BigDecimal> upList = new ArrayList<>();
        ArrayList<BigDecimal> dnList = new ArrayList<>();
        ArrayList<BigDecimal> zList = new ArrayList<>();
        String symbol = "";
        String name = "";

        for (int i = 0; i < array.size(); i++) {
            //{"volume":"23176","prev_price":"15.700","symbol":"bj430047","price":"15.450","kind":"D","name":"诺思兰德","ticktime":"09:34:19"}
            JSONObject json = array.getJSONObject(i);

            symbol = json.getString("symbol");
            name = json.getString("name");

            String ticktime = json.getString("ticktime");
            String price = json.getString("price");
            String prev_price = json.getString("prev_price");
            String volume = json.getString("volume");
            String kind = json.getString("kind");


            BigDecimal amt = new BigDecimal(price).multiply(new BigDecimal(volume));
            if (amt.compareTo(BDUtil.B100W) < 0) {
                continue;
            }
            if (kind.equals("D")) {
                dnList.add(amt);
            } else if (kind.equals("U")) {
                upList.add(amt);
            } else {
                zList.add(amt);
            }

        }

        BigDecimal upAmt = upList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dnAmt = dnList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal zAmt = zList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        if (upAmt.compareTo(BDUtil.B1Y) < 0 || dnAmt.compareTo(BDUtil.B1Y) < 0) {
            return;
        }

        int limit = 100;
        List<BigDecimal> sortedUpList = upList.stream().sorted(Comparator.reverseOrder()).limit(limit).toList();
        List<BigDecimal> sortedDnList = dnList.stream().sorted(Comparator.reverseOrder()).limit(limit).toList();

        BigDecimal bigUpAmt = sortedUpList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bigDnAmt = sortedDnList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal n = bigUpAmt.subtract(bigDnAmt);

        if (n.compareTo(BDUtil.B1000W) < 0) {
            return;
        }

        List<EmDailyK> ks = emClient.getDailyKs(symbol.substring(2), LocalDate.now(), 500, false);
        for (int i = 0; i < ks.size(); i++) {
            EmDailyK k = ks.get(i);

            if (k.getTradeDate().equals(day)) {
                if (k.getPct().compareTo(BDUtil.B5) > 0 || k.getPct().compareTo(BigDecimal.ZERO) <=0 ) {
                    break;
                }
                for (int j = 1; j < 5; j++) {
                    int index = i + j;
                    if (index >= ks.size()) {
                        break;
                    }
                    EmDailyK k1 = ks.get(index);
                    BigDecimal d = k1.getClose().subtract(k.getClose());
                    BigDecimal p = d.divide(k.getClose(), 4, RoundingMode.HALF_UP);
                    log.info("symbol={} name={} day={} tradeDate={} close={} close1={}p={}", symbol, name, j, k1.getTradeDate(), k.getClose(), k1.getClose(), BDUtil.p100(p));
                }

            }
        }

        log.info("symbol={} name={} upAmt={}, dnAmt={} 净值={}, 大额UpAmt={}, 大额DnAmt={} 大额净值={}",
                symbol, name, BDUtil.amtHuman(upAmt), BDUtil.amtHuman(dnAmt), BDUtil.amtHuman(upAmt.subtract(dnAmt))
                , BDUtil.amtHuman(bigUpAmt), BDUtil.amtHuman(bigDnAmt), BDUtil.amtHuman(n));
    }


}

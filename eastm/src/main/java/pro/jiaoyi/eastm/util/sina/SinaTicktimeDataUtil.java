package pro.jiaoyi.eastm.util.sina;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.dao.entity.FenshiAmtSummaryEntity;
import pro.jiaoyi.eastm.dao.repo.FenshiAmtSummaryRepo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SinaTicktimeDataUtil {

    @Resource
    private OkHttpUtil okHttpUtil;

    @Resource
    private FenshiAmtSummaryRepo fenshiAmtSummaryRepo;

    public static final Map<String, String> header = new HashMap<>();

    static {
        header.put("authority", "vip.stock.finance.sina.com.cn");
        header.put("accept", "*/*");
        header.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
        header.put("cache-control", "no-cache");
        header.put("pragma", "no-cache");
//        header.put("referer", "https://vip.stock.finance.sina.com.cn/quotes_service/view/vMS_tradedetail.php?symbol=" + symbol);
        header.put("sec-ch-ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"");
        header.put("sec-ch-ua-mobile", "?0");
        header.put("sec-ch-ua-platform", "\"macOS\"");
        header.put("sec-fetch-dest", "empty");
        header.put("sec-fetch-mode", "cors");
        header.put("sec-fetch-site", "same-origin");
        header.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        header.put("Cookie", "SCF=AuFmhHoaSyiCCayz096GAwetjv40vHjF8PJIFeFkEd6pNT_H6jlRSUzPYBkht6cvX6IkOcQRkc7aQs2_4DUn-bc.; U_TRS1=000000da.f4572845c.6585492b.db11a27e; SUB=_2A25I4VjxDeRhGeFG41ET9ivMwz-IHXVrn9Q5rDV_PUNbm9AGLW_NkW1NeKLTyJmSUmeHBE1Pom81Ga1HNaVAxcIZ; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WhluuG898i7yhOWenU4RHoJ5NHD95QN1hn0eoqfehn0Ws4DqcjUi--Xi-iWi-2Xi--ciK.RiK.0i--fiK.piK.Rx2Bt; ALF=1712108961; FIN_ALL_VISITED=sz000001; UOR=www.baidu.com,finance.sina.com.cn,; FINA_V_S_2=sz000001; SINAGLOBAL=112.87.186.127_1710053722.429644; Apache=112.87.186.127_1710053722.429646; SFA_version7.12.0=2024-03-10%2014%3A51; SR_SEL=1_511; SFA_version7.12.0_click=1; close_rightAppMsg=1; ULV=1710053621628:2:2:2:112.87.186.127_1710053722.429646:1710053544204; U_TRS2=0000007f.bbc36d08f.65ed5963.8b269040");
    }

    public void getTicktimeData(String symbol, String day, boolean sumFlag) {
        if (symbol == null || day == null) {
            log.error("symbol or day is null");
            return;
        }
        if (symbol.length() != 6) {
            log.error("symbol length is not 6");
            return;
        }
        if (symbol.startsWith("6")) {
            symbol = "sh" + symbol;
        }
        if (symbol.startsWith("0") || symbol.startsWith("3")) {
            symbol = "sz" + symbol;
        }
        if (symbol.startsWith("8") || symbol.startsWith("4")) {
            symbol = "bj" + symbol;
        }

        JSONArray array = null;
        String s = null;

        String fileName = "ticktime/" + day + "/" + symbol + ".json";
        boolean fileFlag = FileUtil.fileCheck(fileName);
        if (fileFlag) {
            s = FileUtil.readFromFile(fileName);
        } else {
            String url = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_Bill.GetBillList" +
                    "?symbol=" + symbol +
                    "&num=6000" +
                    "&page=1" +
                    "&sort=ticktime" +
                    "&asc=1" +
                    "&amount=0" +
                    "&type=0" +
                    "&day=" + day;

            header.put("referer", "https://vip.stock.finance.sina.com.cn/quotes_service/view/vMS_tradedetail.php?symbol=" + symbol);

            byte[] bytes = okHttpUtil.getForBytes(url, header);
            if (bytes == null) {
                log.error("get bytes is null");
                return;
            }
            s = new String(bytes, StandardCharsets.UTF_8);

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                log.error("{}", e);
            }
        }

        try {
            array = JSONArray.parseArray(s);
        } catch (Exception e) {
            log.error("{}", e);
        }

        if (array == null || array.isEmpty()) {
            log.error("array is null or size is s={}", s);
            return;
        }

        if (sumFlag) {
            timeGo(array, day);
        }


        if (!fileFlag) {
            log.info("symbol={}, day={}, size={}, write to file {}", symbol, day, array.size(), fileName);
            FileUtil.writeToFile(fileName, array.toJSONString());
        }

    }

    public void getTicktimeData(String symbol, String day) {
        getTicktimeData(symbol, day, false);
    }

    /*
    [
    {
        "volume": "207700",
        "prev_price": "0.000",
        "symbol": "sh600582",
        "price": "6.850",
        "kind": "U",
        "name": "天地科技",
        "ticktime": "09:25:01"
    }
    ]
     */
    //day=20220104
    public void timeGo(JSONArray array, String day) {
        if (array == null || array.isEmpty()) {
            log.error("array is null or empty");
            return;
        }
        //按时间走
        day = day.replace("-", "");
        //yyyy MM dd
        int year = Integer.parseInt(day.substring(0, 4));
        int month = Integer.parseInt(day.substring(4, 6));
        int d = Integer.parseInt(day.substring(6, 8));
        LocalDate date = LocalDate.of(year, month, d);

        ArrayList<TickDto> list = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            TickDto dto = new TickDto();
            dto.setSymbol(item.getString("symbol"));
            dto.setName(item.getString("name"));
            dto.setPrice(new BigDecimal(item.getString("price")));
            dto.setVolume(new BigDecimal(item.getString("volume")));
            dto.setPrev_price(new BigDecimal(item.getString("prev_price")));
            dto.setKind(item.getString("kind"));
            dto.setTicktime(item.getString("ticktime"));

            // 定义时间格式
            // 解析 ticktime 字符串为 LocalTime 对象
            LocalTime localTime = LocalTime.parse(dto.getTicktime(), DateTimeFormatter.ofPattern("HH:mm:ss"));
            dto.setTs(localTime);
            dto.setDay(date);

            list.add(dto);
        }

        LocalTime open = LocalTime.of(9, 25);
        LocalTime end = LocalTime.of(15, 0);

        LocalTime windowStart = LocalTime.of(9, 25);
        int gapSecond = 5;
        int windowSecond = 65;

        // > 100w 的数据
        ArrayList<BigDecimal> amtDay = new ArrayList<>();
        while (!windowStart.isAfter(end)) {
            LocalTime m2 = windowStart.plusSeconds(gapSecond);
            //取前65秒的数据
            LocalTime m1 = m2.minusSeconds(windowSecond);
            if (m1.isBefore(open)) {
                m1 = open;
            }

            //取m1-m2 之间的数据
            BigDecimal windowAmt = BigDecimal.ZERO;
            BigDecimal aU = BigDecimal.ZERO;
            BigDecimal aD = BigDecimal.ZERO;
            BigDecimal aZ = BigDecimal.ZERO;

            BigDecimal p1p0 = BigDecimal.ZERO;
            BigDecimal p1p0avg = BigDecimal.ZERO;

            ArrayList<TickDto> arr = new ArrayList<>();
            for (TickDto dto : list) {
                if (!dto.getTs().isBefore(m1) && dto.getTs().isBefore(m2)) {
                    arr.add(dto);

                    BigDecimal a = dto.getPrice().multiply(dto.getVolume());
                    windowAmt = windowAmt.add(a);

                    if ("U".equals(dto.getKind())) {
                        aU = aU.add(a);
                    } else if ("D".equals(dto.getKind())) {
                        aD = aD.add(a);
                    } else {
                        aZ = aZ.add(a);
                    }

                }
            }
            if (arr.size() > 1) {
                BigDecimal p0 = arr.get(0).getPrice();
                BigDecimal p1 = arr.get(arr.size() - 1).getPrice();
                if (p0.compareTo(BigDecimal.ZERO) > 0) {
                    p1p0 = p1.subtract(p0).divide(p0, 4, RoundingMode.HALF_UP);
                    if (p1p0.compareTo(BigDecimal.ZERO) != 0) {
                        p1p0avg = (aU.subtract(aD)).divide(p1p0, 4, RoundingMode.HALF_UP);
                    }
                }
            }

            if (windowAmt.compareTo(BDUtil.B100W) > 0) {
                amtDay.add(windowAmt);
            }


            log.debug("start={}, end={}, size={}, amt={} U={} D={} Z={} U-D={} p1p0={} p1p0avg={}",
                    m1, m2, amtDay.size(), BDUtil.amtHuman(windowAmt), BDUtil.amtHuman(aU), BDUtil.amtHuman(aD), BDUtil.amtHuman(aZ), BDUtil.amtHuman(aU.subtract(aD)), BDUtil.p100(p1p0), BDUtil.amtHuman(p1p0avg));
            windowStart = m2;
        }

        // 保存到汇总表
        FenshiAmtSummaryEntity summary = new FenshiAmtSummaryEntity();

        String symbol = list.get(0).getSymbol();
        summary.setF12code(symbol.substring(2));
        summary.setCount((long) amtDay.size());
        summary.setTradeDate(day);

        try {
            log.info("save summary {}", summary);
            FenshiAmtSummaryEntity db = fenshiAmtSummaryRepo.findByF12codeAndTradeDate(summary.getF12code(), summary.getTradeDate());
            //查询 date 是否重复
            if (db != null) {//update
                log.info("update summary, db={}", db);
                summary.setId(db.getId());
            }
            fenshiAmtSummaryRepo.save(summary);
            log.info("save summary {} finish", summary.getF12code());
        } catch (Exception e) {
            // 处理异常，例如记录日志或抛出自定义异常
            log.error("Failed to save summary for f12code: {}", symbol, e);
        }
    }

}

@Data
class TickDto {
    String symbol;
    String name;
    BigDecimal price;
    BigDecimal volume;
    BigDecimal prev_price;
    String kind;
    String ticktime;
    LocalTime ts;
    LocalDate day;
}

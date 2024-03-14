package pro.jiaoyi.eastm.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.dao.entity.FenshiAmtSummaryEntity;
import pro.jiaoyi.eastm.dao.repo.FenshiAmtSummaryRepo;
import pro.jiaoyi.eastm.flow.common.CommonInfo;
import pro.jiaoyi.eastm.job.JobAlert;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/stock")
@Slf4j
public class StockController {

    @Resource
    private EmClient emClient;

    @Resource
    private FenshiAmtSummaryRepo fenshiAmtSummaryRepo;
    @Resource
    private WxUtil wxUtil;

    public static final Map<String, BigDecimal> MONITOR_CODE_AMT_MAP = new HashMap<>();
    public static final Cache<String, String> MONITOR_CODE_TICK_MAP = Caffeine.newBuilder()
            .maximumSize(10000).expireAfterWrite(1, TimeUnit.DAYS).build();
    public static final Map<String, String> CODE_REMARK_MAP = new HashMap<>();


    @GetMapping("/vol/hour")
    public JSONObject volHour(String code) {
        JSONObject jsonObject = new JSONObject();
        if (!codeCheck(code)) {
            jsonObject.put("vol", BDUtil.B1000Y);
            return jsonObject;
        }

        BigDecimal amtHour = emClient.getAmtHour(code);
        jsonObject.put("vol", amtHour);
        if (!MONITOR_CODE_AMT_MAP.containsKey(code)) {
            MONITOR_CODE_AMT_MAP.put(code, amtHour);
            extractedFsCount(code);
        }

        jsonObject.put("tick", MONITOR_CODE_TICK_MAP.getIfPresent(code));
        log.info("jsonObject {}", jsonObject.toJSONString());
        return jsonObject;
    }


//    public BigDecimal getAmtHour(String code) {
//        List<EmDailyK> dailyKs =
//                emClient.getDailyKs(code, LocalDate.now(), 100, false);
//        BigDecimal amtDay = emClient.amtTop10p(dailyKs);
//        return amtDay.divide(new BigDecimal(4), 2, RoundingMode.HALF_UP);
//    }

    @GetMapping("/monitor")
    public Object monitor(String codes) {
        if (codes != null) {
            String[] codeArr = codes.split(",");
            for (String code : codeArr) {
                if (codeCheck(code)) {
                    BigDecimal amtHour = emClient.getAmtHour(code);
                    MONITOR_CODE_AMT_MAP.put(code, amtHour);
                    String name = emClient.getCodeNameMap(false).get(code);
                    wxUtil.send("监控" + name + code);
                }
            }
        }
        return MONITOR_CODE_AMT_MAP;
    }

    @PostMapping("/remark")
    @CrossOrigin(origins = "*", methods = {RequestMethod.POST}, allowedHeaders = "*")
    public Object remark(@RequestBody String json) {
        log.info("remark json:{}", json);
        if (StringUtils.hasText(json)) {
            JSONArray array = JSONArray.parseArray(json);
            for (int i = 0; i < array.size(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                String code = jsonObject.getString("code");
                String remark = jsonObject.getString("remark");
                if (codeCheck(code)) {
                    CODE_REMARK_MAP.put(code, remark);
                }
            }
        }
        return CODE_REMARK_MAP;
    }

    @GetMapping("/remark/clear")
    public Object remarkClear() {
        CODE_REMARK_MAP.clear();
        return CODE_REMARK_MAP;
    }


    @GetMapping("/monitor/clear")
    public Object monitorClear() {
        MONITOR_CODE_AMT_MAP.clear();
        return MONITOR_CODE_AMT_MAP;
    }

    @GetMapping("/monitor/del")
    public Object monitorDel(String code) {
        if (codeCheck(code)) {
            MONITOR_CODE_AMT_MAP.remove(code);
        }
        return MONITOR_CODE_AMT_MAP;
    }


    @GetMapping("/tip")
    public String tip(String tip) {
        JobAlert.TIP = tip;
        return JobAlert.TIP;
    }

    public boolean codeCheck(String code) {
        if (StringUtils.hasText(code) && code.length() == 6) {
            if (code.startsWith("6")
                    || code.startsWith("0") || code.startsWith("3")
                    || code.startsWith("8") || code.startsWith("4")) {
                return true;
            }
        }
        return false;
    }

    private void extractedFsCount(String code) {
        List<FenshiAmtSummaryEntity> fs = fenshiAmtSummaryRepo.findRecentDataByCode(code, 30);
        int size = 2880;
        StringBuilder sb = new StringBuilder();
        if (!fs.isEmpty()) {
            for (int i = 0; i < fs.size(); i++) {
                FenshiAmtSummaryEntity f = fs.get(i);
                BigDecimal pct = BigDecimal.valueOf(f.getCount()).divide(BigDecimal.valueOf(size), 2, RoundingMode.HALF_UP);
                sb.append(i).append("=").append(BDUtil.p100(pct, 0)).append(" ");
            }
        }
        MONITOR_CODE_TICK_MAP.put(code, sb.toString());
    }
}


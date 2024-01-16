package pro.jiaoyi.eastm.controller;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.config.WxUtil;
import pro.jiaoyi.eastm.flow.common.CommonInfo;
import pro.jiaoyi.eastm.job.JobAlert;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stock")
public class StockController {

    @Resource
    private EmClient emClient;

    @Resource
    private WxUtil wxUtil;

    public static final Map<String, BigDecimal> MONITOR_CODE_AMT_MAP = new HashMap<>();


    @GetMapping("/vol/hour")
    public JSONObject volHour(String code) {
        JSONObject jsonObject = new JSONObject();
        if (!codeCheck(code)) {
            jsonObject.put("vol", BDUtil.B1000Y);
            return jsonObject;
        }

        BigDecimal amtHour = getAmtHour(code);
        jsonObject.put("vol", amtHour);
        if (!MONITOR_CODE_AMT_MAP.containsKey(code)) {
            MONITOR_CODE_AMT_MAP.put(code, amtHour);
            String name = emClient.getCodeNameMap(false).get(code);
            wxUtil.send("监控" + name + code);
        }
        return jsonObject;
    }


    public BigDecimal getAmtHour(String code) {
        List<EmDailyK> dailyKs =
                emClient.getDailyKs(code, LocalDate.now(), 100, false);
        BigDecimal amtDay = emClient.amtTop10p(dailyKs);
        return amtDay.divide(new BigDecimal(4), 2, RoundingMode.HALF_UP);
    }

    @GetMapping("/monitor")
    public Object monitor(String code) {
        if (codeCheck(code)) {
            BigDecimal amtHour = getAmtHour(code);
            MONITOR_CODE_AMT_MAP.put(code, amtHour);
            String name = emClient.getCodeNameMap(false).get(code);
            wxUtil.send("监控" + name + code);
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
}


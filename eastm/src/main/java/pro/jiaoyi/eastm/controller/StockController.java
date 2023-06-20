package pro.jiaoyi.eastm.controller;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.job.JobAlert;
import pro.jiaoyi.eastm.model.EmDailyK;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/stock")
public class StockController {

    @Resource
    private EmClient emClient;

    @GetMapping("/vol/hour")
    public JSONObject volHour(String code) {
        List<EmDailyK> dailyKs =
                emClient.getDailyKs(code, LocalDate.now(), 100, false);
        BigDecimal amtDay = emClient.amtTop10p(dailyKs);

        JSONObject jsonObject = new JSONObject();
        BigDecimal amtHour = amtDay.divide(new BigDecimal(4), 2, RoundingMode.HALF_UP);
        jsonObject.put("vol", amtHour);
        return jsonObject;
    }


    @GetMapping("/tip")
    public String tip(String tip){
        JobAlert.TIP = tip;
        return JobAlert.TIP;
    }
}


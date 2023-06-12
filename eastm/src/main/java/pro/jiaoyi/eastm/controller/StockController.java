package pro.jiaoyi.eastm.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.jiaoyi.eastm.api.EmClient;
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
    public BigDecimal volHour(String code) {
        List<EmDailyK> dailyKs =
                emClient.getDailyKs(code, LocalDate.now(), 100, false);
        BigDecimal amtDay = emClient.amtTop10p(dailyKs);
        return amtDay.divide(new BigDecimal(4), 2, RoundingMode.HALF_UP);
    }
}


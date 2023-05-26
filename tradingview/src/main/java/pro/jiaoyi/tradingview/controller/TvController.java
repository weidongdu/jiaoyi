package pro.jiaoyi.tradingview.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pro.jiaoyi.tradingview.model.TvChart;
import pro.jiaoyi.tradingview.service.TvService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/tv")
public class TvController {

    @Autowired
    private TvService tvService;

    @GetMapping("/chart")
    public TvChart tvChart(@RequestParam String code) {
        TvChart tvChart = tvService.getTvChart(code, LocalDate.now(), 500);
        return tvChart;
    }

    @GetMapping("/stockList")
    public Map<String, List<String>> getLists() {
        Map<String, List<String>> allIndex = tvService.getAllIndex();
        return allIndex;
    }
}

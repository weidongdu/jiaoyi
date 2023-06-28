package pro.jiaoyi.tradingview.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.config.IndexEnum;
import pro.jiaoyi.tradingview.config.Colors;
import pro.jiaoyi.tradingview.model.TvChart;
import pro.jiaoyi.tradingview.model.chart.Constants;
import pro.jiaoyi.tradingview.model.chart.TvMarker;
import pro.jiaoyi.tradingview.model.chart.TvTimeValue;
import pro.jiaoyi.tradingview.service.TvService;
import pro.jiaoyi.tradingview.service.TvTransUtil;

import java.time.LocalDate;
import java.util.*;

@RestController
@Slf4j
@RequestMapping("/tv")
public class TvController {

    @Autowired
    private TvService tvService;

    @GetMapping("/chart")
    public TvChart tvChart(@RequestParam String code, String codeType) {
        log.info("tvChart code={} codeType={}", code, codeType);
        TvChart tvChart = tvService.getTvChart(code, codeType, LocalDate.now(), 500);
        return tvChart;
    }
    @GetMapping("/chart/random")
    public TvChart tvChartRandom() {
        return tvService.getTvChartRandom();
    }
    @GetMapping("/chart/random/after")
    public TvChart tvChartRandomAfter(String code, @RequestParam("time") String hitDateStr) {
        return tvService.getTvChartRandom(code, hitDateStr);
    }

    @GetMapping("/stockList")
    public Map<String, List<String>> getLists(Boolean sync) {
        if (sync == null) {
            sync = false;
        }
        return tvService.getAllIndex(sync);
    }

//
//    @GetMapping("/stockList/byType")
//    public List<String> getList(String type, boolean async) {
//
//        List<EmCList> lists = tvService.getIndex(type, true, async);
//        return lists.stream().map(EmCList::getF12Code).collect(Collectors.toList());
//
//    }

    @GetMapping("/stockType")
    public Map<String, String> getStockType() {
        HashMap<String, String> map = new HashMap<>();
        IndexEnum[] values = IndexEnum.values();
        for (IndexEnum indexEnum : values) {
            map.put(indexEnum.getType(), indexEnum.getName());
        }
        return map;
    }



    @GetMapping("/chart/T")
    public TvChart tvChartT(@RequestParam String code, String codeType) {
        log.info("tvChart code={} codeType={}", code, codeType);
        TvChart tvChart = tvService.getTvChart(code, LocalDate.now(), 5000, true);
        tvChart.setMks(new ArrayList<>());
        Map<String, List<TvTimeValue>> kMaLines = tvChart.getKMaLines();
        kMaLines.put("up",Collections.emptyList());
        kMaLines.put("dn",Collections.emptyList());
        List<String[]> TArray = new ArrayList<>();//LoadConfig.T_SHADOW_DATA.stream().filter(arr -> arr[0].equals(code)).toList();
        if (TArray.size() > 0) {
            List<TvMarker> mks = tvChart.getMks();
            //String[] head = {"code", "name", "date", "high", "max", "min"};
            for (String[] arr : TArray) {
                TvMarker mk = new TvMarker();
                mk.setColor(Colors.YELLOW.getColor());
                mk.setPosition(Constants.MARKER_POSITION_ABOVEBAR);
                mk.setShape(Constants.MARKER_SHAPE_ARROW_DOWN);
                mk.setText("T");
                mk.setTime(DateUtil.strToLocalDate(arr[2],DateUtil.PATTERN_yyyyMMdd).toString());
                mks.add(mk);
            }
            TvTransUtil.sortMks(mks);
        }
        return tvChart;
    }

}

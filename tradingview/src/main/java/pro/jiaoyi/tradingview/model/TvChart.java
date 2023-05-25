package pro.jiaoyi.tradingview.model;

import lombok.Data;
import pro.jiaoyi.tradingview.model.chart.TvK;
import pro.jiaoyi.tradingview.model.chart.TvTimeValue;
import pro.jiaoyi.tradingview.model.chart.TvVol;

import java.util.List;
import java.util.Map;

@Data
public class TvChart {
    private String code;
    private String name;
    private List<String> ccList;//概念

    private List<TvK> k;//K线
    private List<TvTimeValue> p;//涨跌幅

    //ma5 ma10 ma20 ma30 ma60 ma120 ma250
    private Map<String, List<TvTimeValue>> kMaLines;
    //成交量相关
    private List<TvVol> v;//成交量
    private Map<String, List<TvTimeValue>> vMaLines;//成交量均线
    private List<TvVol> hsl;//换手率
    private Map<String, List<TvTimeValue>> hslMaLines;//成交量均线
    private List<TvTimeValue> osc;//振幅
    private Map<String, List<TvTimeValue>> oscMaLines;//成交量均线
}

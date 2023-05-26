window.addEventListener('resize', () => {
    resize(chart);
});

function resize(chart) {
    let w = window.innerWidth;
    chart.resize(w, w * 6 / 15, true);
}


function initChart(chart) {

    setOptionExt(chart);

    //设置k线主图数据
    const kSeries = chart.addCandlestickSeries();
    kSeries.setData([]);
    kSeries.applyOptions(getKColorOption());


    //设置均线数据
    const ma5Series = initMa(chart, WHITE_MA5);
    const ma10Series = initMa(chart, YELLOW_MA10);
    const ma20Series = initMa(chart, PURPLE_MA20);
    const ma30Series = initMa(chart, GREEN_MA30);
    const ma60Series = initMa(chart, GREY_MA60);
    const ma120Series = initMa(chart, BLUE_MA120);
    const ma250Series = initMa(chart, LIGHT_BLUE_MA250);

    //设置成交量数据
    const volumeSeries = chart.addHistogramSeries(getVolOption());
    volumeSeries.setData([]);
    setVolumeSeriesOption(volumeSeries)

    const ma5VolumeSeries = initMa(chart, WHITE_MA5, getVolOption());
    const ma60VolumeSeries = initMa(chart, GREY_MA60, getVolOption());

    //设置涨跌幅
    const pctSeries = chart.addHistogramSeries();
    pctSeries.setData([]);
    setPctSeriesOption(pctSeries);

    //设置hsl
    const hslSeries = chart.addHistogramSeries();
    hslSeries.setData([]);
    setHslSeriesOption(hslSeries)

    //设置hsl ma
    const ma5HslSeries = initMa(chart);
    const ma60HslSeries = initMa(chart);

    //设置振幅
    const oscSeries = chart.addHistogramSeries();
    oscSeries.setData([]);
    //设置振幅 ma
    const ma5OscSeries = initMa(chart);
    const ma60OscSeries = initMa(chart);

    //返回所有series
    return {
        kSeries,
        ma5Series,
        ma10Series,
        ma20Series,
        ma30Series,
        ma60Series,
        ma120Series,
        ma250Series,
        volumeSeries,
        ma5VolumeSeries,
        ma60VolumeSeries,
        pctSeries,
        hslSeries,
        ma5HslSeries,
        ma60HslSeries,
        oscSeries,
        ma5OscSeries,
        ma60OscSeries,
    }

}

function initMa(chart, color, option) {

    let o = {
        color: color,
        lineWidth: 1,
        lastValueVisible: false,//标签
        priceLineVisible: false,//价格线
    };
    if (option) {
        Object.assign(o, o, option);
    }
    let ma = chart.addLineSeries(o);
    ma.setData([]);
    return ma;
}


function updateChartData(data, series) {
    const {
        kSeries,
        ma5Series,
        ma10Series,
        ma20Series,
        ma30Series,
        ma60Series,
        ma120Series,
        ma250Series,
        volumeSeries,
        ma5VolumeSeries,
        ma60VolumeSeries,
        pctSeries,
        hslSeries,
        ma5HslSeries,
        ma60HslSeries,
        oscSeries,
        ma5OscSeries,
        ma60OscSeries,
    } = series;

    //设置k线主图数据
    kSeries.setData(data.k);
    volumeSeries.setData(data.v);
    pctSeries.setData(data.p);
    ma5Series.setData(data.kmaLines.ma5);
    ma10Series.setData(data.kmaLines.ma10);
    ma20Series.setData(data.kmaLines.ma20);
    ma30Series.setData(data.kmaLines.ma30);
    ma60Series.setData(data.kmaLines.ma60);
    ma120Series.setData(data.kmaLines.ma120);
    ma250Series.setData(data.kmaLines.ma250);
    ma5VolumeSeries.setData(data.vmaLines.ma5);
    ma60VolumeSeries.setData(data.vmaLines.ma60);
    hslSeries.setData(data.hsl);
}
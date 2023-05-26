
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
    const ma5Series = initMa(chart);
    const ma10Series = initMa(chart);
    const ma20Series = initMa(chart);
    const ma30Series = initMa(chart);
    const ma60Series = initMa(chart);
    const ma120Series = initMa(chart);
    const ma250Series = initMa(chart);

    //设置成交量数据
    const volumeSeries = chart.addHistogramSeries(getVolOption());
    volumeSeries.setData([]);
    setVolumeSeriesOption(volumeSeries)

    const ma5VolumeSeries = initMa(chart);
    const ma60VolumeSeries = initMa(chart);

    //设置涨跌幅
    const pctSeries = chart.addHistogramSeries();
    pctSeries.setData([]);
    setPctSeriesOption(pctSeries);

    //设置hsl
    const hslSeries = chart.addHistogramSeries();
    hslSeries.setData([]);
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

function initMa(chart) {
    let ma = chart.addLineSeries();
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

}
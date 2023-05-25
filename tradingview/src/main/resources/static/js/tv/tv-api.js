const ZISE = '#71649C';//("#71649C","紫色"),
const ZISE_L = '#9B7DFF';//亮紫色
const GREY_SHADOW = '#C3BCDB44';//灰色阴影
const RED = '#DE5E57';//("#DE5E57","红色"),
const GREEN = '#52A49A';//("52A49A","绿色");

window.addEventListener('resize', () => {
    resize(chart);
});

function resize(chart) {
    let w = window.innerWidth;
    chart.resize(w, w * 6 / 15, true);
}

function getOption() {
    return {
        height: window.innerWidth * 6 / 15,
        width: window.innerWidth,

        layout: {
            background: {color: '#222'},
            textColor: '#DDD',
        },
        grid: {
            vertLines: {color: '#444'},
            horzLines: {color: '#444'},
        },
    }
}


function getKColorOption() {
    return {
        wickUpColor: RED,
        upColor: RED,
        wickDownColor: GREEN,
        downColor: GREEN,
        borderVisible: false,
    }

}

function getVolOption(){
    return {
        priceFormat: {
            type: 'volume',
        },
        priceScaleId: '', // set as an overlay by setting a blank priceScaleId
    }
}

function setOptionExt(chart) {

    chart.priceScale().applyOptions({
        borderColor: ZISE,
    });

    // Setting the border color for the horizontal axis
    chart.timeScale().applyOptions({
        borderColor: ZISE,
    });

    chart.priceScale('right').applyOptions({
        mode: 1,//• Logarithmic = 1
        scaleMargins: {
            top: 0.1,
            bottom: 0.25,
        },
    });

    // Customizing the Crosshair
    chart.applyOptions({
        crosshair: {
            // Change mode from default 'magnet' to 'normal'.
            // Allows the crosshair to move freely without snapping to datapoints
            mode: LightweightCharts.CrosshairMode.Normal,

            // Vertical crosshair line (showing Date in Label)
            vertLine: {
                width: 1,
                color: GREY_SHADOW,
                style: LightweightCharts.LineStyle.Solid,
                labelBackgroundColor: ZISE_L,
            },

            // Horizontal crosshair line (showing Price in Label)
            horzLine: {
                color: ZISE_L,
                labelBackgroundColor: ZISE_L,
            },
        },
    });

}

function initChart(chart) {

    setOptionExt(chart);

    //设置k线主图数据
    const kSeries = chart.addCandlestickSeries();
    kSeries.applyOptions(getKColorOption());
    kSeries.setData([]);


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
    volumeSeries.priceScale('right').applyOptions({
        scaleMargins: {
            top: 0.8, // highest point of the series will be 70% away from the top
            bottom: 0, // lowest point will be at the very bottom.
        },
    });



    const ma5VolumeSeries = initMa(chart);
    const ma60VolumeSeries = initMa(chart);

    //设置涨跌幅
    const changeSeries = chart.addHistogramSeries();
    changeSeries.setData([]);

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
        changeSeries,
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
        changeSeries,
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


}
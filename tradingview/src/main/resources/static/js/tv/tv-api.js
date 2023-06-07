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
    kSeries.setMarkers([]);
    kSeries.applyOptions(getKColorOption());

    //设置均线数据
    const ma5Series = initMa(chart, 1, WHITE_MA5);
    const ma10Series = initMa(chart, 1, YELLOW_MA10);
    const ma20Series = initMa(chart, 1, PURPLE_MA20);
    const ma30Series = initMa(chart, 1, GREEN_MA30);
    const ma60Series = initMa(chart, 1, GREY_MA60);
    const ma120Series = initMa(chart, 1, BLUE_MA120);
    const ma250Series = initMa(chart, 1, LIGHT_BLUE_MA250);
    //设置上下限
    const upSeries = initMa(chart, 1, YELLOW_UP, {lineStyle: 4});
    const dnSeries = initMa(chart, 1, BLUE_DN, {lineStyle: 4});

    //设置成交量数据
    const volumeSeries = chart.addHistogramSeries(getVolOption());
    volumeSeries.setData([]);
    setVolumeSeriesOption(volumeSeries)
    // const ma5VolumeSeries = initMa(chart, 1, WHITE_MA5, getVolOption());
    const ma60VolumeSeries = initMa(chart, 2, GREY_MA60, getVolOption());
    //设置涨跌幅
    const pctSeries = chart.addHistogramSeries();
    pctSeries.setData([]);
    setPctSeriesOption(pctSeries);
    //设置hsl
    const hslSeries = chart.addHistogramSeries();
    hslSeries.setData([]);
    // setHslSeriesOption(hslSeries)
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
        upSeries,
        dnSeries,
        volumeSeries,
        // ma5VolumeSeries,
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

function initMa(chart, lineWidth, color, option) {
    let o = {
        color: color,
        lineWidth: lineWidth,
        lastValueVisible: false,//标签
        priceLineVisible: false,//价格线
        crosshairMarkerVisible: false,//鼠标交叉点
    };
    if (option) {
        Object.assign(o, o, option);
    }
    let ma = chart.addLineSeries(o);
    ma.setData([]);
    return ma;
}


function updateChartData(data, series) {

    $("#iCode").val(data.code);
    let bk = data.bk;
    // if (data.ccList && data.ccList.length > 0) {
    //     for (let i = 0; i < data.ccList.length; i++) {
    //         if (i > 0) bk += " ";
    //         bk += data.ccList[i]
    //     }
    // }
    let name = data.name +" "+ bk;
    $("#iName").val(name);

    let row = $("#iLegendRow");
    if (row) {
        row.empty();
        console.log('row', row.append($(`<bold>${data.code}-${name}</bold>`)))
    }

    const {
        kSeries,
        ma5Series,
        ma10Series,
        ma20Series,
        ma30Series,
        ma60Series,
        ma120Series,
        ma250Series,
        upSeries,
        dnSeries,
        volumeSeries,
        // ma5VolumeSeries,
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
    kSeries.setData([]);
    kSeries.setData(data.k);
    if (data.mks && data.mks.length > 0) {
        kSeries.setMarkers([]);
        kSeries.setMarkers(data.mks);
    }
    volumeSeries.setData([])
    volumeSeries.setData(data.v);

    pctSeries.setData([])
    pctSeries.setData(data.p);

    ma5Series.setData([])
    ma5Series.setData(data.kmaLines.ma5);

    ma10Series.setData([])
    ma10Series.setData(data.kmaLines.ma10);

    ma20Series.setData([])
    ma20Series.setData(data.kmaLines.ma20);

    ma30Series.setData([])
    ma30Series.setData(data.kmaLines.ma30);

    ma60Series.setData([])
    ma60Series.setData(data.kmaLines.ma60);

    ma120Series.setData([])
    ma120Series.setData(data.kmaLines.ma120);

    ma250Series.setData([])
    ma250Series.setData(data.kmaLines.ma250);

    upSeries.setData([])
    upSeries.setData(data.kmaLines.up);

    dnSeries.setData([])
    dnSeries.setData(data.kmaLines.dn);

    // ma5VolumeSeries.setData(data.vmaLines.ma5);
    ma60VolumeSeries.setData([]);
    ma60VolumeSeries.setData(data.vmaLines.ma60);
    // hslSeries.setData(data.hsl);
}

function legend() {

    let container = document.getElementById('charts');
    const legend = document.createElement('div');
    legend.setAttribute("class", "legend");
    container.appendChild(legend);

    const firstRow = document.createElement('div');
    firstRow.setAttribute("id", "iLegendRow");
    firstRow.innerHTML = $("#iCode").val() + $("#iName").val();
    firstRow.style.color = 'white';
    legend.appendChild(firstRow);

    chart.subscribeCrosshairMove(param => {
        // console.log('param', param)
        if (param.time) {

            let code = $("#iCode").val();
            let name = $("#iName").val();

            const data = param.seriesData.get(kSeries);
            let time = data.time;
            let open = toFix(data.open, 2);
            let close = toFix(data.close, 2);
            let high = toFix(data.high, 2);
            let low = toFix(data.low, 2);

            const vol = (param.seriesData.get(volumeSeries));
            const p = (param.seriesData.get(pctSeries));
            const ma5 = (param.seriesData.get(ma5Series));
            const ma10 = (param.seriesData.get(ma10Series));
            const ma20 = (param.seriesData.get(ma20Series));
            const ma30 = (param.seriesData.get(ma30Series));
            const ma60 = (param.seriesData.get(ma60Series));
            const ma120 = (param.seriesData.get(ma120Series));
            const ma250 = (param.seriesData.get(ma250Series));

            // console.log(p, ma5, ma10, ma20, ma30, ma60, ma120, ma250);
            let vvol = undefined === vol ? '' : toFix(vol.value, 2);
            if (vvol > Y) {
                vvol = toFix(vvol / Y, 2) + '亿';
            } else {
                vvol = toFix(vvol / W) + '万';
            }

            let vp = undefined === p ? '' : toFix(p.value, 2);
            if (vp > 0) vp = "+" + vp;

            let vma5 = undefined === ma5 ? '' : toFix(ma5.value, 2);
            let vma10 = undefined === ma10 ? '' : toFix(ma10.value, 2);
            let vma20 = undefined === ma20 ? '' : toFix(ma20.value, 2);
            let vma30 = undefined === ma30 ? '' : toFix(ma30.value, 2);
            let vma60 = undefined === ma60 ? '' : toFix(ma60.value, 2);
            let vma120 = undefined === ma120 ? '' : toFix(ma120.value, 2);
            let vma250 = undefined === ma250 ? '' : toFix(ma250.value, 2);


            firstRow.innerHTML = `<bold>日期:${time} 开:${open} 收:${close} 高:${high} 低:${low} 涨幅:${vp}% 成交额:${vvol} ${code}-${name}</bold>`;
            firstRow.innerHTML += `<br><bold>ma5:${vma5} ma10:${vma10} ma20:${vma20} ma30:${vma30} ma60:${vma60} ma120:${vma120} ma250:${vma250}</bold>`;
            // console.log(firstRow)
        }
    });
}

function toFix(num, d) {
    if (num) {
        if (typeof num !== 'number') {
            num = parseFloat(num);
        }
        return num.toFixed(d);
    }
    return num;
}
const ZISE = '#71649C';//("#71649C","紫色"),
const ZISE_L = '#9B7DFF';//亮紫色
const GREY_SHADOW = '#C3BCDB44';//灰色阴影
const RED = '#EA463C';//("#DE5E57","红色"),
const GREEN = '#76D770';//("52A49A","绿色");
const Color_BG = "#1C1F26"
const Color_GRID = "rgba(68,68,68,0.8)"

//白色 透明度0.7
const WHITE_MA5 = '#FFFFFF';
// 黄色
const YELLOW_MA10 = '#F9D56E';
// 紫色
const PURPLE_MA20 = '#71649C';
// 绿色
const GREEN_MA30 = '#1af62e';
// 灰色
const GREY_MA60 = '#C3BCDB';
// 蓝色
const BLUE_MA120 = '#4E86E4';
// 浅蓝色
const LIGHT_BLUE_MA250 = '#6EC1EA';


function getChartOption() {
    return {
        height: window.innerWidth * 6 / 15, width: window.innerWidth,

        layout: {
            background: {color: Color_BG}, textColor: WHITE_MA5,
        }, grid: {
            vertLines: {color: Color_GRID}, horzLines: {color: Color_GRID},
        },

        rightPriceScale: {
            visible: true,
        }, leftPriceScale: {
            visible: true,
        },


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
        mode: 1,//• Logarithmic = 1 对数坐标
        scaleMargins: {
            top: 0.1, bottom: 0.3,
        },
    });

    // Customizing the Crosshair
    chart.applyOptions({
        crosshair: {
            mode: LightweightCharts.CrosshairMode.Normal,//可以自由移动

            // Vertical crosshair line (showing Date in Label)
            vertLine: {
                width: 1, color: GREY_SHADOW, style: LightweightCharts.LineStyle.Solid, labelBackgroundColor: ZISE_L,
            },

            // Horizontal crosshair line (showing Price in Label)
            horzLine: {
                color: ZISE_L, labelBackgroundColor: ZISE_L,
            },
        },
    });

}


function getKColorOption() {
    return {
        wickUpColor: RED, upColor: RED, wickDownColor: GREEN, downColor: GREEN, borderVisible: false,
    }
}

function getVolOption() {
    return {
        //color 通过 后台数据给出
        priceFormat: {
            type: 'volume',
        }, priceScaleId: '', // set as an overlay by setting a blank priceScaleId
    }
}


function setVolumeSeriesOption(series) {
    series.priceScale('right').applyOptions({
        scaleMargins: {
            top: 0.8, // highest point of the series will be 80% away from the top
            bottom: 0, // lowest point will be at the very bottom.
        },
    });

}


function setPctSeriesOption(pctSeries) {
    pctSeries.applyOptions({
        overlay: true, priceFormat: {
            type: 'percent',
        }, priceScaleId: 'left', color: GREY_SHADOW
    });

    pctSeries.priceScale('left').applyOptions({
        scaleMargins: {
            top: 0.1, // highest point of the series will be 80% away from the top
            bottom: 0.25, // lowest point will be at the very bottom.
        },
    });
}
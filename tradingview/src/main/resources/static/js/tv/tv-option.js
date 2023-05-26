const ZISE = '#71649C';//("#71649C","紫色"),
const ZISE_L = '#9B7DFF';//亮紫色
const GREY_SHADOW = '#C3BCDB44';//灰色阴影
const RED = '#DE5E57';//("#DE5E57","红色"),
const GREEN = '#52A49A';//("52A49A","绿色");


function getChartOption() {
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

        rightPriceScale: {
            visible: true,
        },
        leftPriceScale: {
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
        mode: 1,//• Logarithmic = 1
        scaleMargins: {
            top: 0.1,
            bottom: 0.3,
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
        // overlay: true,
        // priceFormat: {
        //     type: 'custom',
        //     formatter: (price) => {
        //         if (price > 10000 * 10000){
        //             return price / (10000*10000) + '亿';
        //         }else {
        //             return price / 10000 + '万';
        //         }
        //     }
        // },
        priceFormat: {
            type: 'volume',
        },
        priceScaleId: '', // set as an overlay by setting a blank priceScaleId
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


function setPctSeriesOption(pctSeries){
    pctSeries.applyOptions({
        overlay: true,
        priceFormat: {
            type: 'percent',
        },
        priceScaleId: 'left',
        color: GREY_SHADOW
    });

    pctSeries.priceScale('left').applyOptions({
        scaleMargins: {
            top: 0.1, // highest point of the series will be 80% away from the top
            bottom: 0.25, // lowest point will be at the very bottom.
        },
    });
}
let STOCK_LIST = {};
let SELECT_LIST = [];
let index = 0;

//初始化股票列表

function next() {
    if (index === SELECT_LIST.length - 1) {
        alert("最后一个")
        return;
    }
    index++;
    let code = STOCK_LIST[index];
    update(code);
}

function pre() {
    if (index === 0) {
        alert("第一个")
        return;
    }
    index--;
    let code = STOCK_LIST[index];
    update(code);
}

//获取 股票列表
function selectType() {
    //设置 symbols
    let options = $("#iSelectType option:selected");
    let type = options.val();
    let SELECT_LIST = STOCK_LIST[type];
    if (SELECT_LIST && SELECT_LIST.length > 0) {
        alert(type + "=" + SELECT_LIST.length);
        index = 0;
        update(SELECT_LIST[0]);
    } else {
        alert("没有数据")
    }
}


function update(code) {

    getTvChart(code, (data) => {
        console.log(data.k);
        updateChartData(data, {
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
        })
    });
}



//https://www.toptal.com/developers/keycode/for/191
$(document).keydown(function (e) {

    if (75 === e.keyCode) {//k
        $('input[name=swK]').click();
    }


    if (77 === e.keyCode) {//m
        $('input[name=swMA]').click();
    }

    if (188 === e.keyCode) {//,
        $('input[name=swMAupdn]').click();
    }


    if (221 === e.keyCode) {//]
        $('#iBtnChart').click();
    }


    if (219 === e.keyCode) {
        $('#iBtnChartPre').click();
    }

    //keyCode 79 = o
    if (79 === e.keyCode) {
        document.querySelector('#iBtnXueqiu').querySelector('a').click();
        name = $('#iName').val();
        code = $('#iCode').val();
        openTap(name, code);
    }

    if (81 === e.keyCode) {
        document.querySelector('#iBtnFenshi').querySelector('a').click();
    }

    if (76 === e.keyCode) {//l
        mock();
    }

    if (84 === e.keyCode) {//t
        filter();
    }

    if (191 === e.keyCode) {// code = / ?
        singleStockChart();
    }

});

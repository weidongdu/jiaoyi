//初始化股票列表
let k = {};
let p = {};
let v = {};
let lastDay = '';
let lastCode = '';
let lastBk = '';

function dayAdd(dateString, num) {

    // 将日期字符串转换为Date对象
    let date = new Date(dateString);
    // 将日期增加一天
    date.setDate(date.getDate() + num);
    // 定义日期格式
    let dateFormat = 'yyyy-MM-dd';
    // 将日期格式中的占位符替换为实际日期值
    let formattedDate = dateFormat
        .replace('yyyy', date.getFullYear())
        .replace('MM', ('0' + (date.getMonth() + 1)).slice(-2))
        .replace('dd', ('0' + date.getDate()).slice(-2))
    // 输出格式化后的日期字符串
    console.log(formattedDate);
    return formattedDate;
}

function next() {
    let SELECT_LIST_ = getList();
    if (index >= SELECT_LIST_.length - 1) {
        alert("最后一个")
        return;
    }
    index++;

    $("#iIndex").val((index + 1) + "/" + SELECT_LIST_.length);
    let code = SELECT_LIST_[index];
    console.log(index, code)
    update(code);
}

function pre() {
    let SELECT_LIST_ = getList();
    console.log('pre', SELECT_LIST_.length)
    if (index <= 0) {
        alert("第一个")
        return;
    }
    index--;
    $("#iIndex").val((index + 1) + "/" + SELECT_LIST_.length);
    let code = SELECT_LIST_[index];
    console.log(index, code)
    update(code);
}

//获取 股票列表
function selectType() {
    //设置 symbols
    let options = $("#iSelectType option:selected");
    let type = options.val();
    let SELECT_LIST = STOCK_LIST[type];

    console.log("type:", type, "STOCK_LIST", STOCK_LIST, "SELECT_LIST", SELECT_LIST)
    if (SELECT_LIST && SELECT_LIST.length > 0) {
        console.log(type + "=" + SELECT_LIST.length);
        index = 0;
        update(SELECT_LIST[0]);
    } else {
        alert("没有数据")
    }
}

function getList() {
    let options = $("#iSelectType option:selected");
    let type = options.val();
    return STOCK_LIST[type];
}

function update(code) {
    if (!code) {
        alert("code为空");
        return;
    }

    getTvChart(code, (data) => {
        // console.log(data.k);
        if (data && data.k && data.k.length > 0) {
            k = data.k[data.k.length - 1];
            v = data.v[data.v.length - 1];
            p = data.p[data.p.length - 1];
            lastDay = k.time;
            lastCode = data.code;
            //判断是否切换了板块bk
            if (lastBk !== data.bk) {
                alert(lastBk + " -> " + data.bk);
                lastBk = data.bk;
                //获取板块图
                getTvChart(data.bk + "&codeType=BkValue", (data) => {
                    index--;
                    updateChartData(data, {
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
                    })
                })

            } else {
                updateChartData(data, {
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
                })
            }
        }


    });
}


function openTap(name, code) {
    if (name + code === '') {
        return;
    }
    //判断是否重复
    let codes = $('.tagcode');
    if (codes && codes.length > 0) {
        for (let i = 0; i < codes.length; i++) {
            if (codes[i].innerText === code) {
                console.log("重复数据")
                return;
            }
        }
    }

    if (code) {
        //设置 xueqiu url
        let m = code.startsWith('6') ? 'SH' : 'SZ'
        $("#iLinkxq").attr("href", "https://xueqiu.com/S/" + m + code);
        document.querySelector('#iBtnXueqiu').querySelector('a').click();
    }

    let iTag = $("#iTag");
    /*
        <div class="control">
            <div class="tags has-addons">
                <span class="tag is-dark">chat</span>
                <span class="tag is-primary">on gitter</span>
            </div>
        </div>
     */

    let html = '<div class="control"><div class="tags has-addons"><span class="tag is-dark">' + name + '</span><span class="tag tagcode">' + code + '</span></div></div>';
    iTag.append(html);
    //对此html div 元素绑定 点击事件 chart(code, 500);
    iTag.find("div:last").click(function () {
        update(code);
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

    if (221 === e.keyCode || 13 === e.keyCode) {//]
        $('#iBtnChart').click();
    }

    if (219 === e.keyCode) {
        $('#iBtnChartPre').click();
    }

    //keyCode 79 = o
    if (79 === e.keyCode) {

        name = $('#iName').val();
        code = $('#iCode').val();
        // https://xueqiu.com/S/SH600283

        openTap(name, code);
    }

    if (81 === e.keyCode) {
        document.querySelector('#iBtnFenshi').querySelector('a').click();
    }

    if (76 === e.keyCode) {//l
        mock();
    }

    if (84 === e.keyCode) {//t
        selectType();
    }

    if (191 === e.keyCode) {// code = / ?
        singleStockChart();
    }

    //f keycode = 70
    if (70 === e.keyCode){
        console.log("f");
        //新开一个页面  专门用于这种耗时间的操作
        let options = $("#iSelectType option:selected");
        let type = options.val();
        getStockListByType(type);
    }

});

function mock() {
    console.log('k', k);
    console.log('v', v);
    console.log('p', p);
    console.log('time', lastDay);

    let factor = 1.2;
    if (lastCode.startsWith("60") || lastCode.startsWith("0")) {
        factor = 1.1;
    }
    //跟新最新k
    k.low = toFix(k.close, 2);
    k.open = toFix(k.close, 2);
    k.close = toFix(factor * k.open, 2);
    k.high = toFix(k.close, 2);
    lastDay = dayAdd(lastDay, 1);
    k.time = lastDay;
    kSeries.update(k);

    p.time = lastDay;
    p.value = toFix((factor - 1) * 100, 2);
    pctSeries.update(p);

    v.time = lastDay;
    v.value = toFix(v.value * 2, 2);
    v.color = '#EA463C';
    volumeSeries.update(v);

}
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

function update(code, bk) {
    if (!code) {
        alert("code为空");
        return;
    }

    getTvChart(code, renderChart);
}

//获取随机股票 (次日涨幅 > 7%)
function updateR() {
    getTvChartRandom(renderChart);
}

function updateRandomAfter() {
    getTvChartRandomAfter(renderChart);
}

function updateT() {
    getTvChartT(renderChart);
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
    if (82 === e.keyCode) {//t
        remark();
    }

    if (191 === e.keyCode) {// code = / ?
        singleStockChart();
    }

    //f keycode = 70
    if (70 === e.keyCode) {
        console.log("f");
        //新开一个页面  专门用于这种耗时间的操作
        let options = $("#iSelectType option:selected");
        let type = options.val();
        getStockListByType(type);
    }


    if (186 === e.keyCode) {//186 ;
        updateR();
    }
    if (222 === e.keyCode) {//222 '
        updateRandomAfter();
    }

    if (220 === e.keyCode) {//\ 220
        updateT();
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


function notify(content) {

    let c1 = "notification is-danger top-right"
    let div = document.createElement("div");
    div.setAttribute("class", c1);
    let p = document.createElement("p");
    p.innerText = content;
    div.appendChild(p);
    $("#main").append(div);
    // 使用jQuery删除元素
    let sid = setInterval(function () {
        $('.notification.top-right').remove();
        clearInterval(sid);
    }, 2000)

}


function singleStockChart() {
    let code = prompt("请输入code 如:[000001]", ""); //将输入的内容赋给变量 name ，

    //这里需要注意的是，prompt有两个参数，前面是提示的话，后面是当对话框出来后，在对话框里的默认值
    //如果返回的有内容
    if (code) {
        // chart(code, 500);
        update(code);
    }

}

// 根据数值获取颜色
function getColor(value) {
    // 根据您的需求定义每个数值对应的颜色
    // 这里只是一个示例，您可以根据实际情况进行修改
    if (!value || value < 1000) {
        return "green";
    } else if (value < 2000) {
        return "yellow";
    } else {
        return "red";
    }
}

// 处理背景的函数
function updateBackground(arr) {
    if (!arr) {
        console.log("arr is null");
        return;
    }
    // 获取 iInfo 元素的子元素列表
    // 获取 iInfo 元素
    let iInfo = document.getElementById("iInfo");

    // 获取 iInfo 元素中类名为 colorDiv 的子元素列表
    let colorDivsOld = iInfo.querySelectorAll(".colorDiv");
    if (colorDivsOld) {
        // 遍历 colorDivs 列表，将每个元素从 DOM 中移除
        for (let i = 0; i < colorDivsOld.length; i++) {
            let part = colorDivsOld[i];
            // 删除 container 元素的所有子元素
            while (part.firstChild) {
                part.removeChild(part.firstChild);
            }
            part.remove();
        }

        // 删除 iInfo 元素中的 container（如果存在）
        let container = iInfo.querySelector(".container");
        if (container) {
            container.remove();
        }
    }


    // 创建一个新的容器元素用于水平排列
    let container = document.createElement('div');
    container.style.display = "flex";
    container.style.flexWrap = "nowrap";

    // 计算每个部分的宽度和高度
    let width = 100 / arr.length;
    let height = '20px';

    // 创建长度为 n 的 colorDivs 数组
    for (let i = 0; i < arr.length; i++) {
        let div = document.createElement('div');
        div.className = 'colorDiv';

        // 创建显示 value 的文本节点
        let text = (i + 1) + '-' + (arr[i] / 2880 * 100).toFixed(0) + "%";
        let valueNode = document.createTextNode(text);

        // 设置文字颜色
        let textColor = getTextColor(getColor(arr[i]));
        div.style.color = textColor;

        div.appendChild(valueNode);

        div.style.width = width + "%";
        div.style.height = height;
        // 设置 div 元素的高度与文本内容一样高
        div.style.backgroundColor = getColor(arr[i]);
        // div.style.height = div.clientHeight + "px";
        container.appendChild(div);
    }
    iInfo.appendChild(container);
}

// 获取与背景色对比的文字颜色
function getTextColor(backgroundColor) {
    let rgb;
    if (backgroundColor === "red") {
        rgb = [255, 0, 0];
    } else if (backgroundColor === "yellow") {
        rgb = [255, 255, 0];
    } else if (backgroundColor === "green") {
        rgb = [0, 128, 0];
    } else {
        // 如果颜色名称不匹配，可以返回默认的文字颜色
        return "#000000"; // 返回黑色文字颜色
    }
    // 将背景色转换为 RGB 格式
    // let rgb = backgroundColor.match(/\d+/g);
    let r = parseInt(rgb[0]);
    let g = parseInt(rgb[1]);
    let b = parseInt(rgb[2]);

    // 计算亮度
    let brightness = (r * 299 + g * 587 + b * 114) / 1000;

    // 根据亮度选择文字颜色
    return brightness > 125 ? "#000000" : "#FFFFFF";
}

const renderChart = (data) => {
    // console.log(data.k);
    if (data && data.k && data.k.length > 0) {
        k = data.k[data.k.length - 1];
        v = data.v[data.v.length - 1];
        p = data.p[data.p.length - 1];
        lastDay = k.time;
        lastCode = data.code;
        //设置分时code count
        // let fsCount = data.fsCount;
        // 获取输入框的值数组
        // let values = data.fsCount;

        // 数据数组
        if (data.fsCount) {
            var str = data.fsCount
            console.log(str);
            var arr = str.split(',');
            console.log(arr);
            if (arr.length > 0) {
                updateBackground(arr);
            }
        }

        // if (fsCount) {
        //     $("#iRes").val(fsCount);
        //     if (fsCount < 1000) {
        //         // 条件满足时，添加背景警告效果
        //         document.getElementById("iInfo").style.backgroundColor = "red";
        //         document.getElementById("iInfo").style.border = "1px solid red";
        //     } else if (fsCount < 2000) {
        //         document.getElementById("iInfo").style.backgroundColor = "orange";
        //         document.getElementById("iInfo").style.border = "1px solid orange";
        //     } else {
        //         // 条件不满足时，移除背景警告效果
        //         document.getElementById("iInfo").style.backgroundColor = "";
        //         document.getElementById("iInfo").style.border = "";
        //     }
        // } else {
        //     $("#iRes").val(0);
        // }


        //判断是否切换了板块bk

        if (lastBk !== data.bk) {
            //切换了板块 , 要向tvb 发送 update , 加载bk k线图
            lastBk = data.bk;
            //获取板块图
            //发送 http get请求
            let url = baseUrl + "/tv/chart/bk/update" + "?bk=" + lastBk;

            //通过jquery get 获取 json
            $.get(url, function (data) {
                console.log(data);
            });
        }

        {
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
                volumeSeries, // ma5VolumeSeries,
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


}



function remark(){
    console.log('remark')

    // 获取模态框元素
    var modal = document.getElementById('myModal');
    modal.classList.add('is-active');
    // 获取输入框和确定按钮
    var codeInput = document.getElementById('codeInput');
    codeInput.value = $("#iCode").val();

    var remarkInput = document.getElementById('remarkInput');
    var confirmButton = document.getElementById('confirmButton');
    var cancelBtn = document.getElementById('cancelBtn');

    // 当点击确定按钮时
    confirmButton.addEventListener('click', function() {
        // 获取输入框中的值

        var code = codeInput.value;
        var remark = remarkInput.value;

        // 创建 JSON 对象
        var data = [
            {
                "code": code,
                "remark": remark
            }
        ];

        // 调用接口
        fetch('http://8.142.9.14:28890/stock/remark', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        })
            .then(response => response.json())
            .then(data => {
                console.log('备注信息已添加',data);
            })
            .catch(error => {
                console.error('添加备注信息时出错:', error);
            });
        // 关闭模态框
        modal.classList.remove('is-active');
    });
    cancelBtn.addEventListener('click', function() {
        // 关闭模态框
        modal.classList.remove('is-active');
    });
}

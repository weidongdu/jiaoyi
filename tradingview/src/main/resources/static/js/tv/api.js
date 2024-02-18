//根据code 获取 tv chart
const baseUrl = "http://localhost:28890";
const baseWsUrl = "ws://localhost:28890/websocket";

//记录当前随机股票code + end time

const R = {};

function getTvChartRandom(cb) {
    let url = baseUrl + "/tv/chart/random";
    //通过jquery get 获取 json
    $.get(url, function (data) {
        R.code = data.code;
        R.time = data.k[data.k.length - 1].time;
        console.log(data);
        cb(data);
    });
}

function getTvChartRandomAfter(cb) {
    let url = baseUrl + "/tv/chart/random/after";
    url += "?code=" + R.code + "&time=" + R.time;
    //通过jquery get 获取 json
    $.get(url, function (data) {
        R.code = data.code;
        R.time = data.k[data.k.length - 1].time;
        console.log(data);
        cb(data);
    });
}

function getTvChart(code, cb) {
    let url = baseUrl + "/tv/chart?code=" + code;
    //通过jquery get 获取 json
    $.get(url, function (data) {
        console.log(data);
        cb(data);
    });
}

function getTvChartT(cb) {
    let code = prompt("请输入code 如:[000001]", ""); //将输入的内容赋给变量 name ，
    // let code = $("#iCode").val();
    let url = baseUrl + "/tv/chart/T?code=" + code;
    //通过jquery get 获取 json
    $.get(url, function (data) {
        console.log(data);
        cb(data);
    });
}


function getStockList(cb) {
    let url = baseUrl + "/tv/stockList";
    //通过jquery get 获取 json

    //获取当前 window.location.href 参数async
    if (getQueryString("sync")) {
        url += "?sync=" + true;
    }
    $.get(url, function (data) {
        console.log(data);
        cb(data);
    });
}

function getStockListByType(type) {
    let url = baseUrl + "/tv/stockList/byType?type=" + type;
    //通过jquery get 获取 json
    $.get(url, function (data) {
        //设置 STOCK_LIST
        STOCK_LIST[type] = data;
        alert(type + "=" + data.length)
    });
}

function getStockType(cb) {
    let url = baseUrl + "/tv/stockType";
    //通过jquery get 获取 json
    $.get(url, function (data) {
        console.log(data);
        cb(data);
    });
}

function getQueryString(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}
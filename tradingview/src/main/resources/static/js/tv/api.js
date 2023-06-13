//根据code 获取 tv chart
const baseUrl = "http://localhost:28890";

function getTvChart(code, cb) {
    let url = baseUrl + "/tv/chart?code=" + code;
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
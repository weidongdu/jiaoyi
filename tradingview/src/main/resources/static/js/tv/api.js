//根据code 获取 tv chart

const baseUrl = "http://localhost:8890";

function getTvChart(code,cb) {
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
    $.get(url, function (data) {
        console.log(data);
        cb(data);
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
function selectType() {

    let options = $("#iSelectType option:selected");
    options.val();


}

//获取 股票列表
function filter() {
    //设置 symbols
    let type = selectType();
    let url = "/stock/filter" + "?type=" + type;
    $.get(url, function (data) {
        index_arr = data;//JSON.parse(data);
        alert("size=" + index_arr.length);
        $("#iIndex").val(0 + "/" + index_arr.length);
        chart(index_arr[0], 500);
    }).fail(function () {
        alert('系统异常,请重试');
    });
}

package pro.jiaoyi.tushare.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;

@RestController
@RequestMapping("/ts")
public class TsController {

    @RequestMapping("/test")
    public String test() {
        return "test";
    }

    @Autowired
    private OkHttpUtil okHttpUtil;
    @RequestMapping("/test2")
    public String test2() {
        return okHttpUtil.get("http://www.baidu.com", null).toString();
    }



}

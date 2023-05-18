package pro.jiaoyi.tushare.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ts")
public class TsController {

        @RequestMapping("/test")
        public String test() {
            return "test";
        }
}

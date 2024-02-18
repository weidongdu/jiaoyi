package pro.jiaoyi.tradingview.view;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class TvView {
    @GetMapping("/tv")
    public String chart() {
        return "tv";
    }

    @GetMapping("/tvb")
    public String chartB() {
        return "tvb";
    }
}

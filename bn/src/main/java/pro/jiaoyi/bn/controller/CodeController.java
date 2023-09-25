package pro.jiaoyi.bn.controller;

import com.alibaba.fastjson2.JSON;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.jiaoyi.bn.job.KlineJob;

@RestController("/code")
public class CodeController {

    @GetMapping("/block/add")
    public String block(String symbol){
        KlineJob.BLOCK_SET.add(symbol+"USDT");
        return JSON.toJSONString(KlineJob.BLOCK_SET);
    }

    @GetMapping("/block/del")
    public String blockDel(String symbol){
        KlineJob.BLOCK_SET.remove(symbol+"USDT");
        return JSON.toJSONString(KlineJob.BLOCK_SET);
    }
    @GetMapping("/block/list")
    public String blockList(){
        return JSON.toJSONString(KlineJob.BLOCK_SET);
    }


}

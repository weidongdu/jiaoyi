package pro.jiaoyi.eastm.model;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class EmDataCList {
    /*
        "total": 5400,
        "diff": [{
            "f2": 11.9,
            "f3": -0.92,
            "f4": -0.11,
            "f5": 305362,
            "f6": 364294854.36,
            "f7": 0.92,
            "f8": 0.16,
            "f9": 3.95,
            "f10": 2.8,
            "f12": "000001",
            "f14": "平安银行",
            "f15": 12.0,
            "f16": 11.89,
            "f17": 11.99,
            "f18": 12.01,
            "f22": -0.25,
            "f23": 0.61
          }]
     */
    private int total;
    private List<JSONObject> diff;
}

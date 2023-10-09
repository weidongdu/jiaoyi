package pro.jiaoyi.bn.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.jiaoyi.bn.dao.entity.PremiumIndexEntity;
import pro.jiaoyi.bn.dao.repo.PremiumIndexRepo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/fr")
@RestController
public class FrController {

    @Resource
    PremiumIndexRepo premiumIndexRepo;

    @GetMapping("/list")
    public Map<String, BigDecimal> getFr() {
        List<PremiumIndexEntity> feeList = premiumIndexRepo.findFeeList();
        // fee list to map => symbol -> fee
        Map<String, BigDecimal> feeMap = new LinkedHashMap<>();
        //取前面50 后面50
        if (feeList.size() < 100) {
            return feeMap;
        }


        for (int i = 0; i < 20; i++) {
            PremiumIndexEntity premiumIndexEntity = feeList.get(i);
            feeMap.put(premiumIndexEntity.getSymbol(), premiumIndexEntity.getLastFundingRate());
        }

        for (int i = feeList.size() - 20; i < feeList.size(); i++) {
            PremiumIndexEntity premiumIndexEntity = feeList.get(i);
            feeMap.put(premiumIndexEntity.getSymbol(), premiumIndexEntity.getLastFundingRate());
        }


        return feeMap;
    }
}

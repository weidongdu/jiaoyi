//package pro.jiaoyi.bn.job;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import pro.jiaoyi.bn.model.trade.AccountPosition;
//import pro.jiaoyi.bn.service.BnAccountTradeService;
//
//import java.util.List;
//
//@Slf4j
////@Component
//public class BnTradeJob {
//
//    @Autowired
//    private BnAccountTradeService bnAccountTradeService;
//
//    public static int holdCount = 0;
//
//    @Scheduled(fixedRate = 20 * 1000)
//    public void run() {
//        List<AccountPosition> positions = bnAccountTradeService.positionList();
//        log.info("当前持仓数量{}", positions.size());
//        for (AccountPosition position : positions) {
//            try {
//                bnAccountTradeService.tradeOrder(position);
//            } catch (Exception e) {
//                log.error("tradeOrder error", e);
//            }
//        }
//    }
//}

package pro.jiaoyi.eastm.flow;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.flow.common.FlowNo;
import pro.jiaoyi.eastm.flow.common.TradeTimeEnum;
import pro.jiaoyi.eastm.flow.common.TypeEnum;
import pro.jiaoyi.eastm.flow.common.CommonInfo;
import pro.jiaoyi.eastm.model.EmCList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static pro.jiaoyi.eastm.flow.common.TypeEnum.*;

@Component
@Slf4j
public class IndexFlow implements BaseFlow {

    @Override
    public int getNo() {
        return FlowNo.INDEX;
    }

    @Resource
    private EmClient emClient;

    @Override
    public void runByDay() {

        if (!isTradeDay()) {
            log.info("not trade day");
            return;
        }

        if (!isTradeTime().equals(TradeTimeEnum.POST)) {
            log.info("not 盘后");
            return;
        }

        run();
    }

    @Override
    public void run() {
        log.info("{} run {}", this.getClass().getSimpleName(), getNo());
        //准备基础数据
        List<EmCList> emList = emClient.getClistDefaultSize(true);
        CommonInfo.EM_LIST = emList.stream().filter(em ->
                em.getF3Pct().compareTo(BDUtil.BN3) > 0
                        && em.getF6Amt().compareTo(BDUtil.B5000W) > 0
        ).toList();

        for (EmCList emCList : emList) {
            CommonInfo.CODE_NAME_MAP.put(emCList.getF12Code(), emCList.getF14Name());
        }

        //准备基础数据
        for (TypeEnum typeEnum : CommonInfo.INDEX_ARR) {

            switch (typeEnum) {
                case ALL -> {
                    List<String> list = CommonInfo.CODE_NAME_MAP.keySet().stream().toList();
                    ArrayList<String> codes = sortCodes(emList, list);
                    CommonInfo.TYPE_CODES_MAP.put(typeEnum.getType(), codes);
                }
                case CYCF, HS300, ZZ500 -> {
                    List<String> list = emClient.getIndex(typeEnum.getUrl()).stream().map(EmCList::getF12Code).toList();
                    ArrayList<String> codes = sortCodes(emList, list);
                    CommonInfo.TYPE_CODES_MAP.put(typeEnum.getType(), codes);
                }
                case ZZ1000 -> {
                    List<String> list = emClient.getIndex1000().stream().map(EmCList::getF12Code).toList();
                    ArrayList<String> codes = sortCodes(emList, list);
                    CommonInfo.TYPE_CODES_MAP.put(typeEnum.getType(), codes);

                }

                case INDEX_INCLUDE -> {
                    List<String> list = CommonInfo.TYPE_CODES_MAP.get(CYCF.getType());
                    list.addAll(CommonInfo.TYPE_CODES_MAP.get(HS300.getType()));
                    list.addAll(CommonInfo.TYPE_CODES_MAP.get(ZZ500.getType()));
                    list.addAll(CommonInfo.TYPE_CODES_MAP.get(ZZ1000.getType()));
                    ArrayList<String> codes = sortCodes(emList, list);
                    CommonInfo.TYPE_CODES_MAP.put(typeEnum.getType(), codes);
                }

                case INDEX_EXCLUDE -> {
                    List<String> all = CommonInfo.TYPE_CODES_MAP.get(ALL.getType());
                    List<String> include = CommonInfo.TYPE_CODES_MAP.get(INDEX_INCLUDE.getType());
                    List<String> list = new ArrayList<>(all);
                    list.removeAll(include);

                    ArrayList<String> codes = sortCodes(emList, list);
                    CommonInfo.TYPE_CODES_MAP.put(typeEnum.getType(), codes);
                }

                case EM_MA_UP -> {
                    List<String> list = emClient.xuanguCList().stream().map(EmCList::getF12Code).toList();
                    ArrayList<String> codes = sortCodes(emList, list);
                    CommonInfo.TYPE_CODES_MAP.put(typeEnum.getType(), codes);
                }

                default -> log.error("not support type enum={}", typeEnum);
            }

        }
    }

    @NotNull
    private static ArrayList<String> sortCodes(List<EmCList> emList, List<String> list) {
        ArrayList<String> codes = new ArrayList<>();
        for (EmCList em : emList) {
            if (list.contains(em.getF12Code())) {
                codes.add(em.getF12Code());
            }
        }
        return codes;
    }


}

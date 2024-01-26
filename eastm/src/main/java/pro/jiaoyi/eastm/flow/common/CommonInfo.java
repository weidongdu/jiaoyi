package pro.jiaoyi.eastm.flow.common;

import pro.jiaoyi.eastm.dao.entity.KLineEntity;
import pro.jiaoyi.eastm.model.EmCList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 通用信息
public class CommonInfo {
    public static List<EmCList> EM_LIST = null;
    public static final Map<String, String> CODE_NAME_MAP = new HashMap<>();
    public static final Map<String, KLineEntity> CODE_K_MAP = new HashMap<>();
    public static final Map<String, List<String>> TYPE_CODES_MAP = new HashMap<>();
    public static final TypeEnum[] INDEX_ARR = {
            TypeEnum.ALL,
            TypeEnum.CYCF,
            TypeEnum.HS300,
            TypeEnum.ZZ500,
            TypeEnum.ZZ1000,
            TypeEnum.HIGHP,
            TypeEnum.INDEX_INCLUDE,
            TypeEnum.INDEX_EXCLUDE,
            TypeEnum.EM_MA_UP,
    };
}

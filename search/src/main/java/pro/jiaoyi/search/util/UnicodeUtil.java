package pro.jiaoyi.search.util;

import java.io.UnsupportedEncodingException;

public class UnicodeUtil {

    public static String utf8String(String utf8Str) throws UnsupportedEncodingException {
        String s = new String(utf8Str.getBytes("UTF-8"));
        return s;
    }

    public static void main(String[] args) {
        String s = "\u4e3a\u4e86\u4f7f\u8bdd\u9898\u7ed3\u6784\u5c3d\u53ef\u80fd\u7b80\u6d01\u3001\u4e0d\u91cd\u590d\u3001\u4e0d\u4ea4\u53c9\uff0c\u5e94\u91c7\u53d6\u79d1\u5b66\u7684\u201c\u5206\u7c7b\u5b66\u201d\u65b9\u6cd5\u8fdb\u884c\u5efa\u6784\u3002 \n\u77e5\u4e4e\u7684\u5168\u90e8\u8bdd\u9898\u901a\u8fc7\u7236\u5b50\u5173\u7cfb\u6784\u6210\u4e00\u4e2a\u6709\u6839\u65e0\u5faa\u73af\u7684\u6709\u5411\u56fe \u3002\n\u300c\u6839\u8bdd\u9898\u300d\u5373\u4e3a\u6240\u6709\u8bdd\u9898\u7684\u6700\u4e0a\u5c42\u7684\u7236\u8bdd\u9898\u3002 \n\u8bdd\u9898\u7cbe\u534e\uff1a\u5373\u77e5\u4e4e\u7684 Top 1000 \u9ad8\u7968\u56de\u7b54\u3002 \u8bf7\u4e0d\u8981\u5728\u95ee\u9898\u4e0a\u76f4\u63a5\u7ed1\u5b9a\u300c\u6839\u8bdd\u9898\u300d\u3002 \u8fd9\u6837\u4f1a\u4f7f\u95ee\u9898\u8bdd\u9898\u8fc7\u4e8e\u5bbd\u6cdb\u54e6\u3002";
        try {
            System.out.println(utf8String(s));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}

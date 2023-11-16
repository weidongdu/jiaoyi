package pro.jiaoyi.common.util;

import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;

import java.util.HashMap;
import java.util.HashSet;

public class WeiboUtil {

    public static void main(String[] args) {
        System.out.println("ok");
    }

//    public void getSimpleWeibo(String uid, OkHttpUtil okHttpUtil){
//        String url  = "https://weibo.cn/"+uid;
//        HashMap<String, String> h = new HashMap<>();
//        h.put("authority","weibo.cn");
//        h.put("accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
//        h.put("accept-language","zh-CN,zh;q=0.9,en;q=0.8");
//        h.put("cache-control","no-cache");
//        h.put("cookie","_T_WM=81571544241; MLOGIN=0; SUB=_2A25ITbIeDeRhGeNP7lIT8i7KzDyIHXVrsd5WrDV6PUJbkdAGLRT_kW1NTpVL52UBHRHmWfNmnaidZs_FiiHSQvCN; SCF=AhBtAjtZlr4Lj4FVqo6vK0bXaeJUTaovHRVPqDP_uXyajaW87iB2mwnmx6ZX90rpwxa1j8CskiqHb2gTdFu67Z4.; SSOLoginState=1699332687");
//        h.put("pragma","no-cache");
//        h.put("sec-ch-ua","Chromium\";v=\"118\", \"Google Chrome\";v=\"118\", \"Not=A?Brand\";v=\"99\"");
//        h.put("sec-ch-ua-mobile","?0");
//        h.put("sec-ch-ua-platform","macOS");
//        h.put("sec-fetch-dest","document");
//        h.put("sec-fetch-mode","navigate");
//        h.put("sec-fetch-site","none");
//        h.put("sec-fetch-user","?1");
//        h.put("upgrade-insecure-requests","1");
//        h.put("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
//
//
//        byte[] bytes = okHttpUtil.getForBytes(url, h);
//        HashSet<String> set = new HashSet<>();
//        Jsoup.parse(new String(bytes)).select("div.c").forEach(e->{
//            log.info("{}",e.text());
//            String[] s = e.text().split("收藏");
//            if (s.length > 1){
//                if (!set.contains(s[s.length-1])){
//                    set.add(s[s.length-1]);
//                    wxUtil.send(s[s.length-1]);
//                }
//            }
//        });
//
//    }
}

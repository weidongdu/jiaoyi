package pro.jiaoyi.eastm.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.eastm.config.WxUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Component
@Slf4j
public class ImgService {
    @Resource
    private OkHttpUtil okHttpUtil;
    @Resource
    private WxUtil wxUtil;

    private String download(String url, String fileName) {
        log.info("download {} {}", url, fileName);
        byte[] bytes = okHttpUtil.getForBytes(url, null);
        //将bytes 存储为本地图片
        try {
            // 创建文件对象
            File file = new File(fileName + ".png");
            // 创建输出流
            FileOutputStream fos = new FileOutputStream(file);
            // 写入数据到文件中
            fos.write(bytes);
            // 关闭输出流
            fos.close();
            log.info("download {} done", fileName);
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //44c9d251add88e27b65ed86506f6e5da
    private void downloadFenshi(String code, String name, String token) {
        String t = DateUtil.secondTs();
        String url = "https://webquotepic.eastmoney.com/GetPic.aspx" +
                "?imageType=r" +
                "&type=" +
                "&token=" + token +
                "&nid=" + secid(code) +
                "&timespan=" + t;
        download(url, code + "_fenshi");
    }

    public String downloadKline(String code, String name) {
        String t = DateUtil.secondTs();
        String url = "https://webquoteklinepic.eastmoney.com/GetPic.aspx" +
                "?nid=" + secid(code) +
                "&type=" +
                "&unitWidth=-6" +
                "&ef=" +
                "&formula=RSI" +
                "&AT=1" +
                "&imageType=KXL" +
                "&timespan=" + t;
        String fileAbsPath = download(url, code + "_k");
        return fileAbsPath;
    }

    public String secid(String code) {
        String secid = "0." + code;
        if (code.startsWith("6")) {
            secid = "1." + code;
        }
        return secid;
    }

    public void sendImg(String code) {
        log.info("sendImg {}", code);
        String fileAbsPath = downloadKline(code, "");
        if (fileAbsPath != null) {
            wxUtil.sendImg(fileAbsPath);
        }
    }



}

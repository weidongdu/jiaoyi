//package pro.jiaoyi.eastm;
//
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
//import pro.jiaoyi.eastm.config.WxUtil;
//import pro.jiaoyi.eastm.flow.KlineFlow;
//import pro.jiaoyi.eastm.flow.job.DailyJob;
//import pro.jiaoyi.eastm.service.ImgService;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//
//@SpringBootTest
//@Slf4j
//class ImgTests {
//
//    @Test
//    void contextLoads() {
//    }
//
//
//    @Resource
//    private ImgService imgService;
//
//    @Test
//    public void download() {
//        String s = imgService.downloadKline("603906", "龙蟠科技");
//        log.info("{}", s);
//        if (s !=null){
//            WxUtil
//        }
//    }
//
//
//
//
//}

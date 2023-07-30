package pro.jiaoyi.eastm;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.dao.entity.UserEntity;
import pro.jiaoyi.eastm.dao.repo.UserRepo;

import java.util.List;

@SpringBootTest
@Slf4j
class EastmApplicationDaoTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private EmClient emClient;

    @Autowired
    private UserRepo userRepo;

    @Test
    public void userTest(){
        UserEntity user = new UserEntity();

        user.setAge(2);
        user.setName("xiao2");

        userRepo.save(user);

        List<UserEntity> all = userRepo.findAll();
        System.out.println(JSON.toJSONString(all));
    }

}

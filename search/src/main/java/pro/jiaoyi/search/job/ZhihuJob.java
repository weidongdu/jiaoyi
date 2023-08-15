package pro.jiaoyi.search.job;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.search.dao.repo.SearchResultRepo;

@Slf4j
@Component
public class ZhihuJob {

    @Resource
    private SearchResultRepo searchResultRepo;


    public void run(){

    }


}

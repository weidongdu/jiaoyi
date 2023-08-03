package pro.jiaoyi.search.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.jiaoyi.search.util.Text2Keyword;

import java.util.List;

/**
 * @author dwd
 */
@RestController
@RequestMapping("/keyword")
public class KeywordController {

    @Autowired
    private Text2Keyword text2Keyword;
    @GetMapping("/text")
    public List<String> text( String text) {
        return text2Keyword.text2KeywordList(text);
    }
}

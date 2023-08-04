package pro.jiaoyi.search.scraper;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pro.jiaoyi.common.util.http.okhttp4.OkHttpUtil;
import pro.jiaoyi.search.config.PlatEnum;
import pro.jiaoyi.search.config.SearchTypeEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static pro.jiaoyi.search.config.SourceEnum.BAIDU;

/**
 * 百度关键词抓取
 */
@Component
@Slf4j
public class BaiduKeywordScraper implements Scraper {

    @Resource
    private OkHttpUtil okHttpUtil;

    public static final Map<String, String> MOBILE_HEADERS = new HashMap<>();

    {
        MOBILE_HEADERS.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        MOBILE_HEADERS.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        MOBILE_HEADERS.put("Cache-Control", "no-cache");
        MOBILE_HEADERS.put("Connection", "keep-alive");
        MOBILE_HEADERS.put("Cookie", "wpr=0; BDICON=10123156; BIDUPSID=77B026FA5B06F8583D44C3A22F98371F; PSTM=1654761589; BAIDUID=91AD8E5D2D53DA8CE4CA3AC170BB2D1F:SL=0:NR=10:FG=1; BDORZ=B490B5EBF6F3CD402E515D22BCDA1598; plus_lsv=3965f6be7add0277; plus_cv=1::m:f3ed604d; MSA_WH=390_844; MSA_PBT=148; MSA_ZOOM=1000; COOKIE_SESSION=0_0_0_0_0_0_0_0_0_0_0_0_0_1691035172%7C1%230_0_0_0_0_0_0_0_1691035172%7C1; MSA_PHY_WH=1170_2532; POLYFILL=0; newlogin=1; BAIDUID_BFESS=91AD8E5D2D53DA8CE4CA3AC170BB2D1F:SL=0:NR=10:FG=1; BDUSS=UxHd1lNTFdyZTU0YzJXRXRaN0JxUDZFWVFtN2hna3hJWUlmdTBqVjE1bHYyfkprSUFBQUFBJCQAAAAAAAAAAAEAAABRUDliRHV3ZDI1AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG9Oy2RvTstkV; BDUSS_BFESS=UxHd1lNTFdyZTU0YzJXRXRaN0JxUDZFWVFtN2hna3hJWUlmdTBqVjE1bHYyfkprSUFBQUFBJCQAAAAAAAAAAAEAAABRUDliRHV3ZDI1AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG9Oy2RvTstkV; BDICON=10123156; H_WISE_SIDS=131861_216844_213351_214791_110085_243890_244724_257732_257015_253022_260335_261706_236312_261869_259300_262914_257289_256419_263898_256222_265030_265053_265327_261036_265852_265881_265276_266371_266783_266758_266778_265776_266566_267375_265564_267405_267369_266188_267789_267714_267898_267910_267926_267072_259033_266420_259081_268131_268139_265999_266714_265368_107312_268323_267031_268637_263618_268704_268820_268636_268030_268874_268843_269031_234295_234207_267536_269264_259642_269389_264170_256154_269552_197096_269731_269773_269774_269779_269751_269832_269811_267344_269904_269160_269911_267292_270182_270083_268987_269034_267066_256739_270460_264423_267530_270606; rsv_i=97f9TOuEuJW%2FVeBdzEfwiMLJ63CcyZ%2BgcJ5GAxEYHJteOagG3G3ZHoXow9Gs4wmzOwTPeUBHZmjZ7tESulihOnUkc8MEPVQ; BA_HECTOR=a4812kakaha0808h8kah2lau1icndkf1o; BDPASSGATE=IlPT2AEptyoA_yiU4SDm3lIN8eDEUsCD34OtVnti3ECGh67BmhH84dxLEVz0SWGrGU0l-YyfmqsCpjrFV6xjg0N_gRsVkWpr7Vyk7d727aLUOt-8-rce0rXoGFgNsA8PbRhL-3MEF3V4VFYKbRT9hNgAeOqsxRZIecrR5EDHiMfs2keRBWGFzYKIPoxkPG4fPNu5cPrlnygdPk_cWe8oTi_FgS1iVp1L7qaKiOMmPOD5qkoXGur_QhwlIYvPFXR8_Bjb12q770ys0yU5q-2YSkUtdEiV5sj9IUMMCsDeot2DNv0fJB7AUlPhAKcmjbPbLQdWKQ3zmtsGPTYyynZlJp-j0aLUOmvqNE95RNGGiBjZCXwVqlOMJezY97YzRv4Q4BRYZwNdSikErVaAreCmpSr2HwPgi_lwT0Ug_Sjn9Xgwfn63Gm4K6Hqzc7Rvu7O8ZmGX164p0LbnSIhKuuKyJaXaVKr9E6g2sESWh5DveTCHBfj6RMR7kzgCwnV7TXC3ceKbPjDfXPyKeLZfwrXqvszDu9STzTC3tT8PLOKi4fgZ0oUcmqV4CmjJzGy_eXxAO3bM8mvkE5fTnCdMwtyIjyZnlPA1FtZqZujjJPITwcSIkWwV0yYtpaTHDeAQXJjkufxGIvD0UY8Y1kD9X7lRgrtA2V4vIJSCvqTf1z1g3TWO1C8fG0xP55VKg7UWS_fgC5bxaNYMIliBp4AsP2qip7diFOEjEkV-y7zJ3-8KO4aBz4lJqUVu8ron4o8TYujezIQLoagqUyKuewfzOKytgJgVi9OIgQx9A0m-Omvk8FKyIBEq-nm4; wpr=0; BDSVRTM=9; delPer=0; PSCBD=25%3A3_16%3A3_10%3A2; SE_LAUNCH=5%3A28184535_16%3A28184569_25%3A28184546_10%3A28184573; PSINO=5; __bsi=10842111414974606422_00_34_N_N_8_0303_c02f_Y; FC_MODEL=0_0_0_0_0_0_0_0_0_0_0_0_0_0_0_0_1_0_1691072859%7C2%230_0_0_0_0_0_1691071665%7C1%230_adx_0_0_0_0_0_1691071665; H_WISE_SIDS=131861_216844_213351_214791_110085_243890_244724_257732_257015_253022_260335_261706_236312_261869_259300_262914_257289_256419_263898_256222_265030_265053_265327_261036_265852_265881_265276_266371_266783_266758_266778_265776_266566_267375_265564_267405_267369_266188_267789_267714_267898_267910_267926_267072_259033_266420_259081_268131_268139_265999_266714_265368_107312_268323_267031_268637_263618_268704_268820_268636_268030_268874_268843_269031_234295_234207_267536_269264_259642_269389_264170_256154_269552_197096_269731_269773_269774_269779_269751_269832_269811_267344_269904_269160_269911_267292_270182_270083_268987_269034_267066_256739_270460_264423_267530_270606; PSINO=5; BDSVRBFE=Go; BDSVRTM=59; __bsi=10948402556990053040_00_21_N_N_144_0303_c02f_Y");
        MOBILE_HEADERS.put("Pragma", "no-cache");
//        MOBILE_HEADERS.put("Referer", "https://m.baidu.com/ssid=51504475776432353962/s?word=zhaogushu&ts=1665444&t_kt=0&ie=utf-8&rsv_iqid=8234065662269839228&rsv_t=a3b3C1gfYB0%252F9ClUWiUwMAv6ND23nUHVtuIQ1zmXNAbzYpfYWTt1&sa=ib&rsv_pq=8234065662269839228&rsv_sug4=13124&tj=1&inputT=10986&sugid=29568277565893&ss=100");
        MOBILE_HEADERS.put("Referer", "https://m.baidu.com/");
        MOBILE_HEADERS.put("Sec-Fetch-Dest", "document");
        MOBILE_HEADERS.put("Sec-Fetch-Mode", "navigate");
        MOBILE_HEADERS.put("Sec-Fetch-Site", "same-origin");
        MOBILE_HEADERS.put("Sec-Fetch-User", "?1");
        MOBILE_HEADERS.put("Upgrade-Insecure-Requests", "1");
        MOBILE_HEADERS.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1");
    }

    public void pc(String keyword) {

    }

    public void mobile(String keyword, int pn) {
        //需要的参数
        //ie=utf-8
        //pn=10 第2页 不写默认第一页
        //word=招股书
        String url = "https://www.baidu.com/s?word=" + keyword + "&ie=utf-8";
        if (pn > 1) {
            url += "&pn=" + (pn - 1) * 10;
        }
        byte[] bytes = okHttpUtil.getForBytes(url, MOBILE_HEADERS);
        if (bytes.length == 0) {
            log.error("百度关键词抓取失败");
            return;
        }

        String html = new String(bytes);
        Document doc = Jsoup.parse(html);
        doc.select("head").remove();
        doc.select("script").remove();
        doc.select("style").remove();

        Element results = doc.getElementById("results");
        if (results == null) {
            log.error("百度关键词抓取失败");
            return;
        }


        // <div class="c-result result" srcid="sp_purc_atom" new_srcid="5544" order="1" tpl="sp_purc_atom" data-log="{&quot;fm&quot;:&quot;alop&quot;,&quot;ensrcid&quot;:&quot;sp_purc_atom&quot;,&quot;order&quot;:1,&quot;mu&quot;:&quot;https://m.baidu.com/sf/vsearch?pd=goods_tab&amp;tn=vsearch&amp;sa=vs_tab&amp;atn=index&amp;word=%E5%A5%BD%E7%9C%8B%E7%9A%84%E7%AB%A5%E9%9E%8B&quot;}" nr="1" data-url-sign="13790298292028139682" card-runtime="0" ready="1"><link rel="preload" href="//ms.bdstatic.com/se/static/ala_atom/app/sp_purc_atom/index_e61c54c.js" crossorigin="anonymous" as="script"><div class="c-result-content"><article data-aftclk="" rl-node="" rl-highlight-self="" rl-highlight-radius=".12rem" rl-link-href="https://m.baidu.com/from=844b/bd_page_type=1/ssid=51504475776432353962/uid=0/pu=usm%409%2Csz%401320_2001%2Cta%40iphone_1_13.2_25_13.0/baiduid=91AD8E5D2D53DA8CE4CA3AC170BB2D1F/w=0_10_/t=iphone/l=1/tc?ref=www_iphone&amp;lid=8162275666615643073&amp;order=1&amp;fm=alop&amp;isAtom=1&amp;clk_info=%7B%22tplname%22%3A%22sp_purc_atom%22%2C%22srcid%22%3A%225544%22%7D&amp;is_baidu=1&amp;tj=sp_purc_atom_1_0_10_l1&amp;wd=&amp;eqid=71463a792370abc11000000464cbbf86&amp;w_qd=IlPT2AEptyoA_yk57RsatwevFS2UaWUbqD7&amp;bdver=2_1&amp;tcplug=1&amp;dict=-1&amp;sec=31742&amp;di=8d05b81a4ef3d48a&amp;bdenc=1&amp;nsrc=mMQ82pJog0pERasoA4JVBOTJxK9PWSjxtBhmnVx4hYeVMACQ6vMJJHn0TKgHLBAJuchmNL3cUGKeAURowCRs1tyMHbNEO8vAJ4RhW1boP2rZHkDPsZA12kwhVB5a7YtRiKcAIm9tZhqaztsJXQymNpa1ULl%2B4SD7o8TOUHnTYxkKpdpI4BMZhbzKBKBcc84wPE0ktDWWnvnePcPbofJhiQYejFOFuVERvRl9ByyOIci1PsWznMMrN8TiTvaUddlFgY6vIXJsEBa1O90LURbTGFPr5Et8AuhM21GfxnH9LVLQhd%2F79VIDQqgZdNbn8Di7N2FQPxOU43b5rDdUpbX7yGyvSCGzEabHbN3Ig3yQFrU%3D" rl-link-data-log="{&quot;mu&quot;:&quot;/sf/vsearch?pd=goods_tab&amp;word=%E5%A5%BD%E7%9C%8B%E7%9A%84%E7%AB%A5%E9%9E%8B&amp;tn=vsearch&amp;sa=vs_ala_5544_1&amp;lid=8162275666615643073&amp;ms=1&amp;from=844b&amp;atn=index&quot;}" data-visible="1" data-visible-threshold="0.2" data-visible-log="{&quot;ct&quot;:138,&quot;cst&quot;:3,&quot;clk_extra&quot;:{&quot;tplname&quot;:&quot;sp_purc_atom&quot;,&quot;srcid&quot;:&quot;5544&quot;,&quot;true_pos&quot;:&quot;26607618800|0|0|0|0|1|0|1|0|0|0|0|0|0|0,20758890438|0|0|0|0|1|0|0|0|1|0|0|0|0|0,30870910990|0|0|0|0|1|0|0|0|2|0|0|0|0|0,26825148455|0|0|0|0|1|0|1|0|3|0|0|0|0|0&quot;,&quot;tag&quot;:&quot;2&quot;}}" class="c-container _zpTVW _2kGRF" style="background-color:;padding-top:0;" atom-root-7d6a0f60-d982-4e9b-9dd6-512fcd9ffd20=""><!----><!----><!----><section class="_dCman" style="padding-top:;padding-bottom:;background-color:;"><div data-a-ad8520ca="" data-module="c-ksh" class="kg-service-header"><div data-a-ad8520ca="" class="kg-header-bg-image kg-header-top-radius c-row-tile kg-header-border" style=""><div data-a-ad8520ca="" class="_aowCn kg-header-top-radius"><!----><!----><div data-a-ad8520ca="" class="_jHCcT" style="background: 0% 0% / 100% 100% no-repeat;"></div><div data-a-ad8520ca="" class="_3z9Lr"></div></div><div data-a-ad8520ca="" class="c-kg-header-vertical-center kg-header-pd" style="height:.76rem;"><div class="c-touchable-feedback c-touchable-feedback-no-default c-kg-header-vertical-center" data-a-ad8520ca=""><div data-a-2206cd25="" class="c-row" style="margin-left:-1.0638297872340425%;margin-right:-1.0638297872340425%;" data-a-ad8520ca="" data-uclk="{}" ubc-clk=""><div data-a-339f6d90="" class="c-kg-header-vertical-center c-span9.5" style="padding-left:1.0638297872340425%;padding-right:1.0638297872340425%;max-width:;" data-a-ad8520ca="" data-a-2206cd25=""><div data-a-ad8520ca="" class="title-line c-title" data-a-339f6d90=""><!----><div data-a-ad8520ca="" class="title-main c-line-clamp1 c-title" style="font:;"><span data-a-ad8520ca="">
        Elements results_es = results.select(".c-result.result");
        if (results_es.isEmpty()) {
            log.info("no result");
            return;
        }

        SearchResult sr = new SearchResult(keyword, BAIDU.name(), PlatEnum.MOBILE.name(), SearchTypeEnum.RESULT.name());
        sr.setItems(new ArrayList<>());
        sr.setKeywordRelated(new ArrayList<>());
        sr.setKeywordDropdown(new ArrayList<>());

        sr.setDoc(null);

        for (Element result : results_es) {
            String attr = result.attr("data-log");
            JSONObject jsonObject = JSONObject.parseObject(attr);

            Element content = result.selectFirst(".c-result-content");
            if (content == null) continue;
            SearchResult.Item item = new SearchResult.Item();

            String tpl = result.attr("tpl");
            log.info("tpl={}", tpl);

            switch (tpl) {
                case "recommend_list": {
                    log.debug(result.html());
                    Elements dataToolDivE = result.getElementsByAttribute("data-tool");
                    if (dataToolDivE.size() == 0) continue;
                    String dataToolStr = dataToolDivE.get(0).attr("data-tool");
                    JSONObject dataToolJSONObject = JSONObject.parse(dataToolStr);
                    JSONArray jsonArray = dataToolJSONObject.getJSONObject("feedback")
                            .getJSONObject("suggest")
                            .getJSONObject("content")
                            .getJSONObject("tplData")
                            .getJSONArray("content");

                    if (jsonArray == null || jsonArray.size() == 0) {
                        continue;
                    }

                    for (int i = 0; i < jsonArray.size(); i++) {
                        String text = jsonArray.getJSONObject(i).getString("text");
                        sr.getKeywordRelated().add(text);
                    }

                    Element titleE = result.selectFirst(".c-title");
                    String title = titleE == null ? null : titleE.text();
                    item.setTitle(title);
                    item.setContent(jsonArray.toJSONString());

                    break;
                }
                case "image_normal_tag": {
                    log.debug("image_normal_tag");
                    Element titleE = result.selectFirst(".c-title");
                    String title = titleE == null ? null : titleE.text();
                    Elements itemE = content.select(".c-tags-item");
                    ArrayList<String> tmp = new ArrayList<>();
                    for (Element element : itemE) {
                        tmp.add(element.text());
                    }
                    item.setTitle(title);
                    item.setContent(JSON.toJSONString(tmp));
                    sr.getKeywordRelated().addAll(tmp);
                    break;
                }
                case "sg_kg_entity_san": {
                    log.debug("sg_kg_entity_san");
                }
                default: {
                    Element titleE = content.selectFirst(".c-title-text");
                    if (titleE == null || !StringUtils.hasText(titleE.text())) {
                        titleE = content.selectFirst("a");
                    }
                    String title = titleE == null ? null : titleE.text();
                    //data-module="sc_p"
                    Element contentE = content.selectFirst("div[data-module=sc_p]");
                    if (contentE == null || !StringUtils.hasText(contentE.text())) {
                        contentE = content.selectFirst("div[role=text]");
                    }
                    String contentStr = contentE == null ? null : contentE.text();
                    item.setTitle(title);
                    item.setContent(contentStr);
                    break;
                }

            }


            String urlItem = jsonObject.getString("mu");
            item.setUrl(urlItem);
            item.setRealUrl(urlItem);//真实url
            item.setPage(pn);//搜索结果页数
            item.setRank(jsonObject.getInteger("order"));//搜索结果排名

            sr.getItems().add(item);
        }

        Element pageRelativeE = doc.getElementById("page-relative");
        if (pageRelativeE != null) {
            Elements pageRelativeEs = pageRelativeE.select(".rw-list-new");
            for (Element pageRelative : pageRelativeEs) {
                String text = pageRelative.text();
                if (StringUtils.hasText(text)) {
                    sr.getKeywordRelated().add(text);
                }
            }
        }
        log.info("sr={}", sr);
    }

    /**
     * 下拉框
     *
     * @param keyword
     */
    public void dropdown(String keyword) {

    }

    /**
     * 相关搜索
     *
     * @param keyword
     */
    public void related(String keyword) {

    }


    @Override
    public SearchResult search(String keyword) {
        return null;
    }


}

package com.finance24h.api.controller.common;

import com.finance24h.api.helpers.*;
import com.finance24h.api.model.ShareDetailDTO;
import com.finance24h.api.service.ArticleService;
import com.finance24h.api.service.StockService;
import com.google.gson.*;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

//import org.slf4j.LogStashHelper;

/**
 * @class ArticleController
 * @by HaiLH
 * @version: sprint-1
 * @date: 21/09/2017
 * Test link local:
 * http://10.2.0.36:8081/v1/ios/article/{id}/
 * http://10.2.0.36:8081/v1/ios/article/{8}/?access_token=12323
 * http://10.2.0.36:8081/v1/ios/article_ext/{12}/?access_token=12323
 */
@Component
public class ArticleController extends BaseController {
    @Value("${article.relative_number}")
    private int relativeNumber;

    @Autowired
    private ArticleService articleService;
    @Autowired
    private StockService stockService;
    @Autowired
    private LogStashHelper log;

    @RequestMapping(value = "/article/{id}", produces = "application/json")
    @ResponseBody
    @HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "articleV1")
    public String article(HttpServletRequest request, @PathVariable String id) throws CheckParamException {
        super.checkParams(request);
        try {
            int article_id = Integer.parseInt(id);
            log.logInfo("article_id = " + article_id);

            //1. Get Article from Elasticsearch
            JsonObject articleDetail = articleService.getArticleDetail(article_id);

            //2. Get ProviderName by crawl_id
            Integer crawlId = Integer.parseInt(articleDetail.get("crawl_id").getAsString());
            String providerName = articleService.getProviderByCrawlId(crawlId);

            //3. Get Groups
            JsonObject articleHeaderGroupJson = getArticleHeaderGroup(articleDetail, providerName);
            JsonObject articleContentGroupJson = getArticleContentGroup(articleDetail);

            //4. Add groups into data
            JsonArray data = new JsonArray();
            data.add(articleHeaderGroupJson);
            data.add(articleContentGroupJson);

            String articleDetailTemplate = new TemplateHelper(data).toJson().toString();
            log.logAPI(request);
            return articleDetailTemplate;
        } catch (Exception e) {
            log.logError("Error article method:" + e);
            return super.error(HttpStatus.OK.value(), "Get Article Detail Error").toString();
        }
    }


    @RequestMapping(value = "/article_ext/{id}", produces = "application/json")
    @ResponseBody
    @HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "articleV1")
    public String article_ext(HttpServletRequest request, @PathVariable String id) throws CheckParamException {
        super.checkParams(request);
        try {
            int article_id = Integer.parseInt(id);
            log.logInfo("article_id = " + article_id);

            //1. Get Article from Elasticsearch
            JsonObject articleDetail = articleService.getArticleDetail(article_id);

            //2. Get ProviderName by crawl_id
            //Integer crawlId = Integer.parseInt( articleDetail.get("crawl_id").getAsString() );
            //String providerName = articleService.getProviderByCrawlId(crawlId);

            //3. Get Groups
            JsonArray comTagsArray = articleDetail.getAsJsonArray("com_tags");
            JsonObject quotesGroupJson = getQuotesGroup(comTagsArray);
            JsonObject shareGroupJson = getShareGroup(articleDetail);
            int currentId = articleDetail.get("id").getAsInt();
            JsonArray tagsArray = articleDetail.getAsJsonArray("tags");
            JsonObject relatedArticlesGroupJson = getRelatedArticlesGroup(currentId, tagsArray);

            //4. Add groups into data
            JsonArray data = new JsonArray();
            data.add(quotesGroupJson);
            data.add(shareGroupJson);
            data.add(relatedArticlesGroupJson);

            String articleDetailTemplate = new TemplateHelper(data).toJson().toString();
            log.logAPI(request);
            return articleDetailTemplate;
        } catch (Exception e) {
            log.logError("Error article_ext method:" + e);
            return super.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Get Article Detail Error").toString();
        }
    }


    /**
     * @method: getArticleHeaderGroup
     * Create header group for output json
     */
    private JsonObject getArticleHeaderGroup(JsonObject articleDetail, String providerName) {

        //companyService.getShareIdByTag("Upcom");

        JsonObject data = new JsonObject();
        data.addProperty("id", Integer.parseInt(articleDetail.get("id").getAsString()));
        data.addProperty("title", articleDetail.get("title").getAsString());
        data.addProperty("preview_content", articleDetail.get("short_content").getAsString());
        data.addProperty("figure_type", articleDetail.get("figure_type").getAsString());
        data.addProperty("thumbnail", articleDetail.get("figure").getAsString());
        data.addProperty("provider", providerName);
        data.addProperty("published_date", Integer.parseInt(articleDetail.get("published_at").toString()));
        data.addProperty("share_url", articleDetail.get("url").getAsString());
        data.addProperty("video_url", articleDetail.get("video_url").getAsString());

        JsonArray dataGroupHeader = groupBlockHeader(data);

        GroupHelper articleHeader = new GroupHelper("article_detail_header_group", dataGroupHeader);

        return articleHeader.toJson(); // new GroupHelper("article_detail_header_group", null).toJson();
    }


    /**
     * @method: getArticleContentGroup
     * Create Content group for output json
     */
    private JsonObject getArticleContentGroup(JsonObject articleDetail) {
        String content = articleDetail.get("content").getAsString();

        //Convert json String to JsonObject
        JsonParser parser = new JsonParser();
        JsonObject objContent = parser.parse(content).getAsJsonObject();

        //Get children from content
        JsonArray dataGroup = groupBlockHeader(objContent);
        GroupHelper articleHeader = new GroupHelper("article_detail_content_group", dataGroup);
        return articleHeader.toJson();
    }

    private JsonObject getQuotesGroup(JsonArray comTagsArray) {
        String[] comTags = new Gson().fromJson(comTagsArray, String[].class);
        List<ShareDetailDTO> shareDetailDTOList = stockService.getStockDetails(comTags, 10);
        JsonArray relatedStockBoxes = getAllShareGroup(shareDetailDTOList);
        return new GroupHelper("quotes_from_article_group", Utilities.getLabel("quotes_from_article"), relatedStockBoxes).toJson();
    }

    private JsonObject getShareGroup(JsonObject articleDetail) {
        JsonObject qoutesInfo = new JsonObject();
        qoutesInfo.addProperty("share_url", "http://facebook.com");
        return new GroupHelper("share_this_story_group", "Share this story", null, qoutesInfo).toJson();
    }

    /**
     * @method: getRelatedArticlesGroup
     * Create Related Video group for output json
     */
    private JsonObject getRelatedArticlesGroup(int currentId, JsonArray tags) {
        JsonArray relatedArticle = new JsonArray();
        JsonArray relatedArticleData = articleService.getRelatedArticlesByTags(currentId, tags, relativeNumber);
        JsonObject relatedArticleBox = new BoxHelper("related_article_box", Utilities.getLabel("related_article_box"), relatedArticleData).toJson();
        relatedArticle.add(relatedArticleBox);
        return new GroupHelper("related_articles_group", Utilities.getLabel("related_articles_group"), relatedArticle).toJson();
    }

    private JsonArray groupBlockHeader(JsonElement data) {
        JsonArray arrData = new JsonArray();
        arrData.add(data);
        JsonObject parentData = new JsonObject();
        parentData.addProperty("box_name", "");
        parentData.add("data", arrData);
        JsonArray dataGroup = new JsonArray();
        dataGroup.add(parentData);
        return dataGroup;
    }

    private JsonArray getAllShareGroup(List<ShareDetailDTO> shareDetailDTOS) {
        JsonArray boxElements = new JsonArray();
        for (ShareDetailDTO dto : shareDetailDTOS) {
            if (dto.getSymbol() == null) {
                continue;
            }
            JsonObject obj = new JsonObject();
            obj.addProperty("symbol", dto.getSymbol());
            obj.addProperty("company_name", dto.getCompanyName());
            obj.addProperty("price", dto.getPrice());
            obj.addProperty("change_percent", dto.getPercentChange());
            obj.addProperty("pre_change_percent", dto.getPrePercentChange());
            boxElements.add(obj);
        }
        JsonArray boxArray = new JsonArray();
        boxArray.add(new BoxHelper("", boxElements).toJson());
        return boxArray;
    }
}

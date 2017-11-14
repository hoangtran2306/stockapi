package com.finance24h.api.service;

import com.finance24h.api.helpers.Cache;
import com.finance24h.api.helpers.LogStashHelper;
import com.finance24h.api.helpers.Utilities;
import com.google.gson.*;
import io.searchbox.client.JestClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service("articleService")
public class ArticleServiceImpl implements ArticleService {

    private static final String INDEX_ARTICLE = "articles";
    private static final String INDEX_CRAWL_ARTICLE = "crawl_articles";

    private static final String CACHED_ARTICLE_KEY_PREFIX = "stock24h__article_detail_";

    @Autowired
    LogStashHelper log;

    @Autowired
    JestClient esClient;
    final JsonParser jsonParser = new JsonParser();

    @Override
    public String getProviderByCrawlId(int crawlId) {
        String provider = "";
        Map<Integer, Integer> crawlIdMap = new HashMap<>();
        crawlIdMap.put(crawlId, 0);
        JsonObject allProvider = getProviders(crawlIdMap);
        JsonElement providerElement = allProvider.get(String.valueOf(crawlId));
        if (providerElement != null) {
            provider = providerElement.getAsString();
        }
        return provider;
    }

    @Override
    public JsonArray getListHomeArticle(String tableName, long timestamp, int limit) {
        return getListHomeArticle(null, tableName, timestamp, limit);
    }

    @Override
    public JsonArray getListArticle(String tableName, long timestamp) {
        HashMap<String, String> sortHash = new HashMap<>();
        String timeField = Utilities.timeFieldByTable().get(tableName);
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(QueryBuilders.rangeQuery(timeField).lt(timestamp));
        sortHash.put(timeField, "desc");
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery, sortHash, 20);
        log.logInfo("fullQuery =" + fullQuery);
        JsonArray result = Utilities.getEsResult(esClient, fullQuery, INDEX_ARTICLE);
        return result;
    }

    @Override
    public JsonObject getArticleDetail(int articleId) {
    		String keyName = CACHED_ARTICLE_KEY_PREFIX + String.valueOf(articleId);;
    		JsonObject cached = Cache.getJsonObject(keyName);
        if (cached != null) return cached;
        //If have no cached . Get data from ES
        try {
            log.logInfo("No cached => get from ES");

            BoolQueryBuilder boolQuery = new BoolQueryBuilder();
            boolQuery.must(QueryBuilders.termQuery("id", articleId));
            JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);

            log.logInfo("articles query: " + fullQuery.toString());
            JsonObject result = Utilities.getOneEsResult(esClient, fullQuery, INDEX_ARTICLE);

            //Save result to cache
            	Cache.set(keyName, Cache.TIME_ARTICLE_DETAIL, result);
            return result;

        } catch (Exception e) {
            log.logError("Error Build query:" + e);
            throw e;
        }
    }

    @Override
    public JsonArray getListHomeArticle(List<String> followSymbols, String tableName, long timestamp, int limit) {
        String timeField = Utilities.timeFieldByTable().get(tableName);
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(QueryBuilders.termQuery("category", "stock"));
        boolQuery.must(QueryBuilders.rangeQuery(timeField).lt(timestamp));
        if (followSymbols != null) {
            if (followSymbols.size() > 0) {
                for (String oneSymbol : followSymbols) {
                    boolQuery.should(QueryBuilders.matchQuery("com_tags", oneSymbol));
                }
                boolQuery.minimumShouldMatch(1);
            } else {
                return new JsonArray();
            }
        } else {
            boolQuery.must(QueryBuilders.wildcardQuery("com_tags", "*"));
        }
        HashMap<String, String> sortHash = new HashMap<>();
        sortHash.put(timeField, "desc");
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery, sortHash, 20);
        log.logInfo("fullQuery:" + fullQuery);
        return Utilities.getEsResult(esClient, fullQuery, INDEX_ARTICLE);
    }

    @Override
    public JsonArray getRelatedArticlesByStock(String stockCode, int limit, String tableName, long timeStamp) {
        JsonArray articleArr = new JsonArray();
        String timeField = Utilities.timeFieldByTable().get(tableName);
        HashMap<String, String> sortHash = new HashMap<>();
        sortHash.put(timeField, "desc");
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        if (timeStamp > 0) {
            boolQuery.must(QueryBuilders.rangeQuery(timeField).lt(timeStamp));
        }
        boolQuery.should(QueryBuilders.matchQuery("com_tags", stockCode));
        boolQuery.minimumShouldMatch(1);
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery, sortHash, limit);
        JsonArray articleList = Utilities.getEsResult(esClient, fullQuery, "articles");
        Map<Integer, Integer> crawlIdMap = new HashMap<>();
        for (JsonElement oneArticle : articleList) {
            int crawlId = oneArticle.getAsJsonObject().get("crawl_id").getAsInt();
            crawlIdMap.put(crawlId, 0);
        }
        JsonObject allProvider = getProviders(crawlIdMap);
        for (JsonElement oneArticle : articleList) {
            articleArr.add(normalize(oneArticle.getAsJsonObject(), allProvider));
        }
        return articleArr;
    }

    @Override
    public JsonArray getRelatedArticlesByTags(int currentId, JsonArray listTags, int limit) {
        List<String> tags = new Gson().fromJson(listTags, List.class);
        return getRelatedArticle(currentId, tags, limit);
    }

    private JsonObject normalize(JsonObject obj, JsonObject allProvider) {
        JsonObject newObj = new JsonObject();
        newObj.add("article_id", obj.get("id"));
        newObj.add("title", obj.get("title"));
        newObj.add("preview_content", obj.get("short_content"));
        newObj.addProperty("type", Utilities.convertArticleType(obj.get("figure_type").getAsString()));
        newObj.add("thumbnail", obj.get("figure"));
        String crawlIdString = obj.get("crawl_id").getAsString();
        String provider = allProvider.get(crawlIdString) == null ? "" : allProvider.get(crawlIdString).getAsString();
        newObj.addProperty("provider", provider);
        newObj.add("published_date", obj.get("published_at"));
        newObj.add("video_url", obj.get("video_url"));
        newObj.add("share_url", obj.get("url"));
        JsonArray tagArray = new JsonArray();
        JsonArray tags = obj.get("com_tags").getAsJsonArray();
        for (int i = 0; i < tags.size(); i++) {
            JsonObject tmp = new JsonObject();
            tmp.addProperty("symbol", tags.get(i).getAsString());
            tagArray.add(tmp);
        }
        newObj.add("share_tags", tagArray);
        return newObj;
    }

    private JsonArray getRelatedArticle(int currentId, List<String> tags, int limit) {
        try {
            if (tags == null || tags.size() == 0) {
                return new JsonArray();
            }
            BoolQueryBuilder boolQuery = new BoolQueryBuilder();
            HashMap<String, String> sortMap = new HashMap<>();
            sortMap.put("id", "desc");
            boolQuery.mustNot(QueryBuilders.termQuery("id", currentId));
            for (String tag : tags) {
                boolQuery.should(QueryBuilders.matchQuery("tags.keyword", tag.trim()));
            }
            boolQuery.minimumShouldMatch(1);
            JsonObject fullQuery = Utilities.buildFullQuery(boolQuery, sortMap, limit);
            log.logInfo("----related query: \n" + fullQuery.toString());
            JsonArray articleList = Utilities.getEsResult(esClient, fullQuery, INDEX_ARTICLE);
            Map<Integer, Integer> crawlIdMap = new HashMap<>();
            for (JsonElement oneArticle : articleList) {
                int crawlId = oneArticle.getAsJsonObject().get("crawl_id").getAsInt();
                crawlIdMap.put(crawlId, 0);
            }
            JsonObject allProvider = getProviders(crawlIdMap);
            JsonArray articleArr = new JsonArray();
            for (JsonElement oneArticle : articleList) {
                articleArr.add(normalize(oneArticle.getAsJsonObject(), allProvider));
            }
            return articleArr;
        } catch (Exception e) {
            log.logError("Error getRelatedVideoArticle method:" + e);
            throw e;
        }
    }

    @Override
    public JsonObject getProviders(Map<Integer, Integer> crawlIds) {
        JsonObject finalJson = new JsonObject();
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        for (Map.Entry<Integer, Integer> oneId : crawlIds.entrySet()) {
            boolQuery.should(QueryBuilders.termQuery("id", oneId.getKey()));
        }
        boolQuery.minimumShouldMatch(1);
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery, crawlIds.size());
        JsonArray resultArray = Utilities.getEsResult(esClient, fullQuery, INDEX_CRAWL_ARTICLE);
        for (JsonElement oneResult : resultArray) {
            String crawlId = oneResult.getAsJsonObject().get("id").getAsString();
            String provider = oneResult.getAsJsonObject().get("from").getAsString().toUpperCase();
            finalJson.addProperty(crawlId, provider);
        }
        return finalJson;
    }
}

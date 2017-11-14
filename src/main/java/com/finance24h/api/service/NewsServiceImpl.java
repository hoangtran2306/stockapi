package com.finance24h.api.service;

import com.finance24h.api.helpers.BoxHelper;
import com.finance24h.api.helpers.Cache;
import com.finance24h.api.helpers.LogStashHelper;
import com.finance24h.api.helpers.Utilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service("newsService")
public class NewsServiceImpl implements NewsService {
	@Autowired
	private LogStashHelper logger;
	@Autowired
	private ArticleService articleService;
	
	@Override
	public JsonArray getNewestHomeArticles(String tableName, long timestamp, boolean pagingCache) {
		return getNewestHomeArticles(0, null, tableName, timestamp, pagingCache);
	}

	@Override
	public JsonArray getListArticles(String tableName, long timestamp, boolean pagingCache) {
		logger.logInfo("Get news for news list");
		String keyName = "stock24h_news_list" + (pagingCache ? ("_" + timestamp) : "");
		JsonArray cached = Cache.getJsonArray(keyName);
		if (cached != null) return cached;
		JsonArray articleArr;
		logger.logInfo("Get from ES");
		JsonArray listNewsArr = articleService.getListArticle(tableName, timestamp);
		articleArr = buildJson(listNewsArr);
		JsonObject newsBox = new BoxHelper("", articleArr).toJson();
		JsonArray boxArray = new JsonArray();
		boxArray.add(newsBox);
		Cache.set(keyName, Cache.TIME_NEWS_LIST, boxArray);
		return boxArray;
	}
	
	private JsonArray buildJson(JsonArray newsJsonArray) {
		JsonArray articleArr = new JsonArray();
		Map<Integer, Integer> crawlIdMap = new HashMap<>();
		for(JsonElement oneArticle : newsJsonArray) {
			int crawlId = oneArticle.getAsJsonObject().get("crawl_id").getAsInt();
			crawlIdMap.put(crawlId, 0);
		}
		JsonObject allProvider = articleService.getProviders(crawlIdMap);
		for(JsonElement oneArticle : newsJsonArray) {
			JsonObject oneArticleJson = oneArticle.getAsJsonObject();
			JsonObject articleJson = new JsonObject();
			articleJson.addProperty("article_id", oneArticleJson.get("id").getAsInt());
			articleJson.addProperty("title", oneArticleJson.get("title").getAsString());
			articleJson.addProperty("preview_content", oneArticleJson.get("short_content").getAsString());
			articleJson.addProperty("type", Utilities.convertArticleType(oneArticleJson.get("figure_type").getAsString()));
			articleJson.addProperty("thumbnail", oneArticleJson.get("figure").getAsString());
			String crawlId = oneArticleJson.get("crawl_id").getAsString();
			String provider = allProvider.get(crawlId) == null ? "" : allProvider.get(crawlId).getAsString();
			articleJson.addProperty("provider", provider);
			articleJson.addProperty("published_date", oneArticleJson.get("published_at").getAsLong());
			articleJson.addProperty("video_url", oneArticleJson.get("video_url").getAsString());
			JsonArray comTags = oneArticleJson.get("com_tags").getAsJsonArray();
			JsonArray shareTagArray = new JsonArray();
			String category = oneArticleJson.get("category").getAsString();
			if (category.equals("stock")) {
				for (JsonElement oneComTag : comTags) {
					String oneComTagString = oneComTag.getAsString();
					if (oneComTagString.length() == 0) {
						continue;
					}
					JsonObject shareTagObject = new JsonObject();
					shareTagObject.addProperty("symbol", oneComTagString);
					shareTagArray.add(shareTagObject);
				}
			}
			articleJson.add("share_tags", shareTagArray);
			
			articleArr.add(articleJson);
		}
		return articleArr;
	}

	@Override
	public JsonArray getNewestHomeArticles(int deviceId, List<String> followSymbols, String tableName, long timestamp, boolean pagingCache) {
		logger.logInfo("Get news for home");
		JsonArray articleArr;
		String followSymbolStr = "";
		if (followSymbols != null) {
			Collections.sort(followSymbols);
			followSymbolStr = String.valueOf(deviceId);
			for (String follow : followSymbols) {
				followSymbolStr += "_" + follow;
			}
			followSymbolStr = DigestUtils.md5Hex(followSymbolStr);
		}
		String keyName = "stock24h_news_home" + (pagingCache ? ("_" + timestamp) : "") + (followSymbolStr.length() > 0 ? ("_" + followSymbolStr) : "");
		JsonArray cached = Cache.getJsonArray(keyName);
		if (cached != null) return cached;
		logger.logInfo("Get from ES");
		JsonArray homeNewsArr;
		if (followSymbols == null) {
			homeNewsArr = articleService.getListHomeArticle(tableName, timestamp, 20);
		} else {
			homeNewsArr = articleService.getListHomeArticle(followSymbols, tableName, timestamp, 20);
		}
		articleArr = buildJson(homeNewsArr);
		JsonObject newsBox = new BoxHelper("", articleArr).toJson();
		JsonArray boxArray = new JsonArray();
		boxArray.add(newsBox);
		Cache.set(keyName, Cache.TIME_NEWS_HOME, boxArray);
		return boxArray;
	}
}

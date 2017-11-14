package com.finance24h.api.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

public interface ArticleService {

	JsonObject getArticleDetail(int articleId);
	String getProviderByCrawlId(int crawlId);
	JsonObject getProviders(Map<Integer, Integer> crawlIds);
	JsonArray getListHomeArticle(String tableName, long timestamp, int limit);
	JsonArray getListArticle(String tableName, long timestamp);
	JsonArray getListHomeArticle(List<String> followSymbols, String tableName, long timestamp, int limit);
	JsonArray getRelatedArticlesByStock(String stockCode, int limit, String tableName, long timestamp);
	JsonArray getRelatedArticlesByTags(int currentId, JsonArray tags, int limit);
}

package com.finance24h.api.service;

import com.google.gson.JsonArray;

import java.util.List;

public interface NewsService {
	JsonArray getNewestHomeArticles(String tableName, long timestamp, boolean allowCache);
	JsonArray getListArticles(String tableName, long timestamp, boolean pagingCache);
	JsonArray getNewestHomeArticles(int deviceId, List<String> followSymbols, String tableName, long timestamp, boolean allowCache);
}

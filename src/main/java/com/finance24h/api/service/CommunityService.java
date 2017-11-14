package com.finance24h.api.service;

import com.google.gson.JsonArray;

public interface CommunityService {
	JsonArray getListArticles(String tableName, long timestamp, boolean pagingCache);
}

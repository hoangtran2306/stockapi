package com.finance24h.api.service;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.finance24h.api.helpers.BoxHelper;
import com.finance24h.api.helpers.Cache;
import com.finance24h.api.helpers.LogStashHelper;
import com.finance24h.api.helpers.Utilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.searchbox.client.JestClient;

@Service("communityService")
public class CommunityServiceImpl implements CommunityService {
	
	private String communityTable;
	
	private String providerTable;
	
	@Autowired
	private LogStashHelper logger;
	
	@Autowired
	JestClient esClient;
	
	private final JsonParser jsonParser = new JsonParser();
	Map<String, String> providerMap = new HashMap<>();
	
	String[] providerList;
	
	public CommunityServiceImpl(@Value("${community.table}") String communityTable, @Value("${provider.table}") String providerTable, @Value("${community.providers}") String communityProviders) {
		super();
		this.communityTable = communityTable;
		this.providerTable = providerTable;
		providerList = communityProviders.split(",");
	}

	@Override
	public JsonArray getListArticles(String tableName, long timestamp, boolean pagingCache) {
		logger.logInfo("Get posts for community list");
		String keyName = "stock24h_community_posts_list" + (pagingCache ? ("_" + timestamp) : "");
		JsonArray cached = Cache.getJsonArray(keyName);
		if (cached != null) return cached;
		logger.logInfo("Get from ES");
		JsonArray articleArr;
		JsonArray listNewsArr = getListArticle(tableName, timestamp);
		articleArr = buildJson(listNewsArr);
		JsonObject newsBox = new BoxHelper("", articleArr).toJson();
		JsonArray boxArray = new JsonArray();
		boxArray.add(newsBox);
		Cache.set(keyName, Cache.TIME_COMMUNITY_POSTS, boxArray);
		return boxArray;
	}
	
	public JsonArray getListArticle(String tableName, long timestamp) {
		HashMap<String, String> sortHash = new HashMap<>();
		String timeField = Utilities.timeFieldByTable().get(tableName);
		BoolQueryBuilder boolQuery = new BoolQueryBuilder();
		boolQuery.must(QueryBuilders.rangeQuery(timeField).lt(timestamp));
		for (String provider : providerList) {				
			boolQuery.should(QueryBuilders.matchQuery("provider", provider));
		}
		boolQuery.minimumShouldMatch(1);
		sortHash.put(timeField, "desc");
		JsonObject fullQuery = Utilities.buildFullQuery(boolQuery, sortHash, 20);
		logger.logInfo("fullQuery =" + fullQuery);
		JsonArray result = Utilities.getEsResultFixedTable(esClient, fullQuery, communityTable);
		return result;
	}
	
	private JsonArray buildJson(JsonArray newsJsonArray) {
		JsonArray articleArr = new JsonArray();
		for(JsonElement oneArticle : newsJsonArray) {
			JsonObject oneArticleJson = oneArticle.getAsJsonObject();
			JsonObject articleJson = new JsonObject();
			articleJson.addProperty("id", oneArticleJson.get("id").getAsInt());
			articleJson.addProperty("page_id", oneArticleJson.get("sns_page_id").getAsString());
			articleJson.addProperty("post_id", oneArticleJson.get("sns_post_id").getAsString());
			articleJson.addProperty("title", oneArticleJson.get("content").getAsString());
			articleJson.addProperty("type", oneArticleJson.get("type").getAsString());
			try {
				String photosString = oneArticleJson.get("image_url").getAsString();
				JsonArray photos = jsonParser.parse(photosString).getAsJsonArray();
				articleJson.add("photo", photos);
				articleJson.addProperty("summary_image_url", photos.size() > 0 ? photos.get(0).getAsString() : "");
			} catch (Exception e) {
				articleJson.add("photo", null);
				articleJson.add("summary_image_url", null);
			}
			articleJson.addProperty("provider", getProvider(oneArticleJson.get("provider").getAsString()));
			articleJson.addProperty("published_date", oneArticleJson.get("sns_created_at").getAsLong());
			try {
				String shareInfo = oneArticleJson.get("share_info").getAsString();
				articleJson.add("share_info", jsonParser.parse(shareInfo).getAsJsonObject());
			} catch (Exception e) {
				articleJson.add("share_info", null);
			}
			articleJson.addProperty("likes", oneArticleJson.get("likes").getAsInt());
			articleJson.addProperty("shares", oneArticleJson.get("shares").getAsInt());
			articleJson.addProperty("comments", oneArticleJson.get("comments").getAsInt());
			articleArr.add(articleJson);
		}
		return articleArr;
	}
	
	private String getProvider(String code) {
		if (!providerMap.containsKey(code)) {
			BoolQueryBuilder boolQuery = new BoolQueryBuilder();
			for (String provider : providerList) {				
				boolQuery.should(QueryBuilders.matchQuery("provider", provider));
			}
			boolQuery.minimumShouldMatch(1);
			JsonObject fullQuery = Utilities.buildFullQuery(boolQuery, 20);
			System.out.println("fullQuery =" + fullQuery);
			JsonArray providers = Utilities.getEsResultFixedTable(esClient, fullQuery, providerTable);
			for (int i=0; i< providers.size(); i++) {
				JsonObject tmp = providers.get(i).getAsJsonObject();
				providerMap.put(tmp.get("provider").getAsString(), tmp.get("provider_name").getAsString());
			}
		}
		return providerMap.get(code);
	}

}

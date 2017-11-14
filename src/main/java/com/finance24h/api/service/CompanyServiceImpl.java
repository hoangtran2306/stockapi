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

@Service("companyService")
public class CompanyServiceImpl implements CompanyService {
	
	private static final String INDEX_COMPANY = "vndc_companies";
	
	private static final String  CACHED_COMPANY_KEY = "stock24h__company";

	long lastTimestamp = 0;
	final JsonParser jsonParser = new JsonParser();
	@Autowired
    LogStashHelper log;
	@Autowired
	JestClient esClient;
	
	@Override
	public int getShareIdByTag(String tag) {
		String floor = getFloor(tag);
		int shareId = getShareIdByTagAndFloor(tag, floor);
		return shareId;
	}
	
	private String getFloor(String tag) {
		String floor = null;
		try {
			BoolQueryBuilder boolQuery = new BoolQueryBuilder();
			boolQuery.must(QueryBuilders.termQuery("symbol", tag));
			JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
			JsonObject result = Utilities.getOneEsResult(esClient, fullQuery, INDEX_COMPANY);
			if (result != null) {
				floor = result.get("floor").getAsString();
			}
		} catch (Exception e) {
			log.logError("Error getFloor method:" + e);
			throw e;
		}
		return floor;
	}

	@Override
	public int getShareIdByTagAndFloor(String tag, String floor) {
		int shareId = 0;
		try {
			BoolQueryBuilder boolQuery = new BoolQueryBuilder();
			boolQuery.must(QueryBuilders.termQuery("code", tag));
			HashMap<String, String> sortMap = new HashMap<>();
			sortMap.put("id", "desc");
			JsonObject fullQuery = Utilities.buildFullQuery(boolQuery, sortMap, 1);
			JsonObject result = Utilities.getOneEsResult(esClient, fullQuery, ("vndc_" + floor.toLowerCase()));
			if (result != null) {
				shareId = result.get("id").getAsInt();
			}
		} catch (Exception e) {
			log.logError("Error getShareIdByTagAndFloor method:" + e);
			throw e;
		}
		return shareId;
	}

	@Override
	public JsonArray getAllCompanies() {
		String keyName = CACHED_COMPANY_KEY;
        JsonArray cached = Cache.getJsonArray(keyName);
        if (cached != null) return cached;
		
		BoolQueryBuilder boolQuery = new BoolQueryBuilder();
		HashMap<String, String> sortHash = new HashMap<String, String>();
		sortHash.put("id", "asc");

		JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
		fullQuery.addProperty("size", Utilities.maxLimit);
		fullQuery.add("sort", Utilities.buildSort(sortHash));
		
		log.logInfo("getAllCompanies query: " + fullQuery.toString());
		JsonArray arrCompanies = new JsonArray();
		JsonArray result = Utilities.getEsResult(esClient, fullQuery, INDEX_COMPANY);
		for (JsonElement pa : result) {
			int id = pa.getAsJsonObject().get("id").getAsInt();
			String symbol = pa.getAsJsonObject().get("symbol").getAsString();
			String companyNm = pa.getAsJsonObject().get("company").getAsString().toUpperCase();
			int active = pa.getAsJsonObject().get("active").getAsInt();

			JsonObject newArticle = new JsonObject();
			newArticle.addProperty("id", id);
			newArticle.addProperty("symbol", symbol);
			newArticle.addProperty("company_name", companyNm);
			newArticle.addProperty("active", active);

			arrCompanies.add(newArticle);
		}
		Cache.set(keyName, Cache.TIME_ALL_COMPANY, arrCompanies);
		return arrCompanies;
	}
	
	public JsonArray getCompaniesBySymbols(String[] symbols) {
		log.logInfo("getCompaniesBySymbols symbols: " + new Gson().toJson(symbols));

		BoolQueryBuilder boolQuery = new BoolQueryBuilder();
		boolQuery.must(QueryBuilders.termQuery("active", 1));
		boolQuery.must(QueryBuilders.termsQuery("symbol.keyword", symbols));
		JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
		
		log.logInfo("getCompaniesBySymbols query: " + fullQuery.toString());
		JsonArray result = Utilities.getEsResult(esClient, fullQuery, INDEX_COMPANY);

		return result;
	 
	}
	

}

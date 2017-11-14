package com.finance24h.api.service;

import com.finance24h.api.helpers.Utilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("deviceFollowsService")
public class DeviceFollowServiceImpl implements DeviceFollowService {
	
	private static final String INDEX_DEVICE_FOLLOWS = "device_follows";

	
	@Autowired
	JestClient esClient;
	
	@Override
	public JsonArray getAllDeviceFollow(int deviceId) {
		BoolQueryBuilder boolQuery = new BoolQueryBuilder();
		boolQuery.must(QueryBuilders.termQuery("device_id", deviceId));
		JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
		fullQuery.addProperty("size", Utilities.maxLimit);
		return Utilities.getEsResult(esClient, fullQuery, INDEX_DEVICE_FOLLOWS);
	}
	
	
	@Override
	public int getDeviceFollowBySymbol(int device_id, String symbol) {
		JsonArray allProvider = getAllDeviceFollow(device_id);
		
		JsonObject finalJson = new JsonObject();
		for (JsonElement oneResultJson : allProvider) {
			String symbol_tmp = oneResultJson.getAsJsonObject().get("symbol").getAsString().toUpperCase();
			
			finalJson.addProperty(symbol_tmp, symbol_tmp);
		}
		
		return getDeviceFollowBySymbol( finalJson,  symbol);
	}

	@Override
	public int getDeviceFollowBySymbol(JsonObject listDeviceFollows, String symbol) {
		JsonElement providerElement = listDeviceFollows.get(String.valueOf(symbol));
		if (providerElement != null) {
			return 1;
		}
		return 0;
	}

	@Override
	public List<String> getAllFollowSymbol(int deviceId) {
		List<String> symbols = new ArrayList<>();
		for(JsonElement oneFollow : getAllDeviceFollow(deviceId)) {
			symbols.add(oneFollow.getAsJsonObject().get("symbol").getAsString());
		}
		return symbols;
	}
}

package com.finance24h.api.service;

import com.finance24h.api.helpers.Cache;
import com.finance24h.api.helpers.LogStashHelper;
import com.finance24h.api.helpers.Utilities;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.searchbox.client.JestClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("deviceService")
public class DeviceServiceImpl implements DeviceService {
	
	private static final String INDEX_DEVICE = "devices";

	private static final String  CACHED_DEVICE_KEY_PREFIX = "stock24h__device_";
	@Autowired
    LogStashHelper log;

	@Autowired
	JestClient esClient;

	final JsonParser jsonParser = new JsonParser();

	@Override
	public JsonObject getDevicesByDeviceID(String deviceId) {
		String keyName = CACHED_DEVICE_KEY_PREFIX + String.valueOf(deviceId);
		JsonObject cached = Cache.getJsonObject(keyName);
		if (cached != null) return cached;
		//If have no cached . Get data from ES
		try {
			BoolQueryBuilder boolQuery = new BoolQueryBuilder();
			boolQuery.must(QueryBuilders.termQuery("device_id.keyword", deviceId));
			JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
			JsonObject result = Utilities.getOneEsResult(esClient, fullQuery, INDEX_DEVICE);
			Cache.set(keyName, Cache.TIME_DEVICE, result);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			log.logError("Error getDevicesByDeviceID method #67:" + e.toString());
			throw e;
		}
	}
	
}

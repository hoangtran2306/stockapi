package com.finance24h.api.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class Utilities {
	
	public static final int maxLimit = 5000;
	private static final int defaultLimit = 10;

	public static String tablePrefix;
	public static ResourceBundleMessageSource messageSource;
	public static Locale locate;
	public static String timeZone;
	
	public static long getCurrentTimestamp() {
		return getCurrentTimestampMilis() / 1000;
	}
	
	public static long getCurrentTimestampMilis() {
		return (Calendar.getInstance(TimeZone.getTimeZone(timeZone)).getTimeInMillis());
	}

	public static JsonArray getEsResultFixedTable(JestClient esClient, JsonObject fullQuery, String endPoint) {
		JsonArray resultJsonArray;
		try {
			Search search = new Search.Builder(fullQuery.toString()).addIndex(endPoint).addType(endPoint).build();
			SearchResult result = esClient.execute(search);
			resultJsonArray = getListESResult(result.getJsonObject()).get("result");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return resultJsonArray;
	}

	public static JsonArray getEsResult(JestClient esClient, JsonObject fullQuery, String tableName) {
		String endPoint = tablePrefix + tableName;
		return getEsResultFixedTable(esClient, fullQuery, endPoint);
	}
	
	public static JsonObject getOneEsResult (JestClient esClient, JsonObject fullQuery, String tableName) {
		JsonArray resultJsonArray = getEsResult(esClient, fullQuery, tableName);
		if (resultJsonArray.size() > 0) {
			return resultJsonArray.get(0).getAsJsonObject();
		}
		return null;
	}

	public static Map<String, JsonArray> getEsResultWithAggs (JestClient esClient, JsonObject fullQuery, String tableName) {
		Map<String, JsonArray> resultMap = new HashMap<>();
		JsonArray resultJsonArray = new JsonArray();
		JsonArray aggsJsonArray = null;
		try {
			String endPoint = tablePrefix + tableName;
			Search search = new Search.Builder(fullQuery.toString()).addIndex(endPoint).addType(endPoint).build();
			SearchResult result = esClient.execute(search);
			Map<String, JsonArray> listMap = getListESResult(result.getJsonObject());
			resultJsonArray = listMap.get("result");
			aggsJsonArray = listMap.get("aggs");
		} catch (Exception e) {
			e.printStackTrace();
		}
		resultMap.put("result", resultJsonArray);
		resultMap.put("aggs", aggsJsonArray);
		return resultMap;
	}
	
	public static JsonObject getAggEsResult (JestClient esClient, JsonObject fullQuery, String tableName) {
		try {
			String endPoint = tablePrefix + tableName;
			Search search = new Search.Builder(fullQuery.toString()).addIndex(endPoint).addType(endPoint).build();
			SearchResult result = esClient.execute(search);
			return result.getJsonObject().get("aggregations").getAsJsonObject();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String convertArticleType(String figureType) {
		switch (figureType) {
			case "img": 
				return "photo";
			case "video" : 
				return "video";
			default : 
				return "normal";
		}
	}
	
	private static Map<String, JsonArray> getListESResult(JsonObject esResponse) {
		Map<String, JsonArray> result = new HashMap<>();
		JsonArray listResult = new JsonArray();
		JsonObject hitsObject = esResponse.get("hits").getAsJsonObject();
		if (hitsObject != null) {
			JsonArray hitsArray = hitsObject.getAsJsonArray("hits");
			for (JsonElement oneHit : hitsArray) {
				JsonObject source = oneHit.getAsJsonObject().getAsJsonObject("_source");
				listResult.add(source);
			}
		}
		JsonElement aggsElement = esResponse.get("aggregations");
		JsonObject aggs = aggsElement == null ? null : aggsElement.getAsJsonObject();
		JsonArray aggsArr = new JsonArray();
		aggsArr.add(aggs);
		result.put("result", listResult);
		result.put("aggs", aggsArr);
		return result;
	}

	public static JsonArray buildSort(HashMap<String, String> sortMap) {
		JsonArray sort = new JsonArray();
		for (Map.Entry<String, String> entry : sortMap.entrySet()) {
			JsonObject oneSort = new JsonObject();
			oneSort.addProperty(entry.getKey(), entry.getValue());
			sort.add(oneSort);
		}
		return sort;
	}
	
	public static String getLabel(String code, String language) {
		return messageSource.getMessage(code, null, new Locale(language));
	}
	
	public static String getLabel(String code) {
		String text = messageSource.getMessage(code, null, locate );
		String result;
		try {
			String utf8 = new String(text.getBytes("ISO-8859-1"), "UTF-8");
			result = utf8;
		} catch (UnsupportedEncodingException e) {
			result = "";
		}
		return result;
	}
	
	/** 
	 * Elasticsearch Query Builder Utility
	 * */
	/**
	 * 
	 * @param boolQuery
	 * @param sortMap
	 * @param limit
	 * @param offset
	 * @return JsonObject
	 */
	public static JsonObject buildFullQuery(BoolQueryBuilder boolQuery, HashMap<String, String> sortMap, int limit, int offset) {
		JsonParser jParser = new JsonParser();

		JsonObject fullQuery = new JsonObject();
		if (boolQuery != null) {
			JsonObject boolQueryJson = jParser.parse(boolQuery.toString()).getAsJsonObject();
			fullQuery.add("query", boolQueryJson);
		}
		fullQuery.addProperty("from", offset > 0 ? offset : 0);
		limit = limit > maxLimit ? maxLimit : limit;
		fullQuery.addProperty("size", limit > 0 ? limit : defaultLimit);
		if (sortMap != null) {
			fullQuery.add("sort", buildSort(sortMap));
		}
		return fullQuery;
	}
	
	/**
	 * 
	 * @param boolQuery
	 * @param sortMap
	 * @param limit
	 * @return JsonObject
	 */
	public static JsonObject buildFullQuery(BoolQueryBuilder boolQuery, HashMap<String, String> sortMap, int limit) {
		return buildFullQuery(boolQuery, sortMap, limit, -1);
	}
	
	/**
	 * 
	 * @param boolQuery
	 * @param limit
	 * @param offset
	 * @return
	 */
	public static JsonObject buildFullQuery(BoolQueryBuilder boolQuery, int limit, int offset) {
		return buildFullQuery(boolQuery, null, limit, offset);
	}
	
	/**
	 * 
	 * @param sortMap
	 * @param limit
	 * @param offset
	 * @return JsonObject
	 */
	public static JsonObject buildFullQuery(HashMap<String, String> sortMap, int limit, int offset) {
		return buildFullQuery(null, sortMap, limit, offset);
	}
	
	/**
	 * 
	 * @param boolQuery
	 * @param limit
	 * @return JsonObject
	 */
	public static JsonObject buildFullQuery(BoolQueryBuilder boolQuery, int limit) {
		return buildFullQuery(boolQuery, limit, -1);
	}
	
	/**
	 * 
	 * @param boolQuery
	 * @return JsonObject
	 */
	public static JsonObject buildFullQuery(BoolQueryBuilder boolQuery) {
		return buildFullQuery(boolQuery, -1);
	}

	/**
	 *
	 * @param sortMap
	 * @return
	 */
	public static JsonObject buildFullQuery(Map<String, String> sortMap) {
		HashMap<String, String> _sortMap = new HashMap<>(sortMap);
		return buildFullQuery(null, _sortMap, -1, -1);
	}
	
	public static HashMap<String, String> timeFieldByTable() {
		HashMap<String, String> hash = new HashMap<>();
		hash.put("articles", "published_at");
		hash.put("posts", "sns_created_at");
		return hash;
	}

	public static String getCurrentDateTime() {
		return ZonedDateTime.now().format( DateTimeFormatter.ISO_INSTANT);
	}

	public static String numberFormat(Object value) {
		try {
			NumberFormat roundFix = new DecimalFormat("#0.00");
			String roundFixString = roundFix.format(Double.valueOf(value.toString()));
			NumberFormat numberFormat = NumberFormat.getInstance();
			return numberFormat.format(Double.valueOf(roundFixString));
		} catch (NumberFormatException e) {
			return "";
		}
	}
}

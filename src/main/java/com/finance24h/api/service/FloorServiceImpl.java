package com.finance24h.api.service;

import com.finance24h.api.helpers.BoxHelper;
import com.finance24h.api.helpers.Cache;
import com.finance24h.api.helpers.LogStashHelper;
import com.finance24h.api.helpers.Utilities;
import com.finance24h.api.model.FloorDetailDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by ait on 10/3/17.
 */
@Service("floorService")
public class FloorServiceImpl implements FloorService {

    @Autowired
    private JestClient esClient;
    @Autowired
    private LogStashHelper logger;

    @Override
    public Map<String, String> getAllIndex() {
        Map<String, String> allIndexName = new LinkedHashMap<>();
        Map<String, String> sortMap = new HashMap<>();
        sortMap.put("order", "asc");
        JsonObject fullQuery = Utilities.buildFullQuery(sortMap);
        JsonArray resultArray = Utilities.getEsResult(esClient, fullQuery, "vndc_floors");
        for (JsonElement oneResultObject : resultArray) {
            String code = oneResultObject.getAsJsonObject().get("code").getAsString();
            String index = oneResultObject.getAsJsonObject().get("index").getAsString();
            allIndexName.put(code, index);
        }
        return allIndexName;
    }

    @Override
    public JsonObject getFloorInfoHeader() {
        logger.logInfo("get top_shares_group");
        String keyName = "stock24h_floor_header";
        JsonObject cached = Cache.getJsonObject(keyName);
        if (cached != null) return cached;
        logger.logInfo("get from ES");
        Map<String, String> allIndexName = getAllIndex();
        JsonArray boxArray = new JsonArray();
        for (Map.Entry<String, String> oneIndex : allIndexName.entrySet()) {
            String floorCode = oneIndex.getKey();
            Map<String, Float> newestValue = getFloorNewestValues(floorCode);
            JsonObject boxElement = new JsonObject();
            boxElement.addProperty("share_id", floorCode);
            boxElement.addProperty("symbol", oneIndex.getValue());
            boxElement.addProperty("company_name", oneIndex.getValue());
            boxElement.addProperty("price", newestValue.get("market_index"));
            boxElement.addProperty("change", newestValue.get("market_percentage"));
            boxArray.add(boxElement);
        }
        JsonObject boxObject = new BoxHelper(boxArray).toJson();
        Cache.set(keyName, Cache.TIME_FLOOR_HEADER, boxObject);
        return boxObject;
    }

    @Override
    public JsonObject getFloorDetail(String floorCode) {
        logger.logInfo("get floor key_statistics_group");
        String keyName = "stock24h_floor_statistic_" + floorCode;
        JsonObject cached = Cache.getJsonObject(keyName);
        if (cached != null) return cached;
        logger.logInfo("get from ES");
        JsonObject groupObject = new JsonObject();
        JsonObject newestObject = getFloorByCode(floorCode);
        FloorDetailDTO floorDetailDTO = new FloorDetailDTO();
        // share logInfo
        floorDetailDTO.setFloorCode(floorCode);
        floorDetailDTO.setIndex(getIndexByCode(floorCode));
        floorDetailDTO.setMarketIndex(newestObject.get("market_index").getAsFloat());
        floorDetailDTO.setPriorMarketIndex(newestObject.get("prior_market_index").getAsFloat());

        // statistic
        float lowestIndex = newestObject.get("lowest_index").getAsFloat();
        float highestIndex = newestObject.get("highest_index").getAsFloat();
        floorDetailDTO.setDayMinValue(lowestIndex);
        floorDetailDTO.setDayMaxValue(highestIndex);
        floorDetailDTO.setDayCurrentValue(newestObject.get("market_index").getAsFloat());

        Map<String, Float> annualInfo = getAnnualInfo(floorCode);
        floorDetailDTO.setWeekMinValue(annualInfo.get("week_min"));
        floorDetailDTO.setWeekMaxValue(annualInfo.get("week_max"));
        floorDetailDTO.setWeekCurrentValue(annualInfo.get("week_current"));

        floorDetailDTO.setIndexMinValue(lowestIndex);
        floorDetailDTO.setIndexMaxValue(highestIndex);
        floorDetailDTO.setAdvanceValue(newestObject.get("advance").getAsInt());
        floorDetailDTO.setDeclineValue(newestObject.get("decline").getAsInt());
        floorDetailDTO.setNoChangeValue(newestObject.get("no_change").getAsInt());
        floorDetailDTO.setTotalValueTraded(newestObject.get("total_value_traded").getAsFloat());
        floorDetailDTO.setTotalShareTraded(newestObject.get("total_share_traded").getAsFloat());
        floorDetailDTO.setTradingDate(newestObject.get("trading_date").getAsLong() / 1000);
        floorDetailDTO.setTradingTime(newestObject.get("trading_time").getAsString());
        JsonArray shareDetailArray = floorDetailDTO.getShareDetail();//
        JsonArray statisticArray = floorDetailDTO.getStatistic();
        groupObject.add("share_detail", shareDetailArray);
        groupObject.add("statistic", statisticArray);
        Cache.set(keyName, Cache.TIME_FLOOR_STATISTICS, groupObject);
        return groupObject;
    }

    private Map<String, Float> getFloorNewestValues(String floorCode) {
        Map<String, Float> values = new HashMap<>();
        JsonObject newestObject = getFloorByCode(floorCode);
        float marketIndex = 0;
        float marketPercentage = 0;
        if (newestObject != null) {
            marketIndex = newestObject.get("market_index").getAsFloat();
            float priorMarkerIndex = newestObject.get("prior_market_index").getAsFloat();
            marketPercentage = priorMarkerIndex == 0 ? 0 : ((marketIndex - priorMarkerIndex) * 100 / priorMarkerIndex);
        }
        values.put("market_index", marketIndex);
        values.put("market_percentage", marketPercentage);
        return values;
    }

    @Override
    public JsonObject getFloorGraph(String floorCode, int type) {
        floorCode = floorCode.toLowerCase();
        String keyName = "stock24h__graph_" + floorCode + "_" + type;
        JsonObject cached = Cache.getJsonObject(keyName);
        if (cached != null) return cached;
        String table = "vndc_market_daily";
        String timeField = "time";
        int cachedTime = Cache.TIME_GRAPH_DAILY;
        Calendar from = Calendar.getInstance();
        JsonArray xaxis = new JsonArray();
        switch (type) {
            case 1:
	            	if (from.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
	        			from.add(Calendar.DATE, -2);
	        		} else if (from.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
	        			from.add(Calendar.DATE, -1);
	        		}
                from.set(Calendar.HOUR_OF_DAY, 9);
                from.set(Calendar.MINUTE, 0);
                from.set(Calendar.SECOND, 0);
                long root = from.getTimeInMillis() / 1000;
                for (int i = 0; i < 7; i++) {
                    xaxis.add(root + i * 3600);
                }
                break;
            case 2:
                from.add(Calendar.DAY_OF_YEAR, -6);
                from.set(Calendar.HOUR_OF_DAY, 0);
                from.set(Calendar.MINUTE, 0);
                int tmp = from.get(Calendar.DAY_OF_WEEK);
                long root2 = from.getTimeInMillis() / 1000;
                for (int i = 0; i < 7; i++) {
                    int z = (tmp + i) % 7;
                    if (z > 1) {
                        xaxis.add(root2 + i * 86400);
                    }
                }
                break;
            case 3:
                table = "vndc_market_monthly";
                timeField = "trading_date";
                cachedTime = Cache.TIME_GRAPH_MONTHLY;
                from.add(Calendar.MONTH, -3);
                long root3 = from.getTimeInMillis() / 1000;
                xaxis.add(root3);
                xaxis.add(root3 + 2592000);
                xaxis.add(root3 + 2592000 * 2);
                break;
            case 4:
                table = "vndc_market_monthly";
                timeField = "trading_date";
                cachedTime = Cache.TIME_GRAPH_MONTHLY;
                from.add(Calendar.MONTH, -6);
                long root4 = from.getTimeInMillis() / 1000;
                xaxis.add(root4);
                xaxis.add(root4 + 2592000 * 2);
                xaxis.add(root4 + 2592000 * 4);
                break;
            case 5:
                table = "vndc_market_monthly";
                timeField = "trading_date";
                cachedTime = Cache.TIME_GRAPH_MONTHLY;
                from.add(Calendar.MONTH, -12);
                long root5 = from.getTimeInMillis() / 1000;
                xaxis.add(root5);
                xaxis.add(root5 + 2592000 * 3);
                xaxis.add(root5 + 2592000 * 6);
                xaxis.add(root5 + 2592000 * 9);
                break;
            case 6:
                table = "vndc_market_annual";
                timeField = "from_date";
                cachedTime = Cache.TIME_GRAPH_YEARLY;
                from.add(Calendar.YEAR, -5);
                long root6 = from.getTimeInMillis() / 1000;
                xaxis.add(root6);
                xaxis.add(root6 + 31536000);
                xaxis.add(root6 + 31536000 * 2);
                xaxis.add(root6 + 31536000 * 3);
                xaxis.add(root6 + 31536000 * 4);
                break;
            default:
                break;
        }
        HashMap<String, String> sortHash = new HashMap<>();
        sortHash.put("id", "desc");
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(QueryBuilders.termQuery("code", floorCode));
        boolQuery.must(QueryBuilders.rangeQuery(timeField).gte(from.getTimeInMillis() / 1000));
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
        fullQuery.addProperty("size", 600);
        fullQuery.add("sort", Utilities.buildSort(sortHash));
        System.out.println(fullQuery);
        JsonArray points = Utilities.getEsResult(esClient, fullQuery, table);
        JsonObject result = new JsonObject();
        JsonArray chartPoint = new JsonArray();
        JsonArray yaxis = new JsonArray();
        JsonArray zaxis = new JsonArray();
        if (points != null) {
            float min = Float.MAX_VALUE, max = 0, tmpPrice;
            long zmin = 0, zmax = 0, tmpQtty;
            for (int i = points.size() - 1; i >= 0; --i) {
                JsonObject src = points.get(i).getAsJsonObject();
                JsonObject tmp = new JsonObject();
                tmp.addProperty("x", src.get(timeField).getAsInt());
                tmpPrice = src.get("index").getAsFloat();
                tmpQtty = src.get("share").getAsLong();
                if (tmpPrice > 0.0) {
                    tmp.addProperty("y", tmpPrice);
                    tmp.addProperty("z", tmpQtty);
                    min = Math.min(min, tmpPrice);
                    max = Math.max(max, tmpPrice);
                    zmin = Math.min(zmin, tmpQtty);
                    zmax = Math.max(zmax, tmpQtty);
                    chartPoint.add(tmp);
                }
            }
            result.add("chart_points", chartPoint);
            result.add("x-axis", xaxis);
            if (max > 0) {
	            float middle = max - min;
	            if (middle < 0.1) middle = 1;
	            min = Math.max(0, min - middle);
	            max = max + middle / 2;
	            middle = (min + max) / 2;
	            yaxis.add(min);
	            yaxis.add((min + middle) / 2);
	            yaxis.add(middle);
	            yaxis.add((middle + max) / 2);
	            yaxis.add(max);
            } else {
            		yaxis.add(0);
            		yaxis.add(0);
            		yaxis.add(0);
            		yaxis.add(0);
            		yaxis.add(0);
            }
            if (zmax > 0) {
	            long zmiddle = zmax - zmin;
//	            zmin = Math.max(0, zmin - zmiddle / 2);
//	            zmax = zmax + zmiddle / 3;
	            zmin = 0;
	            long t = (long) Math.pow(10, String.valueOf(zmax).length()-1);
	            zmax = ((zmax/t)+1) * t;
	            zmiddle = (zmin + zmax) / 2;
	            zaxis.add(zmin);
	            zaxis.add((zmin + zmiddle) / 2);
	            zaxis.add(zmiddle);
	            zaxis.add((zmiddle + zmax) / 2);
	            zaxis.add(zmax);
            } else {
            		zaxis.add(0);
            		zaxis.add(0);
            		zaxis.add(0);
            		zaxis.add(0);
            		zaxis.add(0);
            }
            result.add("y-axis", yaxis);
            result.add("z-axis", zaxis);
            Cache.set(keyName, cachedTime, result);
        }
        return result;
    }

    private Map<String, Float> getAnnualInfo(String floorCode) {
        Map<String, Float> annualInfo = new HashMap<>();
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(QueryBuilders.termQuery("code", floorCode));
        Calendar from = Calendar.getInstance();
        from.add(Calendar.MONTH, -12);
        boolQuery.must(QueryBuilders.rangeQuery("from_date").gte(from.getTimeInMillis() / 1000));
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery, 1);
        JsonObject aggQuery = new JsonObject();
        aggQuery.add("max_index", getAgg("max", "index"));
        aggQuery.add("min_index", getAgg("min", "index"));
        fullQuery.add("aggs", aggQuery);
        Map<String, JsonArray> annualMap = Utilities.getEsResultWithAggs(esClient, fullQuery, "vndc_market_annual");
        JsonArray resultArray = annualMap.get("result");
        float weekMin = 0, weekMax = 0, weekCurrent = 0;
        if (resultArray.size() > 0) {
            JsonObject oneAnnualObject = resultArray.get(0).getAsJsonObject();
            JsonObject aggsObject = annualMap.get("aggs").get(0).getAsJsonObject();
            weekMin = aggsObject.get("min_index").getAsJsonObject().get("value").getAsFloat();
            weekMax = aggsObject.get("max_index").getAsJsonObject().get("value").getAsFloat();
            weekCurrent = oneAnnualObject.get("index").getAsFloat();
        }
        annualInfo.put("week_min", weekMin);
        annualInfo.put("week_max", weekMax);
        annualInfo.put("week_current", weekCurrent);
        return annualInfo;
    }

    private JsonObject getAgg(String aggType, String fieldName) {
        JsonObject aggQuery = new JsonObject();
        JsonObject tmpQuery = new JsonObject();
        tmpQuery.addProperty("field", fieldName);
        aggQuery.add(aggType, tmpQuery);
        return aggQuery;
    }

    @Override
    public JsonArray getFloorShareDetail(String floorCode, JsonObject graph) {
        JsonObject newestObject = getFloorByCode(floorCode);
        FloorDetailDTO floorDetailDTO = new FloorDetailDTO();
        if (newestObject != null) {
            floorDetailDTO.setFloorCode(floorCode);
            floorDetailDTO.setIndex(getIndexByCode(floorCode));
            floorDetailDTO.setMarketIndex(newestObject.get("market_index").getAsFloat());
            floorDetailDTO.setPriorMarketIndex(newestObject.get("prior_market_index").getAsFloat());
            floorDetailDTO.setTradingDate(newestObject.get("trading_date").getAsLong() / 1000);
            floorDetailDTO.setTradingTime("");
        }
        return floorDetailDTO.getShareDetail(graph);
    }

    private JsonObject getFloorByCode(String floorCode) {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(QueryBuilders.termQuery("floor_code", floorCode));
        HashMap<String, String> sortMap = new HashMap<>();
        sortMap.put("id", "desc");
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery, sortMap, 1);
        return Utilities.getOneEsResult(esClient, fullQuery, "vndc_markets");
    }

    @Override
    public String getIndexByCode(String floorCode) {
        return getAllIndexName().get(floorCode).getAsString();
    }

    private JsonObject getAllIndexName() {
        logger.logInfo("Get all floor name");
        String keyName = "stock24h_all_floor_index";
        JsonObject cached = Cache.getJsonObject(keyName);
        if (cached != null) return cached;
        logger.logInfo("get from ES");
        JsonObject floorNameObject = new JsonObject();
        Map<String, String> sortMap = new HashMap<>();
        sortMap.put("id", "desc");
        JsonObject fullQuery = Utilities.buildFullQuery(sortMap);
        JsonArray resultArray = Utilities.getEsResult(esClient, fullQuery, "vndc_floors");
        if (resultArray == null) {
            return new JsonObject();
        }
        for (JsonElement oneFloor : resultArray) {
            String code = oneFloor.getAsJsonObject().get("code").getAsString();
            String index = oneFloor.getAsJsonObject().get("index").getAsString();
            floorNameObject.addProperty(code, index);
        }
        Cache.set(keyName, Cache.TIME_FLOOR_INDEX, floorNameObject);
        return floorNameObject;
    }
}

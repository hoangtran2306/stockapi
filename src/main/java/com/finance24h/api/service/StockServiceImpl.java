package com.finance24h.api.service;

import com.finance24h.api.helpers.Cache;
import com.finance24h.api.helpers.Utilities;
import com.finance24h.api.model.ShareDetailDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.searchbox.client.JestClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("stockService")
public class StockServiceImpl implements StockService {
    @Autowired
    private JestClient esClient;

    final JsonParser jsonParser = new JsonParser();
    private Map<String, String> floorCodeMap = new HashMap<>();

    @Override
    public synchronized ShareDetailDTO getStockDetails(String stockCode) {
        ShareDetailDTO dto = new ShareDetailDTO();
        stockCode = stockCode.toLowerCase();
        JsonObject result = getCompanyInfo(stockCode);
        if (result != null) {
            dto.setCompanyName(result.get("company_name").getAsString());
            dto.setFloor(result.get("floor").getAsString());
            dto.setSymbol(result.get("symbol").getAsString());
        }

        result = getScffDetail(stockCode);
        if (result != null) {
            dto.setPrice(result.get("match_price").getAsFloat());
            dto.setMarketCapacity(result.get("market_cap").getAsFloat());
            dto.setAccumulatedVolume(result.get("accumylated_vol").getAsLong());
            dto.setBeta(result.get("the_beta").getAsFloat());
            dto.setCirculationVolume(result.get("circulation_vol").getAsLong());
            dto.setEps(result.get("basic_eps").getAsFloat());
            dto.setPe(result.get("pe").getAsFloat());
            dto.setAverageVolume(result.get("avg_trading_vol").getAsLong());
            dto.setPrice(result.get("match_price").getAsFloat());
            dto.setOpenPrice(result.get("open_price").getAsFloat());
            dto.setLow(result.get("lowest_price").getAsFloat());
            dto.setHigh(result.get("highest_price").getAsFloat());
        }
        if (dto.getFloor() != null) {
            JsonArray resultArr = getVndcDetail(stockCode, dto.getFloor());
            if (resultArr != null && resultArr.size() > 0) {
                result = resultArr.get(0).getAsJsonObject();
                dto.setPrice(result.get("match_price").getAsFloat());
                dto.setOpenPrice(result.get("open_price").getAsFloat());
                dto.setBasicPrice(result.get("basic_price").getAsFloat());
                dto.setUpdated(result.get("trading_date").getAsLong() / 1000);
                dto.setMarketValue(result.get("match_value").getAsFloat());
                dto.setFloorPrice(result.get("floor_price").getAsFloat());
                dto.setCeilingPrice(result.get("ceiling_price").getAsFloat());
                dto.setLow(result.get("lowest_price").getAsFloat());
                dto.setHigh(result.get("hieghest_price").getAsFloat());
                dto.setVolume(result.get("match_qtty").getAsFloat());
                if (resultArr.size() > 1) {
                    result = resultArr.get(1).getAsJsonObject();
                    float prePrice = result.get("match_price").getAsFloat();
                    float preBasic = result.get("basic_price").getAsFloat();
                    if (preBasic > 0) dto.setPrePercentChange((prePrice - preBasic) / preBasic * 100);
                }
            }
        }
        result = getStatistics(stockCode);
        if (result != null) {
            try {
                dto.setYearLow(result.get("min_price").getAsJsonObject().get("value").getAsFloat());
                dto.setYearHigh(result.get("max_price").getAsJsonObject().get("value").getAsFloat());
            } catch (Exception e) {
            }
        }
        return dto;
    }

    private JsonObject getStatistics(String stockCode) {
        String keyName = "stock24h__year_statistics_" + stockCode;
        JsonObject cached = Cache.getJsonObject(keyName);
        if (cached != null) return cached;
        Calendar from = Calendar.getInstance();
        from.add(Calendar.MONTH, -12);
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(QueryBuilders.termQuery("code", stockCode));
        boolQuery.must(QueryBuilders.rangeQuery("from_date").gte(from.getTimeInMillis() / 1000));
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
        fullQuery.addProperty("size", 0);
        JsonObject aggQuery = new JsonObject();
        aggQuery.add("max_price", getAgg("max", "match_price"));
        aggQuery.add("min_price", getAgg("min", "match_price"));
        fullQuery.add("aggs", aggQuery);
        JsonObject result = Utilities.getAggEsResult(esClient, fullQuery, "vndc_stock_annual");
        Cache.set(keyName, Cache.TIME_STOCK_STATISTICS, result);
        return result;
    }

    private JsonElement getAgg(String aggType, String fieldName) {
        JsonObject aggQuery = new JsonObject();
        JsonObject tmpQuery = new JsonObject();
        tmpQuery.addProperty("field", fieldName);
        aggQuery.add(aggType, tmpQuery);
        return aggQuery;
    }

    private JsonObject getScffDetail(String stockCode) {
        String keyName = "stock24h__scff_detail_" + stockCode;
        JsonObject cached = Cache.getJsonObject(keyName);
        if (cached != null) return cached;
        HashMap<String, String> sortHash = new HashMap<String, String>();
        sortHash.put("id", "desc");
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(QueryBuilders.termQuery("symbol", stockCode));
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
        fullQuery.add("sort", Utilities.buildSort(sortHash));
        fullQuery.addProperty("size", 1);
        JsonObject result = Utilities.getOneEsResult(esClient, fullQuery, "scff_sprofiles");
        Cache.set(keyName, Cache.TIME_SCFF_DETAIL, result);
        return result;
    }

    private JsonObject getCompanyInfo(String stockCode) {
        String keyName = "stock24h__company_info_" + stockCode;
        JsonObject cached = Cache.getJsonObject(keyName);
        if (cached != null) return cached;
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(QueryBuilders.termQuery("symbol", stockCode));
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
        JsonObject result = Utilities.getOneEsResult(esClient, fullQuery, "vndc_companies");
        Cache.set(keyName, Cache.TIME_COMPANY_INFO, result);
        return result;
    }

    private JsonArray getVndcDetail(String stockCode, String floor) {
        String keyName = "stock24h__vndc_detail_" + stockCode;
        JsonArray cached = Cache.getJsonArray(keyName);
        if (cached != null) return cached;
        HashMap<String, String> sortHash = new HashMap<String, String>();
        sortHash.put("id", "desc");
        String table = "vndc_" + floor.toLowerCase();
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(QueryBuilders.termQuery("code", stockCode));
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
        fullQuery.add("sort", Utilities.buildSort(sortHash));
        fullQuery.addProperty("size", 2);
        JsonArray result = Utilities.getEsResult(esClient, fullQuery, table);
        Cache.set(keyName, Cache.TIME_VNDC_DETAIL, result);
        return result;
    }

    @Override
    public List<ShareDetailDTO> getStockDetails(String[] symbols) {
        List<ShareDetailDTO> list = new ArrayList<>();
        for (String symbol : symbols) {
            list.add(getStockDetails(symbol));
        }
        return list;
    }
    
    @Override
    public List<ShareDetailDTO> getStockDetails(String[] symbols, int limit) {
        List<ShareDetailDTO> list = new ArrayList<>();
        for (String symbol : symbols) {
            list.add(getStockDetails(symbol));
            if (list.size() == limit) return list;
        }
        return list;
    }

    @Override
    public JsonObject getStockGraph(String stockCode, int type) {
        stockCode = stockCode.toLowerCase();
        String keyName = "stock24h__graph_" + stockCode + "_" + type;
        JsonObject cached = Cache.getJsonObject(keyName);
        if (cached != null) return cached;
        JsonObject companyInfo = getCompanyInfo(stockCode);
        String floorCode = "";
        if (companyInfo != null) {
            String floor = companyInfo.get("floor").getAsString();
            floorCode = getFloorCode(floor);
        }
        String table = "vndc_stock_daily";
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
                table = "vndc_stock_monthly";
                timeField = "trading_date";
                cachedTime = Cache.TIME_GRAPH_MONTHLY;
                from.add(Calendar.MONTH, -3);
                long root3 = from.getTimeInMillis() / 1000;
                xaxis.add(root3);
                xaxis.add(root3 + 2592000);
                xaxis.add(root3 + 2592000 * 2);
                break;
            case 4:
                table = "vndc_stock_monthly";
                timeField = "trading_date";
                cachedTime = Cache.TIME_GRAPH_MONTHLY;
                from.add(Calendar.MONTH, -6);
                long root4 = from.getTimeInMillis() / 1000;
                xaxis.add(root4);
                xaxis.add(root4 + 2592000 * 2);
                xaxis.add(root4 + 2592000 * 4);
                break;
            case 5:
                table = "vndc_stock_monthly";
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
                table = "vndc_stock_annual";
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
        boolQuery.must(QueryBuilders.termQuery("code", stockCode));
        boolQuery.must(QueryBuilders.termQuery("floor", floorCode));
        boolQuery.must(QueryBuilders.rangeQuery(timeField).gte(from.getTimeInMillis() / 1000));
        JsonObject fullQuery = Utilities.buildFullQuery(boolQuery);
        fullQuery.addProperty("size", 600);
        fullQuery.add("sort", Utilities.buildSort(sortHash));
        JsonArray points = Utilities.getEsResult(esClient, fullQuery, table);
        JsonObject result = new JsonObject();
        JsonArray chartPoint = new JsonArray();
        JsonArray yaxis = new JsonArray();
        JsonArray zaxis = new JsonArray();
        if (points != null) {
            float min = Float.MAX_VALUE, max = 0, tmpPrice;
            long zmin = Integer.MAX_VALUE, zmax = 0, tmpQtty; 
            for (int i = points.size() - 1; i >= 0; --i) {
                JsonObject src = points.get(i).getAsJsonObject();
                JsonObject tmp = new JsonObject();
                tmpPrice = src.get("match_price").getAsFloat();
                tmpQtty = src.get("match_qtty").getAsLong();
                if (tmpPrice > 0.0) {
                    tmp.addProperty("x", src.get(timeField).getAsInt());
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

    private String getFloorCode(String floor) {
        if (!floorCodeMap.containsKey(floor)) {
            JsonArray floors = Utilities.getEsResult(esClient, new JsonObject(), "vndc_floors");
            for (int i = 0; i < floors.size(); i++) {
                JsonObject tmp = floors.get(i).getAsJsonObject();
                floorCodeMap.put(tmp.get("name").getAsString(), tmp.get("code").getAsString());
            }
        }
        return floorCodeMap.get(floor);
    }

}

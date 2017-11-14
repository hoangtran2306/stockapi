package com.finance24h.api.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Created by ait on 10/3/17.
 */
public interface FloorService {
    Map<String, String> getAllIndex();
    JsonObject getFloorInfoHeader();
    JsonObject getFloorDetail(String floorCode);
	JsonArray getFloorShareDetail(String floorCode, JsonObject graph);
	String getIndexByCode(String floorCode);
	JsonObject getFloorGraph(String floorCode, int type);
}

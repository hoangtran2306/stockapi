package com.finance24h.api.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public interface DeviceFollowService {
	public JsonArray getAllDeviceFollow(int device_id);
	public int getDeviceFollowBySymbol(int device_id, String symbol);
	public int getDeviceFollowBySymbol(JsonObject listDeviceFollows, String symbol);
	public List<String> getAllFollowSymbol(int deviceId);
}

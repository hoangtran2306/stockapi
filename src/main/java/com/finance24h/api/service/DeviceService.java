package com.finance24h.api.service;

import com.google.gson.JsonObject;

public interface DeviceService {
	public JsonObject getDevicesByDeviceID(String device_id);

}

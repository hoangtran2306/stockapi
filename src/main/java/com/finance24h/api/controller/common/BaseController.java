package com.finance24h.api.controller.common;

import com.finance24h.api.helpers.Utilities;
import com.finance24h.api.service.DeviceService;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class BaseController {
	
	public static String appVersion;
	public static String deviceId;
	public static String os;
	public static String accessToken;

	@Autowired
	private DeviceService deviceService;
	
	public void checkParams(HttpServletRequest request) throws CheckParamException {
		appVersion = request.getParameter("app_version");
		deviceId = request.getParameter("device_id");
		os =  request.getParameter("os");
		accessToken = request.getParameter("access_token");
		if (isEmpty(appVersion) || isEmpty(deviceId)) {
			String message = "Missing require parameters";
			throw new CheckParamException(message);
		}
		request.getSession().setAttribute("start_time", Utilities.getCurrentTimestampMilis());
		request.getSession().setAttribute("platform", getPlatform(request.getRequestURI()));
		request.getSession().setAttribute("app_version", appVersion);
		request.getSession().setAttribute("device_id", deviceId);
		JsonObject deviceObject = deviceService.getDevicesByDeviceID(deviceId);
		try {
			int id = deviceObject.get("id").getAsInt();
			request.getSession().setAttribute("deviceId", id);
		} catch (Exception e) {
			String message = "Device id invalid!";
			throw new CheckParamException(message);
		}
	}
	
	public JsonObject error(int status, String message) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("status", status);
		jsonObject.addProperty("message", message);
		if (accessToken != null) {
			return jsonObject;
		}
		return null;
	}

	private String getPlatform(String url) {
		String platform = null;
		if (isIos(url)) {
			platform = "ios";
		}
		if (isAndroid(url)) {
			platform = "android";
		}
		return platform;
	}

	private boolean isAndroid(String url) {
		return url.contains("android");
	}

	private boolean isIos(String url) {
		return url.contains("ios");
	}

	private boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}
}

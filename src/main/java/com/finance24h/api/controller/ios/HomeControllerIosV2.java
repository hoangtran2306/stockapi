package com.finance24h.api.controller.ios;

import com.finance24h.api.controller.common.HomeController;
import com.finance24h.api.helpers.LogStashHelper;
import com.finance24h.api.helpers.TemplateHelper;
import com.finance24h.api.helpers.Utilities;
import com.finance24h.api.service.DeviceFollowService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping(value = "/v2/ios", produces = "application/json")
public class HomeControllerIosV2 extends HomeController{
	public HomeControllerIosV2() {
		super();
	}

	@Autowired
	private LogStashHelper logStashHelper;
	@Autowired
	private DeviceFollowService deviceFollowService;

	@Override
	@RequestMapping(value = "/home")
	@ResponseBody
	@HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "homeV2")
	public String home(HttpServletRequest request) throws Exception {
		super.checkParams(request);
		String tableName;
		long timestamp;
		boolean pagingCache;
		String condition = request.getParameter("condition");
		try {
			if (condition == null) {
				tableName = "articles";
				timestamp = Utilities.getCurrentTimestamp();
				pagingCache = false;
			} else {
				byte[] conditionJsonByte = Base64.getDecoder().decode(condition);
				String conditionJsonString = new String(conditionJsonByte);
				JsonObject conditionJsonObject = new JsonParser().parse(conditionJsonString).getAsJsonObject();
				tableName = conditionJsonObject.get("key").getAsString();
				timestamp = conditionJsonObject.get("value").getAsLong();
				pagingCache = true;
			}
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		String followSymbolParam = request.getParameter("follow");
		int deviceId = (int)request.getSession().getAttribute("deviceId");
		List<String> followSymbols = new ArrayList<>();
		if (followSymbolParam != null && followSymbolParam.length() != 0) {
			String[] followSymbolArr = followSymbolParam.split(",");
			for (String oneSymbol : followSymbolArr) {
				followSymbols.add(oneSymbol);
			}
		} else {
			followSymbols = deviceFollowService.getAllFollowSymbol(deviceId);
		}
		JsonObject hotNewsGroupJson = super.getHotNewsGroup(deviceId, followSymbols, tableName, timestamp, pagingCache);

		JsonArray data = new JsonArray();
		data.add(hotNewsGroupJson);

		String homeTemplate = new TemplateHelper(data).toJson().toString();
		logStashHelper.logAPI(request);
		return homeTemplate;
	}
}

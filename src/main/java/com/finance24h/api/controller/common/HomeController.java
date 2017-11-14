package com.finance24h.api.controller.common;

import com.finance24h.api.helpers.GroupHelper;
import com.finance24h.api.helpers.LogStashHelper;
import com.finance24h.api.helpers.TemplateHelper;
import com.finance24h.api.helpers.Utilities;
import com.finance24h.api.service.FloorService;
import com.finance24h.api.service.NewsService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.List;

// http://10.2.0.36:8081/v1/ios/home
@Component
public class HomeController extends BaseController {
	
	@Autowired
	private NewsService newsService;
	@Autowired
	private FloorService floorService;
	@Autowired
	private LogStashHelper logStashHelper;
	
	@RequestMapping(value = "/home")
	@ResponseBody
	@HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "homeV1")
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
		JsonObject hotNewsGroupJson = getHotNewsGroup(tableName, timestamp, pagingCache);
		
		JsonArray data = new JsonArray();
		data.add(hotNewsGroupJson);
		
		String homeTemplate = new TemplateHelper(data).toJson().toString();
		logStashHelper.logAPI(request);
		return homeTemplate;
	}

	@RequestMapping(value = "/home/header")
	@ResponseBody
	@HystrixCommand(commandKey = "homeHeaderV1", ignoreExceptions = {Exception.class})
	public String header(HttpServletRequest request) throws Exception {
		super.checkParams(request);
		String headerTemplate = new TemplateHelper(getTopSharesGroup()).toJson().toString();
		logStashHelper.logAPI(request);
		return headerTemplate;
	}
	
	private JsonObject getHotNewsGroup(String tableName, long timestamp, boolean pagingCache) {
		return getHotNewsGroup(0,null, tableName, timestamp, pagingCache);
	}

	private JsonObject getTopSharesGroup() {
		JsonObject boxObject = floorService.getFloorInfoHeader();
		JsonArray dataArray = new JsonArray();
		dataArray.add(boxObject);
		return new GroupHelper("top_shares_group", dataArray).toJson();
	}

	public JsonObject getHotNewsGroup(int deviceId, List<String> followSymbols, String tableName, long timestamp, boolean pagingCache) {
		JsonArray topNews;
		if (followSymbols == null) {
			topNews = newsService.getNewestHomeArticles(tableName, timestamp, pagingCache);
		} else {
			topNews = newsService.getNewestHomeArticles(deviceId, followSymbols, tableName, timestamp, pagingCache);
		}
		JsonObject hotNewsGroup = new GroupHelper("hot_news_group", topNews).toJson();
		JsonArray boxDataArray = topNews.get(0).getAsJsonObject().get("data").getAsJsonArray();
		JsonObject groupViewMore = new JsonObject();
		groupViewMore.addProperty("key", tableName);
		int boxDataSize = boxDataArray.size();
		if (boxDataSize > 0) {
			JsonObject lastObject = boxDataArray.get((boxDataArray.size() - 1)).getAsJsonObject();
			long lastTimestamp = lastObject.get("published_date").getAsLong();
			groupViewMore.addProperty("value", lastTimestamp);
		} else {
			groupViewMore.addProperty("value", timestamp);
		}
		hotNewsGroup.add("group_view_more", groupViewMore);
		return hotNewsGroup;
	}
}

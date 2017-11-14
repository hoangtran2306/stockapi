package com.finance24h.api.controller.common;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import com.finance24h.api.helpers.LogStashHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.finance24h.api.helpers.GroupHelper;
import com.finance24h.api.helpers.TemplateHelper;
import com.finance24h.api.helpers.Utilities;
import com.finance24h.api.service.NewsService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@Component
public class NewsController extends BaseController {
	
	@Autowired
	private NewsService newsService;
	@Autowired
	private LogStashHelper logStashHelper;
	
	@RequestMapping(value = "/news")
	@ResponseBody
	@HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "newsV1")
	public String news(HttpServletRequest request) throws Exception {
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
	
	private JsonObject getHotNewsGroup(String tableName, long timestamp, boolean pagingCache) {
		JsonArray topNews = newsService.getListArticles(tableName, timestamp, pagingCache);
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

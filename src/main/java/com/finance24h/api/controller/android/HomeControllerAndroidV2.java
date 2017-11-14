package com.finance24h.api.controller.android;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.HomeController;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@RestController
@RequestMapping(value = "/v2/android", produces = "application/json")
public class HomeControllerAndroidV2 extends HomeController {
	public HomeControllerAndroidV2() {
		super();
	}
	
	final JsonParser jsonParser = new JsonParser();
	
	@Override
	@RequestMapping(value = "/home")
	@ResponseBody
	@HystrixCommand(fallbackMethod = "home", commandKey = "homeV2")
	public String home(HttpServletRequest request) throws Exception {
		String jsonString = super.home(request);
		JsonObject articleArr = jsonParser.parse(jsonString).getAsJsonObject();
		return articleArr.toString();
	}
}

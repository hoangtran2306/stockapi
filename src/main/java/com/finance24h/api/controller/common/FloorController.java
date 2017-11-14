package com.finance24h.api.controller.common;

import com.finance24h.api.helpers.GroupHelper;
import com.finance24h.api.helpers.LogStashHelper;
import com.finance24h.api.helpers.TemplateHelper;
import com.finance24h.api.helpers.Utilities;
import com.finance24h.api.service.FloorService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ait on 10/3/17.
 */
@Component
public class FloorController extends BaseController {

    @Autowired
    private FloorService floorService;
	@Autowired
	private LogStashHelper logService;

    @RequestMapping(value = "/floor/{code}")
    @ResponseBody
    @HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "floorDetailV1")
    public String floorDetail(HttpServletRequest request, @PathVariable String code) throws Exception {
        super.checkParams(request);
        Map<String, JsonObject> floorDetail = getFloorDetail(code);
        JsonArray groupArray = new JsonArray();
        groupArray.add(floorDetail.get("share_detail"));
        groupArray.add(floorDetail.get("statistic"));
        String template = new TemplateHelper(groupArray).toJson().toString();
        logService.logAPI(request);
        return template;
    }

    private Map<String, JsonObject> getFloorDetail(String floorCode) {
    	Map<String, JsonObject> floorDetail = new HashMap<>();
        JsonObject floorDetailObject = floorService.getFloorDetail(floorCode);
        JsonObject shareDetailGroup = new GroupHelper("share_detail_group", floorDetailObject.get("share_detail").getAsJsonArray()).toJson();
        JsonObject statisticGroup = new GroupHelper("key_statistics_group", Utilities.getLabel("key_statistics_group"), floorDetailObject.get("statistic").getAsJsonArray()).toJson();
        floorDetail.put("share_detail", shareDetailGroup);
        floorDetail.put("statistic", statisticGroup);
        return floorDetail;
    }
    
    @RequestMapping(value = "/floor/graph", produces = "application/json")
	@ResponseBody
	@HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "getFloorGraph")
	public String getFloorGraph(HttpServletRequest request, @RequestParam(value = "code") String code, @RequestParam(value = "type") int type) throws Exception {
		super.checkParams(request);
		if (type < 1 || type > 6) {
			throw new CheckParamException("Invalid type param!");
		}
		JsonArray data = new JsonArray();
		JsonObject graph = floorService.getFloorGraph(code, type);
		JsonObject shareDetailGroup = new GroupHelper("share_detail_group", floorService.getFloorShareDetail(code, graph)).toJson();
		data.add(shareDetailGroup);
        String response = new TemplateHelper(data).toJson().toString();
		logService.logAPI(request);
		return response;
	}
    
}

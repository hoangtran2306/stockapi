package com.finance24h.api.controller.common;

import com.finance24h.api.helpers.GroupHelper;
import com.finance24h.api.helpers.LogStashHelper;
import com.finance24h.api.helpers.TemplateHelper;
import com.finance24h.api.service.CompanyService;
import com.finance24h.api.service.DeviceFollowService;
import com.finance24h.api.service.DeviceService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

//import org.slf4j.LogStashHelper;

// http://10.2.0.36:8081/v1/ios/company
@Component
public class CompanyController extends BaseController {

    @Autowired
    LogStashHelper log;

    @Autowired
    public CompanyService companyService;
    @Autowired
    public DeviceFollowService deviceFollowService;
    @Autowired
    public DeviceService deviceService;

    @RequestMapping(value = "/companies")
    @ResponseBody
    @HystrixCommand(ignoreExceptions = {CheckParamException.class}, commandKey = "companyV1")
    public String companies(HttpServletRequest request) throws CheckParamException {
        super.checkParams(request);

        log.logInfo("call API: /companies");

        //deviceId = request.getParameter("device_id");
        JsonObject objCompaniesGroup = this.getAllCompanynies();

        JsonArray data = new JsonArray();
        data.add(objCompaniesGroup);

        String companyTemplate = new TemplateHelper(data).toJson().toString();
        log.logAPI(request);
        return companyTemplate;
    }


    @RequestMapping(value = "/follows")
    @ResponseBody
    @HystrixCommand(ignoreExceptions = {CheckParamException.class}, commandKey = "followsV1")
    public String follows(HttpServletRequest request) throws CheckParamException {
        super.checkParams(request);

        log.logInfo("call API: /follows");

        //deviceId = request.getParameter("device_id");
        JsonObject objCompaniesGroup = getCompanyGroup();

        JsonArray data = new JsonArray();
        data.add(objCompaniesGroup);

        String followTemplate = new TemplateHelper(data).toJson().toString();
        log.logAPI(request);
        return followTemplate;
    }


    public JsonObject getAllCompanynies() {
        //1. Get all companies
        JsonArray allProvider = companyService.getAllCompanies();

        return new GroupHelper("companies_group", "Company List", allProvider).toJson();
    }


    JsonObject getCompanyGroup() {
        JsonArray arrCompanies = new JsonArray();

        //1. Get Device by deviceID
        int id_device_table = -1;
        JsonObject deviceData = deviceService.getDevicesByDeviceID(deviceId);
        if (deviceData != null) {
            id_device_table = deviceData.get("id").getAsInt();
        }

        log.logInfo("id_device_table=" + id_device_table);

        //2. Get all device follows by id of device table
        JsonArray allDeviceFollows = deviceFollowService.getAllDeviceFollow(id_device_table);

        String tmp_symbols = "";
        for (JsonElement oneResultJson : allDeviceFollows) {
            String syb = oneResultJson.getAsJsonObject().get("symbol").getAsString().toUpperCase();

            if (tmp_symbols.equals("")) {
                tmp_symbols = syb;
            } else {
                tmp_symbols = tmp_symbols + "," + syb;
            }
        }

        String[] list_symbols = tmp_symbols.split(",");
        JsonArray allProvider = companyService.getCompaniesBySymbols(list_symbols);

        for (JsonElement pa : allProvider) {
            int id = pa.getAsJsonObject().get("id").getAsInt();
            String symbol = pa.getAsJsonObject().get("symbol").getAsString();
            JsonObject companyObject = new JsonObject();
            companyObject.addProperty("id", id);
            companyObject.addProperty("symbol", symbol);
            arrCompanies.add(companyObject);
        }

        return new GroupHelper("companies_group", "Company List", arrCompanies).toJson();
    }
}

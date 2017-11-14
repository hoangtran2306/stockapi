package com.finance24h.api.controller.common;

import com.finance24h.api.helpers.*;
import com.finance24h.api.model.ShareDetailDTO;
import com.finance24h.api.service.ArticleService;
import com.finance24h.api.service.DeviceFollowService;
import com.finance24h.api.service.DeviceService;
import com.finance24h.api.service.StockService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.List;

@Component
public class StockController extends BaseController {
    @Value("${stock_detail.relative_number}")
    private int relativeNumber;

    @Autowired
    private StockService stockService;
    @Autowired
    private ArticleService articleService;

    @Autowired
    private LogStashHelper logService;

    @Autowired
    private DeviceFollowService deviceFollowService;

    @Autowired
    private DeviceService deviceService;

    @RequestMapping(value = "/stock/graph", produces = "application/json")
    @ResponseBody
    @HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "getStockGraph")
    public String getStockGraph(HttpServletRequest request, @RequestParam(value = "symbol") String symbol, @RequestParam(value = "type") int type) throws Exception {
        super.checkParams(request);
        if (type < 1 || type > 6) {
            throw new CheckParamException("Invalid type param!");
        }
        ShareDetailDTO dto = stockService.getStockDetails(symbol);

        int id_device_table = -1;
        JsonObject deviceData = deviceService.getDevicesByDeviceID(deviceId);
        if (deviceData != null) {
            id_device_table = deviceData.get("id").getAsInt();
        }

        JsonArray data = new JsonArray();

        JsonObject graph = stockService.getStockGraph(symbol, type);
        JsonObject shareDetailGroupJson = getShareDetailGroup(dto, id_device_table, graph);
        data.add(shareDetailGroupJson);
        JsonObject stockTemplate = new TemplateHelper(data).toJson();
        String response = stockTemplate.toString();
        logService.logAPI(request);
        return response;
    }

    @RequestMapping(value = "/stock/basic", produces = "application/json")
    @ResponseBody
    @HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "getStockBasic")
    public String getStockBasic(HttpServletRequest request, @RequestParam(value = "symbols") String symbolList) throws Exception {
        super.checkParams(request);
        String[] symbols = symbolList.split(",");
        List<ShareDetailDTO> dtos = stockService.getStockDetails(symbols);

        JsonObject all_shares_group = getAllShareGroup(dtos, false);

        JsonArray data = new JsonArray();
        data.add(all_shares_group);

        JsonObject stockTemplate = new TemplateHelper(data).toJson();
        String response = stockTemplate.toString();
        logService.logAPI(request);
        return response;
    }

    @RequestMapping(value = "/stock/detail", produces = "application/json")
    @ResponseBody
    @HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "getStockDetail")
    public String getStockDetail(HttpServletRequest request, @RequestParam(value = "symbols") String symbolList) throws Exception {
        super.checkParams(request);
        String[] symbols = symbolList.split(",");
        List<ShareDetailDTO> dtos = stockService.getStockDetails(symbols);

        JsonObject all_shares_group = getAllShareGroup(dtos, true);

        JsonArray data = new JsonArray();
        data.add(all_shares_group);

        JsonObject stockTemplate = new TemplateHelper(data).toJson();
        String response = stockTemplate.toString();
        logService.logAPI(request);
        return response;
    }

    private JsonObject getAllShareGroup(List<ShareDetailDTO> dtos, boolean detail) {
        JsonArray details = new JsonArray();
        for (ShareDetailDTO dto : dtos) {
            JsonObject obj = new JsonObject();
            obj.addProperty("symbol", dto.getSymbol());
            obj.addProperty("company_name", dto.getCompanyName());
            obj.addProperty("price", dto.getPrice());
            obj.addProperty("change_percent", dto.getPercentChange());
            obj.addProperty("pre_change_percent", dto.getPrePercentChange());
            obj.addProperty("basic_price", dto.getBasicPrice());
            if (detail) {
                obj.addProperty("change", dto.getPriceChange());
                obj.addProperty("day_low", dto.getLow());
                obj.addProperty("day_high", dto.getHigh());
                obj.addProperty("52wk_low", dto.getYearLow());
                obj.addProperty("52wk_high", dto.getYearHigh());
                obj.addProperty("market_value", dto.getMarketValue());
                obj.addProperty("mkt_cap", dto.getMarketCapacity());
                obj.addProperty("market_time", dto.getUpdated());
                obj.addProperty("volume", dto.getVolume());
                obj.addProperty("avg_volume", dto.getAverageVolume());
            }
            details.add(obj);
        }
        JsonArray data = new JsonArray();
        data.add(new BoxHelper("", details).toJson());
        return new GroupHelper("all_shares_group", data).toJson();
    }

    @RequestMapping(value = "/stock", produces = "application/json")
    @ResponseBody
    @HystrixCommand(ignoreExceptions = {Exception.class}, commandKey = "getStock")
    public String getStock(HttpServletRequest request, @RequestParam(value = "symbol") String symbol) throws Exception {
        super.checkParams(request);
        ShareDetailDTO dto = stockService.getStockDetails(symbol);
        String condition = request.getParameter("condition");
        JsonArray data = new JsonArray();
        try {
            if (condition != null) {
                byte[] conditionJsonByte = Base64.getDecoder().decode(condition);
                String conditionJsonString = new String(conditionJsonByte);
                JsonObject conditionJsonObject = new JsonParser().parse(conditionJsonString).getAsJsonObject();
                String tableName = conditionJsonObject.get("key").getAsString();
                long timestamp = conditionJsonObject.get("value").getAsLong();
                JsonObject relatedArticlesGroupJson = getRelatedArticlesGroup(symbol, tableName, timestamp);
                data.add(relatedArticlesGroupJson);
                String response = new TemplateHelper(data).toJson().toString();
                logService.logAPI(request);
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        int id_device_table = -1;
        JsonObject deviceData = deviceService.getDevicesByDeviceID(deviceId);
        if (deviceData != null) {
            id_device_table = deviceData.get("id").getAsInt();
        }

        JsonObject shareDetailGroupJson = getShareDetailGroup(dto, id_device_table);
        JsonObject keyStatisticsGroupJson = getKeyStatisticsGroup(dto);
        JsonObject conversationsGroupJson = getConversationsGroup();
        JsonObject relatedArticlesGroupJson = getRelatedArticlesGroup(symbol, "articles", -1);
//		JsonObject hotNewsGroupJson = getHotNewsGroup();


        data.add(shareDetailGroupJson);
        data.add(keyStatisticsGroupJson);
        data.add(conversationsGroupJson);
        data.add(relatedArticlesGroupJson);
//		data.add(hotNewsGroupJson);


        String response = new TemplateHelper(data).toJson().toString();
        logService.logAPI(request);
        return response;
    }

    public JsonObject getShareDetailGroup(ShareDetailDTO dto, int device_id) {
        JsonArray details = new JsonArray();
        details.add(getStockDetails(dto, device_id, null));
        JsonArray data = new JsonArray();
        data.add(new BoxHelper("", details).toJson());
        return new GroupHelper("share_detail_group", data).toJson();
    }

    public JsonObject getShareDetailGroup(ShareDetailDTO dto, int device_id, JsonObject graph) {
        JsonArray details = new JsonArray();
        details.add(getStockDetails(dto, device_id, graph));
        JsonArray data = new JsonArray();
        data.add(new BoxHelper("", details).toJson());
        return new GroupHelper("share_detail_group", data).toJson();
    }

    private JsonObject getStockDetails(ShareDetailDTO dto, int device_id, JsonObject graph) {
        JsonObject obj = new JsonObject();
        obj.addProperty("symbol", dto.getSymbol());
        obj.addProperty("company_name", dto.getCompanyName());
        obj.addProperty("following", deviceFollowService.getDeviceFollowBySymbol(device_id, dto.getSymbol()));
        obj.addProperty("price", dto.getPrice());
        obj.addProperty("change", dto.getPriceChange());
        obj.addProperty("pre_change", dto.getPercentChange());
        obj.addProperty("currency_price", dto.getCurrency());
        obj.addProperty("stock_exchange", dto.getFloor());
        obj.addProperty("session_status", dto.getSessionStatus());
        obj.addProperty("is_open", dto.isOpen());
        obj.addProperty("updated_at", dto.getUpdated());
        JsonArray graphArray = new JsonArray();
        if (graph != null) {
            graphArray.add(graph);
        }
        obj.add("graph_data", graphArray);
        return obj;
    }

    public JsonObject getKeyStatisticsGroup(ShareDetailDTO dto) {
        JsonArray dayStatistics = new JsonArray();
        dayStatistics.add(getDayStatistics(dto));
        JsonArray yearStatistics = new JsonArray();
        yearStatistics.add(getYearStatistics(dto));
        JsonArray otherStatistics = getOtherStatistics(dto);
        JsonArray data = new JsonArray();
        data.add(new BoxHelper("day_range_statistics_box", Utilities.getLabel("day_range_statistics_box"), dayStatistics).toJson());
//		data.add(new BoxHelper("week_range_statistics_box", Utilities.getLabel("week_range_statistics_box"), yearStatistics).toJson());
        data.add(new BoxHelper("other_statistics_box", Utilities.getLabel("other_statistics_box"), otherStatistics).toJson());
        return new GroupHelper("key_statistics_group", Utilities.getLabel("key_statistics_group"), data).toJson();
    }

    private JsonArray getOtherStatistics(ShareDetailDTO dto) {
        JsonArray data = new JsonArray();
        JsonObject obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.prev_close"));
        obj.addProperty("value", Utilities.numberFormat(dto.getBasicPrice()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.open"));
        obj.addProperty("value", Utilities.numberFormat(dto.getOpenPrice()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.low"));
        obj.addProperty("value", Utilities.numberFormat(dto.getLow()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.high"));
        obj.addProperty("value", Utilities.numberFormat(dto.getHigh()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.year_low"));
        obj.addProperty("value", Utilities.numberFormat(dto.getYearLow()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.year_high"));
        obj.addProperty("value", Utilities.numberFormat(dto.getYearHigh()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.mkr_cap"));
        obj.addProperty("value", Utilities.numberFormat(dto.getMarketCapacity()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.circulation_volume"));
        obj.addProperty("value", Utilities.numberFormat(dto.getCirculationVolume()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.accumulated_volume"));
        obj.addProperty("value", Utilities.numberFormat(dto.getAccumulatedVolume()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.year_target"));
        obj.addProperty("value", Utilities.numberFormat(dto.getTarget()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.avg_volume"));
        obj.addProperty("value", Utilities.numberFormat(dto.getAverageVolume()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.pe"));
        obj.addProperty("value", Utilities.numberFormat(dto.getPe()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.eps"));
        obj.addProperty("value", Utilities.numberFormat(dto.getEps()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.beta"));
        obj.addProperty("value", Utilities.numberFormat(dto.getBeta()));
        data.add(obj);
        obj = new JsonObject();
        obj.addProperty("name", Utilities.getLabel("stock.dividend"));
        obj.addProperty("value", Utilities.numberFormat(dto.getDividends()));
        data.add(obj);
//		obj = new JsonObject();
//		obj.addProperty("name", Utilities.getLabel("stock.earning_date"));
//		obj.addProperty("value", dto.getEarningDate());
//		data.add(obj);
        return data;
    }

    private JsonObject getYearStatistics(ShareDetailDTO dto) {
        JsonObject obj = new JsonObject();
        obj.addProperty("pointer_value", dto.getPrice());
        obj.addProperty("min_value", dto.getYearLow());
        obj.addProperty("max_value", dto.getYearHigh());
        return obj;
    }

    private JsonObject getDayStatistics(ShareDetailDTO dto) {
        JsonObject obj = new JsonObject();
        obj.addProperty("low_value", dto.getBasicPrice() > dto.getPrice() ? dto.getPrice() : dto.getBasicPrice());
        obj.addProperty("high_value", dto.getBasicPrice() < dto.getPrice() ? dto.getPrice() : dto.getBasicPrice());
        obj.addProperty("pointer_value", dto.getPrice());
        obj.addProperty("min_value", dto.getFloorPrice());
        obj.addProperty("max_value", dto.getCeilingPrice());
        return obj;
    }

    private JsonObject getConversationsGroup() {
        return new GroupHelper("conversations_group", Utilities.getLabel("conversations_group"), null).toJson();
    }

    private JsonObject getRelatedArticlesGroup(String symbol, String tableName, long timestamp) {
        JsonArray related = articleService.getRelatedArticlesByStock(symbol, relativeNumber, tableName, timestamp);
        JsonArray data = new JsonArray();
        data.add(new BoxHelper("related_article_box", Utilities.getLabel("related_article_box"), related).toJson());
        JsonObject groupObject = new GroupHelper("related_articles_group", Utilities.getLabel("related_articles_group"), data).toJson();
        JsonObject groupViewMore = new JsonObject();
        groupViewMore.addProperty("key", tableName);
        int boxDataSize = related.size();
        if (boxDataSize > 0) {
            JsonObject lastObject = related.get((related.size() - 1)).getAsJsonObject();
            long lastTimestamp = lastObject.get("published_date").getAsLong();
            groupViewMore.addProperty("value", lastTimestamp);
        } else {
            groupViewMore.addProperty("value", timestamp);
        }
        groupObject.add("group_view_more", groupViewMore);
        return groupObject;
    }
}

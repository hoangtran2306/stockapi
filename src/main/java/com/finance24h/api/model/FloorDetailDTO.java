package com.finance24h.api.model;

import com.finance24h.api.helpers.BoxHelper;
import com.finance24h.api.helpers.Utilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by ait on 10/4/17.
 */
public class FloorDetailDTO {
    // share detail
    private String floorCode;
    private String index;
    private float marketIndex;
    private float priorMarketIndex;
    // statistic
    private float dayMinValue;
    private float dayMaxValue;
    private float dayCurrentValue;
    private float weekMinValue;
    private float weekMaxValue;
    private float weekCurrentValue;
    private float indexMaxValue;
    private float indexMinValue;
    private int advanceValue;
    private int declineValue;
    private int noChangeValue;
    private float totalValueTraded;
    private float totalShareTraded;
    private long tradingDate;
    private String tradingTime;

    public void setFloorCode(String floorCode) {
        this.floorCode = floorCode;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void setMarketIndex(float marketIndex) {
        this.marketIndex = marketIndex;
    }

    public void setPriorMarketIndex(float priorMarketIndex) {
        this.priorMarketIndex = priorMarketIndex;
    }

    public void setDayMinValue(float dayMinValue) {
        this.dayMinValue = dayMinValue;
    }

    public void setDayMaxValue(float dayMaxValue) {
        this.dayMaxValue = dayMaxValue;
    }

    public void setDayCurrentValue(float dayCurrentValue) {
        this.dayCurrentValue = dayCurrentValue;
    }

    public void setWeekMinValue(float weekMinValue) {
        this.weekMinValue = weekMinValue;
    }

    public void setWeekMaxValue(float weekMaxValue) {
        this.weekMaxValue = weekMaxValue;
    }

    public void setWeekCurrentValue(float weekCurrentValue) {
        this.weekCurrentValue = weekCurrentValue;
    }

    public void setIndexMaxValue(float indexMaxValue) {
        this.indexMaxValue = indexMaxValue;
    }

    public void setIndexMinValue(float indexMinValue) {
        this.indexMinValue = indexMinValue;
    }

    public void setAdvanceValue(int advanceValue) {
        this.advanceValue = advanceValue;
    }

    public void setDeclineValue(int declineValue) {
        this.declineValue = declineValue;
    }

    public void setNoChangeValue(int noChangeValue) {
        this.noChangeValue = noChangeValue;
    }

    public void setTotalValueTraded(float totalValueTraded) {
        this.totalValueTraded = totalValueTraded;
    }

    public void setTotalShareTraded(float totalShareTraded) {
        this.totalShareTraded = totalShareTraded;
    }

    public void setTradingDate(long tradingDate) {
        this.tradingDate = tradingDate;
    }

    public void setTradingTime(String tradingTime) {
        this.tradingTime = tradingTime;
    }

    public JsonArray getStatistic() {
        JsonArray jsonArray = new JsonArray();
        // day_range_statistics_box
        JsonObject dayRangeObject = new JsonObject();
        dayRangeObject.addProperty("min_value", dayMinValue);
        dayRangeObject.addProperty("max_value", dayMaxValue);
        dayRangeObject.addProperty("pointer_value", dayCurrentValue);
        makeBoxArray("day_range_statistics_box", Utilities.getLabel("day_range_statistics_box"), dayRangeObject, jsonArray);

        // week_range_statistics_box
        JsonObject weekRangeObject = new JsonObject();
        weekRangeObject.addProperty("min_value", weekMinValue);
        weekRangeObject.addProperty("max_value", weekMaxValue);
        weekRangeObject.addProperty("pointer_value", weekCurrentValue);
//        makeBoxArray("week_range_statistics_box", Utilities.getLabel("week_range_statistics_box"), weekRangeObject, jsonArray);

        // other_statistics_box
        JsonArray dataArray = new JsonArray();
        dataForOther(Utilities.getLabel("floor.highest_index"), Utilities.numberFormat(indexMaxValue), dataArray);
        dataForOther(Utilities.getLabel("floor.lowest_index"), Utilities.numberFormat(indexMinValue), dataArray);
        dataForOther(Utilities.getLabel("floor.advance"), Utilities.numberFormat(advanceValue), dataArray);
        dataForOther(Utilities.getLabel("floor.decline"), Utilities.numberFormat(declineValue), dataArray);
        dataForOther(Utilities.getLabel("floor.no_change"), Utilities.numberFormat(noChangeValue), dataArray);
        dataForOther(Utilities.getLabel("floor.total_value_traded"), Utilities.numberFormat(totalValueTraded), dataArray);
        dataForOther(Utilities.getLabel("floor.total_share_traded"), Utilities.numberFormat((int)totalShareTraded), dataArray);
        String dateTimeTrading;
        Date date = new Date(tradingDate * 1000);
        dateTimeTrading = new SimpleDateFormat("dd/MM/yyyy").format(date) + " " + tradingTime;
        dataForOther(Utilities.getLabel("floor.trading_date"), dateTimeTrading, dataArray);
        makeBoxArray("other_statistics_box", dataArray, jsonArray);
        return  jsonArray;
    }
    
    public JsonArray getShareDetail() {
        return getShareDetail(null);
    }

    public JsonArray getShareDetail(JsonObject graph) {
        JsonArray jsonArray = new JsonArray();
        JsonObject boxObject = new JsonObject();
        boxObject.addProperty("share_id", floorCode);
        boxObject.addProperty("symbol", index);
        boxObject.addProperty("following", 1);
        boxObject.addProperty("price", marketIndex);
        boxObject.addProperty("change", marketIndex - priorMarketIndex);
        float preChange = priorMarketIndex == 0 ? 0 : ((marketIndex - priorMarketIndex) * 100 / priorMarketIndex);
        boxObject.addProperty("pre_change", preChange);
        int hour = Calendar.getInstance(TimeZone.getTimeZone(Utilities.timeZone)).get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        boolean isOpen = hour >= 9 && hour < 15 && dayOfWeek != 1 && dayOfWeek != 7;
        boxObject.addProperty("session_status", isOpen ? Utilities.getLabel("session.opened") : Utilities.getLabel("session.closed"));
        boxObject.addProperty("is_open", isOpen);
        boxObject.addProperty("updated_at", tradingDate);
        JsonArray graphArray = new JsonArray();
		if (graph != null) {
			graphArray.add(graph);
		}
        boxObject.add("graph_data", graphArray);
        makeBoxArray(null, "", boxObject, jsonArray);
        return jsonArray;
    }

    private void makeBoxArray(String boxId, String boxName, JsonObject data, JsonArray boxArray) {
        JsonArray boxDataArray = new JsonArray();
        boxDataArray.add(data);
        JsonObject boxObject = new BoxHelper(boxId, boxName, boxDataArray).toJson();
        boxArray.add(boxObject);
    }

    private void makeBoxArray(String boxId, JsonArray data, JsonArray boxArray) {
        JsonObject boxObject = new BoxHelper(boxId, null, data).toJson();
        boxArray.add(boxObject);
    }

    private void dataForOther(String name, Object value, JsonArray dataArray) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", name);
        String valueString = String.valueOf(value);
        jsonObject.addProperty("value", valueString);
        dataArray.add(jsonObject);
    }
}

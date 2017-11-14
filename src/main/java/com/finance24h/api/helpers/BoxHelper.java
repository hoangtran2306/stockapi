package com.finance24h.api.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class BoxHelper {
	private String boxId;
	private String boxName;
	private JsonArray data;

	public BoxHelper(JsonArray data) {
		init(null, "", data);
	}
	public BoxHelper(String boxName, JsonArray data) {
		init(null, boxName, data);
	}
	public BoxHelper(String boxId, String boxName, JsonArray data) {
		init(boxId, boxName, data);
	}
	public String getBoxId() {
		return boxId;
	}
	public void setBoxId(String boxId) {
		this.boxId = boxId;
	}
	public String getBoxName() {
		return boxName;
	}
	public void setBoxName(String boxName) {
		this.boxName = boxName;
	}
	public JsonArray getData() {
		return data;
	}
	public void setData(JsonArray data) {
		this.data = data;
	}
	public JsonObject toJson() {
		JsonObject boxJsonObject = new JsonObject();
		if (boxId != null) {
			boxJsonObject.addProperty("box", boxId);
		}
		if (boxName != null) {
			boxJsonObject.addProperty("box_name", boxName);
		}
		boxJsonObject.add("data", data == null ? (new JsonArray()) : data);
		return boxJsonObject;
	}
	
	private void init(String boxId, String boxName, JsonArray data) {
		this.boxId = boxId;
		this.boxName = boxName;
		this.data = data;
	}
}

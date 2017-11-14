package com.finance24h.api.helpers;

import org.springframework.http.HttpStatus;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TemplateHelper {
	private JsonArray data;
	
	public TemplateHelper(JsonArray data) {
		this.data = data;
	}
	public TemplateHelper(JsonObject oneData) {
		JsonArray data = new JsonArray();
		data.add(oneData);
		this.data = data;
	}

	public JsonArray getData() {
		return data;
	}

	public void setData(JsonArray data) {
		this.data = data;
	}
	
	public JsonObject toJson() {
		JsonObject templateJsonObject = new JsonObject();
		templateJsonObject.addProperty("message", "success");
		templateJsonObject.addProperty("status", HttpStatus.OK.value());
		templateJsonObject.add("data", this.data);
		return templateJsonObject;
	}
}

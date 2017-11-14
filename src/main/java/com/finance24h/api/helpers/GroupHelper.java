package com.finance24h.api.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GroupHelper {
	
	private String groupId;
	private String groupName;
	private JsonArray data;
	private JsonObject info;
	
	public GroupHelper() {}

	public GroupHelper(String groupId, JsonArray data) {
		init(groupId, null, data, null);
	}
	/**
	 * 
	 * @param groupId
	 * @param groupName
	 * @param data
	 */
	public GroupHelper(String groupId, String groupName, JsonArray data) {
		init(groupId, groupName, data, null);
	}
	/**
	 * 
	 * @param groupId
	 * @param groupName
	 * @param data
	 * @param info
	 */
	public GroupHelper(String groupId, String groupName, JsonArray data, JsonObject info) {
		init(groupId, groupName, data, info);
	}
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public JsonArray getData() {
		return data;
	}
	public void setData(JsonArray data) {
		this.data = data;
	}
	public JsonObject getInfo() {
		return info;
	}
	public void setInfo(JsonObject info) {
		this.info = info;
	}
	public JsonObject toJson() {
		JsonObject groupJsonObject = new JsonObject();
		groupJsonObject.addProperty("group", groupId);
		groupJsonObject.addProperty("group_name", groupName == null ? "" : groupName);
		groupJsonObject.add("data",data == null ? (new JsonArray()) : data);
		if (info != null) {
			groupJsonObject.add("info", info);
		}
		return groupJsonObject;
	}
	
	private void init(String groupId, String groupName, JsonArray data, JsonObject info) {
		this.groupId = groupId;
		this.groupName = groupName;
		this.data = data;
		this.info = info;
	}
}

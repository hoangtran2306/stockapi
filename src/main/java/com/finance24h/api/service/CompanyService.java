package com.finance24h.api.service;

import com.google.gson.JsonArray;

public interface CompanyService {
	public int getShareIdByTag(String tag);
	public int getShareIdByTagAndFloor(String tag, String floor);
	public JsonArray getAllCompanies();

	public JsonArray getCompaniesBySymbols(String[] symbols);

}

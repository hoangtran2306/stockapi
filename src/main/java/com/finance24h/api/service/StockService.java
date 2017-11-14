package com.finance24h.api.service;

import java.util.List;

import com.finance24h.api.model.ShareDetailDTO;
import com.google.gson.JsonObject;

public interface StockService {

	ShareDetailDTO getStockDetails(String stockCode);

	List<ShareDetailDTO> getStockDetails(String[] symbols);
	
	List<ShareDetailDTO> getStockDetails(String[] symbols, int limit);

	JsonObject getStockGraph(String symbol, int type);

}

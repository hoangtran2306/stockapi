package com.finance24h.api.controller.android;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.StockController;

@RestController
@RequestMapping(value = "/v1/android")
public class StockControllerAndroidV1 extends StockController{
	public StockControllerAndroidV1() {
		super();
	}
}

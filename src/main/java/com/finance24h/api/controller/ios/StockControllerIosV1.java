package com.finance24h.api.controller.ios;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.StockController;

@RestController
@RequestMapping(value = "/v1/ios")
public class StockControllerIosV1 extends StockController{
	public StockControllerIosV1() {
		super();
	}
}

package com.finance24h.api.controller.ios;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.HomeController;

@RestController
@RequestMapping(value = "/v1/ios", produces = "application/json")
public class HomeControllerIosV1 extends HomeController{
	public HomeControllerIosV1() {
		super();
	}
}

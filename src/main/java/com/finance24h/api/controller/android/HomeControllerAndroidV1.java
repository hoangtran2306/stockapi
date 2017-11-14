package com.finance24h.api.controller.android;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.HomeController;

@RestController
@RequestMapping(value = "/v1/android", produces = "application/json")
public class HomeControllerAndroidV1 extends HomeController{
	public HomeControllerAndroidV1() {
		super();
	}
}

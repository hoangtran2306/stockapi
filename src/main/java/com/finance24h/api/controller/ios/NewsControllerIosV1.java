package com.finance24h.api.controller.ios;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.NewsController;

@RestController
@RequestMapping(value = "/v1/ios", produces = "application/json")
public class NewsControllerIosV1 extends NewsController{
	public NewsControllerIosV1() {
		super();
	}
}

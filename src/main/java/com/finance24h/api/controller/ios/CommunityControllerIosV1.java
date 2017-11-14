package com.finance24h.api.controller.ios;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.CommunityController;

@RestController
@RequestMapping(value = "/v1/ios", produces = "application/json")
public class CommunityControllerIosV1 extends CommunityController{
	public CommunityControllerIosV1() {
		super();
	}
}

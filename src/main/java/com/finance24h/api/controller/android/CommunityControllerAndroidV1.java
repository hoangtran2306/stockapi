package com.finance24h.api.controller.android;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.CommunityController;

@RestController
@RequestMapping(value = "/v1/android")
public class CommunityControllerAndroidV1 extends CommunityController{
	public CommunityControllerAndroidV1() {
		super();
	}
}

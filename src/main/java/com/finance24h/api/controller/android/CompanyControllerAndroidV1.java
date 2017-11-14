package com.finance24h.api.controller.android;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.CompanyController;

@RestController
@RequestMapping(value = "/v1/android", produces = "application/json")
public class CompanyControllerAndroidV1 extends CompanyController{
	public CompanyControllerAndroidV1() {
		super();
	}
}

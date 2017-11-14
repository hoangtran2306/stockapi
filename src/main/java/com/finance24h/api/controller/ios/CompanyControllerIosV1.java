package com.finance24h.api.controller.ios;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.finance24h.api.controller.common.CompanyController;

@RestController
@RequestMapping(value = "/v1/ios", produces = "application/json")
public class CompanyControllerIosV1 extends CompanyController{
	public CompanyControllerIosV1() {
		super();
	}
}

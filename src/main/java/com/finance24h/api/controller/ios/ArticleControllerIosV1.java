package com.finance24h.api.controller.ios;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.ArticleController;

@RestController
@RequestMapping(value = "/v1/ios")
public class ArticleControllerIosV1 extends ArticleController{
	public ArticleControllerIosV1() {
		super();
	}
}

package com.finance24h.api.controller.android;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance24h.api.controller.common.ArticleController;

@RestController
@RequestMapping(value = "/v1/android")
public class ArticleControllerAndroidV1 extends ArticleController{
	public ArticleControllerAndroidV1() {
		super();
	}
}

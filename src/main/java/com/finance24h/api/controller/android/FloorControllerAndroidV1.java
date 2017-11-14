package com.finance24h.api.controller.android;

import com.finance24h.api.controller.common.FloorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by ait on 10/3/17.
 */
@RestController
@RequestMapping(value = "/v1/android", produces = "application/json")
public class FloorControllerAndroidV1 extends FloorController {
    public FloorControllerAndroidV1() {
        super();
    }
}

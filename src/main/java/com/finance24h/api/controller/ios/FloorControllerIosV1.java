package com.finance24h.api.controller.ios;

import com.finance24h.api.controller.common.FloorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by ait on 10/3/17.
 */
@RestController
@RequestMapping(value = "/v1/ios", produces = "application/json")
public class FloorControllerIosV1 extends FloorController {
    public FloorControllerIosV1() {
        super();
    }
}

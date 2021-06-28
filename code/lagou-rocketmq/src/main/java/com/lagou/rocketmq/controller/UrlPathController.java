package com.lagou.rocketmq.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("buy/url")
public class UrlPathController {

    @GetMapping
    public Map getUrl(@RequestParam Integer productId) {

        String urlPath = "";

        if (LocalDateTime.now().compareTo(LocalDateTime.parse("2020-10-07T21:00:00")) >= 0) {
            urlPath = "product/" + productId + "/order";
        }

        Map<String, String> result = new HashMap();
        result.put("urlPath", urlPath);
        return result;
    }
}

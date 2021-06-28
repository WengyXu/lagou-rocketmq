package com.lagou.rocketmq.controller;

import com.lagou.rocketmq.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("product")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("{productId}/order")
    public Boolean order(@PathVariable Integer productId,
                         @RequestParam Long userId) {
        return orderService.order(productId, userId);
    }
}

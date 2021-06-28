package com.lagou.rocketmq.entity;

import lombok.Data;

@Data
public class ProductStock {

    private Integer productId;
    private String productName;
    private Integer amount;
}

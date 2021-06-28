package com.lagou.rocketmq.entity;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class OrderInfo implements Serializable {

    private Long orderId;
    private Long userId;
    private Integer productId;
    private Integer price;
    private Integer amount;
    private Integer status;
    private Timestamp createTime;
    private Timestamp modifyTime;
}

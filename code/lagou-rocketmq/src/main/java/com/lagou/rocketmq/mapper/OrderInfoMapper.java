package com.lagou.rocketmq.mapper;

import com.lagou.rocketmq.entity.OrderInfo;
import org.springframework.stereotype.Component;

@Component
public interface OrderInfoMapper {

    Long add(OrderInfo orderInfo);

    Long modifyStatusById(Long orderId, Integer oldStatus, Integer newStatus);

    Integer findStatusById(Long orderId);
}

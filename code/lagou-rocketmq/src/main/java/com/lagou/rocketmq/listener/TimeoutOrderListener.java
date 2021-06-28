package com.lagou.rocketmq.listener;

import com.alibaba.fastjson.JSON;
import com.lagou.rocketmq.entity.OrderInfo;
import com.lagou.rocketmq.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "order_consumer02", topic = "timeout_order")
public class TimeoutOrderListener implements RocketMQListener {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void onMessage(Object object) {

        OrderInfo orderInfo = JSON.parseObject((String) object, OrderInfo.class);

        // 1 检查订单状态
        Integer status = orderService.getStatus(orderInfo.getOrderId());
        if (status != null && status == 1) {
            // 2 取消订单
            orderService.cancelOrder(orderInfo.getOrderId());
            log.info("取消订单，订单id：" + orderInfo.getOrderId());

            // 3 恢复redis库存
//            redisTemplate.opsForValue().increment(String.valueOf(orderInfo.getProductId()));
            Integer amount = (Integer) redisTemplate.opsForValue().get(orderInfo.getProductId());
            redisTemplate.opsForValue().set(orderInfo.getProductId(), amount + 1);
            amount = (Integer) redisTemplate.opsForValue().get(orderInfo.getProductId());
            log.info("恢复redis库存:" + amount + "，商品id：" + orderInfo.getProductId());
        }
    }
}

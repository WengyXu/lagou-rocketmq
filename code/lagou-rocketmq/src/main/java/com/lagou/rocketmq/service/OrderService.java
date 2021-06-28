package com.lagou.rocketmq.service;

import com.lagou.rocketmq.entity.OrderInfo;
import com.lagou.rocketmq.mapper.OrderInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderService {

    public static Boolean isSellOut = false;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    public Boolean order(Integer productId, Long userId) {

        // 1 检查销售状态
        if (isSellOut) {
            // 已售完
            log.info("检查销售状态，商品已售完，商品id：" + productId);
            return false;
        }

        // 2 检查redis库存
        Integer amount = (Integer) redisTemplate.opsForValue().get(productId);
        if (amount <= 0) {
            isSellOut = true;
            log.info("检查redis库存，商品已售完，商品id：" + productId);
            // 已售完
            return false;
        }

        // 3 发送订单消息，等待生成订单
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setProductId(productId);
        orderInfo.setUserId(userId);

        rocketMQTemplate.asyncSend("new_order", orderInfo, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                // 扣减redis库存
//                redisTemplate.opsForValue().decrement(orderInfo.getProductId());
                Integer amount = (Integer) redisTemplate.opsForValue().get(orderInfo.getProductId());
                redisTemplate.opsForValue().set(orderInfo.getProductId(), amount - 1);
                amount = (Integer) redisTemplate.opsForValue().get(orderInfo.getProductId());
                log.info("扣减redis库存:" + amount + "，商品id：" + orderInfo.getProductId());
            }

            @Override
            public void onException(Throwable throwable) {

            }
        });
        log.info("发送订单消息，等待生成订单，商品id：" + productId + "，用户id：" + userId);

        return true;
    }

    public Long createOrder(Integer productId, Long userId) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setProductId(productId);
        orderInfo.setUserId(userId);
        orderInfo.setPrice(300000);
        orderInfo.setAmount(1);
        orderInfo.setStatus(1);
        Long count = orderInfoMapper.add(orderInfo);
        if (count == 0) {
            log.error("创建订单失败");
        }

        return orderInfo.getOrderId();
    }

    public void cancelOrder(Long orderId) {
        Long count = orderInfoMapper.modifyStatusById(orderId, 1, 2);
        if (count == 0) {
            log.error("取消订单失败");
        }
    }

    public Integer getStatus(Long orderId) {
        return orderInfoMapper.findStatusById(orderId);
    }
}

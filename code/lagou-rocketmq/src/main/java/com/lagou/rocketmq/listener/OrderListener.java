package com.lagou.rocketmq.listener;

import com.alibaba.fastjson.JSON;
import com.lagou.rocketmq.entity.OrderInfo;
import com.lagou.rocketmq.service.OrderService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "new_order",
        consumerGroup = "order_consumer01",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        secretKey = "*")
public class OrderListener implements RocketMQListener<OrderInfo> {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @SneakyThrows
    @Override
    public void onMessage(OrderInfo orderInfo) {

        // 1 创建订单
        Long orderId = orderService.createOrder(orderInfo.getProductId(), orderInfo.getUserId());
        orderInfo.setOrderId(orderId);
        log.info("订单创建成功，订单id：" + orderId);

        // 2 发送延迟消息，准备订单超时处理
        Message message = new Message("timeout_order", JSON.toJSONString(orderInfo).getBytes());
        org.springframework.messaging.Message springMessage = RocketMQUtil.convertToSpringMessage(message);
        rocketMQTemplate.asyncSend(message.getTopic(), springMessage, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {

            }

            @Override
            public void onException(Throwable throwable) {

            }
        }, 10000, 3);

        log.info("发送延迟消息，准备订单超时处理，订单id：" + orderInfo.getOrderId());
    }
}

package com.lagou.rocketmq;

import com.lagou.rocketmq.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(value = 1)
public class InitDataRunner implements CommandLineRunner {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductService productService;

    @Override
    public void run(String... args) {

        // 初始化Redis库存信息
        initRedisStock();
    }

    /**
     * 初始化Redis库存信息
     * 从mysql库存表中读入数据，用于秒杀时检查库存数量
     */
    private void initRedisStock() {
        log.info("初始化Redis库存信息");
        productService.getStocks().forEach(e -> {
            redisTemplate.opsForValue().set(e.getProductId(), e.getAmount());
            log.info("商品id：" + e.getProductId() + "，库存数量：" + e.getAmount());
        });
    }
}

package com.lagou.rocketmq.mapper;

import com.lagou.rocketmq.entity.ProductStock;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface ProductStockMapper {

    List<ProductStock> findAllStock();

    Long modifyAmountById(Integer productId, Integer step);
}

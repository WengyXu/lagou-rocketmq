package com.lagou.rocketmq.service;

import com.lagou.rocketmq.entity.ProductStock;
import com.lagou.rocketmq.mapper.ProductStockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ProductService {

    @Autowired
    private ProductStockMapper productStockMapper;

    public List<ProductStock> getStocks() {
        return productStockMapper.findAllStock();
    }

    public void increaseStock(Integer productId) {
        Long count = productStockMapper.modifyAmountById(productId, 1);
        if (count == 0) {
            log.error("增加库存失败");
        }
    }

    public void decreaseStock(Integer productId) {
        Long count = productStockMapper.modifyAmountById(productId, -1);
        if (count == 0) {
            log.error("减少库存失败");
        }
    }
}

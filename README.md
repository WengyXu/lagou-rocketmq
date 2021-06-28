## 作业说明

#### 课程介绍

> **第六阶段 分布式消息服务中间件进阶**
>
> 模块三 Apache消息中间件RocketMQ
>
> 本模块对市场上常用的开源消息中间件RocketMQ进行深度源码剖析、并对零拷贝原理、刷盘机制、消息幂等、死信队列、优化配置、动态扩容缩容、集群搭建等高级应用和原理进行讲解。



#### 作业内容

> 基于RocketMQ设计秒杀。
>
> 要求：
>
> 1.     秒杀商品LagouPhone，数量100个。
>
> 2.     秒杀商品不能超卖。
>
> 3.     抢购链接隐藏
>
> 4.     Nginx+Redis+RocketMQ+Tomcat+MySQL
>
> 提示： 
>
> 1. 作业需提交：
>
>    1、html的截图+搭建的过程+结果截图以文档或视频演示形式提供。
>
>    2、作业实现过程的代码。
>
>    ![输入图片说明](https://images.gitee.com/uploads/images/2020/0912/180511_fa9b566f_1712191.png "屏幕截图.png")
>
>    注意：
>
>    1、需要把搭建过程及配置用文档写清楚。
>
>    2、运行效果：展示商品销售的过程和结果，效果最好以视频形式演示。




#### 软件版本

```
centos-7.7
jdk-11.0.7
nginx-1.19.2
redis-5.0.5
rocketmq-4.7.1
mysql-5.7
```



#### 注意问题

1. rocketmq配置文件
    rocketmq课程PDF讲义中修改过的配置文件因为PDF中显示不开自动换行导致粘贴出来的文件也多了换行符，导致启动rocketmq服务时会报各种错误。因此需要手动将换行符删除，使shell命令完整。课程演示时没有问题是因为讲师使用的是md文件，所以只有PDF文件时复制粘贴后才会出现多余换行符的情况。

  据说notepad++可以解决这个问题。就是先将PDF内容粘贴到notepad++，再复制粘贴到md中。




2. rocketmq重试问题

   代码调试过程中发现在没有发起请求的情况下，也会触发下单处理，一开始觉得莫名其妙，后来才发现是rocketmq的失败重试机制。通过rocket-console管理页面查看重试队列中存放着消费失败的消息，所以当服务启动时，会不停的进行重试，从而出现调用接口的情况。可以在rocket-console管理页面中手动删除重试队列和topic，保证测试环境的状态。

   需要勾选RETRY后才可以看到重试队列。

   ![输入图片说明](https://images.gitee.com/uploads/images/2020/0925/015104_9acddb12_1712191.png "屏幕截图.png")




3.
`redisTemplate.opsForValue().increment()`和`redisTemplate.opsForValue().decrement()`





#### 实现步骤

* 安装软件

1. JDK

```shell
rpm -ivh /root/upload/jdk-11.0.7_linux-x64_bin.rpm
```
设置环境变量`/etc/profile`
```shell
export JAVA_HOME=/usr/java/jdk-11.0.7
export PATH=$PATH:$JAVA_HOME/bin
```
刷新设置
```shell
source /etc/profile
```



2. Nginx

安装相关类库

```shell
yum -y install gcc gcc-c++ pcre-devel zlib zlib-devel openssl openssl-devel
```

编译nginx

```shell
tar -zxvf /root/upload/nginx-1.19.2.tar.gz
cd /root/upload/nginx-1.19.2/
./configure --prefix=/opt/nginx
make && make install
cd /opt/nginx/sbin
./nginx -V
```

nginx配置文件`/opt/nginx-1.19.2/conf/nginx.conf`采用默认配置

启动nginx

```shell
./nginx
```

启动成功

![输入图片说明](https://images.gitee.com/uploads/images/2020/0916/014206_924be5f5_1712191.png "屏幕截图.png")

![输入图片说明](https://images.gitee.com/uploads/images/2020/0916/014711_27475f1e_1712191.png "屏幕截图.png")



3. Redis

由于redis是由C语言编写，编译时需要gcc，如果机器还没有安装的话，执行命令安装gcc。

```shell
yum -y install gcc
```

解压redis安装包

```shell
tar -zxvf /root/upload/redis-5.0.5.tar.gz
```

执行命令安装redis

```shell
cd /root/upload/redis-5.0.5
make install PREFIX=/opt/redis
```

将配置文件拷贝到sbin目录下

```shell
cp /root/upload/redis-5.0.5/redis.conf /opt/redis/bin/
```

修改配置文件`redis.conf`，解除本机ip访问的限制

```shell
bind 0.0.0.0
```

启动redis

```shell
cd /opt/redis/bin
./redis-server ./redis.conf &
```

启动成功

![输入图片说明](https://images.gitee.com/uploads/images/2020/0916/013149_f9284162_1712191.png "屏幕截图.png")



4. RocketMQ

配置程序包

```shell
yum -y install unzip
unzip rocketmq-all-4.7.1-bin-release.zip -d /opt
mv /opt/rocketmq-all-4.7.1-bin-release /opt/rocketmq
```
设置环境变量`/etc/profile`
```shell
export ROCKET_HOME=/opt/rocketmq
export PATH=$PATH:$ROCKET_HOME/bin
```
刷新设置
```shell
source /etc/profile
```
修改配置文件
`/opt/rocketmq/bin/runserver.sh`

修改内容

```shell
删除
UseCMSCompactAtFullCollection
UseParNewGC
UseConcMarkSweepGC
修改内存：
JAVA_OPT="${JAVA_OPT} -server -Xms256m -Xmx256m -Xmn128m -
XX:MetaspaceSize=64mm -XX:MaxMetaspaceSize=160mm"
-Xloggc修改为-Xlog:gc
```

修改结果

```shell
#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#==========================================================================
# Java Environment Setting
#==========================================================================
error_exit ()
{
echo "ERROR: $1 !!"
exit 1
}
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java
[ ! -e "$JAVA_HOME/bin/java" ] && error_exit "Please set the JAVA_HOME variable in your environment, We need java(x64)!"
export JAVA_HOME
export JAVA="$JAVA_HOME/bin/java"
export BASE_DIR=$(dirname $0)/..
export CLASSPATH=.:${BASE_DIR}/conf:${JAVA_HOME}/jre/lib/ext:${BASE_DIR}/lib/*
#===========================================================================================
# JVM Configuration
#===========================================================================================
JAVA_OPT="${JAVA_OPT} -server -Xms256m -Xmx256m -Xmn128m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=160m"
JAVA_OPT="${JAVA_OPT} -XX:CMSInitiatingOccupancyFraction=70 -XX:+CMSParallelRemarkEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSClassUnloadingEnabled -XX:SurvivorRatio=8"
JAVA_OPT="${JAVA_OPT} -verbose:gc -Xlog:gc:/dev/shm/rmq_srv_gc.log -XX:+PrintGCDetails"
JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow"
JAVA_OPT="${JAVA_OPT} -XX:-UseLargePages"
# JAVA_OPT="${JAVA_OPT} -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext:${BASE_DIR}/lib"
# JAVA_OPT="${JAVA_OPT} -Xdebug -Xrunjdwp:transport=dt_socket,address=9555,server=y,suspend=n"
JAVA_OPT="${JAVA_OPT} ${JAVA_OPT_EXT}"
JAVA_OPT="${JAVA_OPT} -cp ${CLASSPATH}"
$JAVA ${JAVA_OPT} $@
```

`/opt/rocketmq/bin/runbroker.sh`

修改内容

```shell
删除：
PrintGCDateStamps
PrintGCApplicationStoppedTime
PrintAdaptiveSizePolicy
UseGCLogFileRotation
NumberOfGCLogFiles=5
GCLogFileSize=30m
```

修改结果

```shell
#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#===========================================================================================
# Java Environment Setting
#===========================================================================================
error_exit ()
{
echo "ERROR: $1 !!"
exit 1
}
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java
[ ! -e "$JAVA_HOME/bin/java" ] && error_exit "Please set the JAVA_HOME variable in your environment, We need java(x64)!"
export JAVA_HOME
export JAVA="$JAVA_HOME/bin/java"
export BASE_DIR=$(dirname $0)/..
export CLASSPATH=.${JAVA_HOME}/jre/lib/ext:${BASE_DIR}/lib/*:${BASE_DIR}/conf:${CLASSPATH}
#===========================================================================================
# JVM Configuration
#===========================================================================================
JAVA_OPT="${JAVA_OPT} -server -Xms256m -Xmx256m -Xmn128m"
JAVA_OPT="${JAVA_OPT} -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:G1ReservePercent=25 -XX:InitiatingHeapOccupancyPercent=30 -XX:SoftRefLRUPolicyMSPerMB=0"
JAVA_OPT="${JAVA_OPT} -verbose:gc -Xloggc:/dev/shm/mq_gc_%p.log -XX:+PrintGCDetails"
JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow"
JAVA_OPT="${JAVA_OPT} -XX:+AlwaysPreTouch"
JAVA_OPT="${JAVA_OPT} -XX:MaxDirectMemorySize=15g"
JAVA_OPT="${JAVA_OPT} -XX:-UseLargePages -XX:-UseBiasedLocking"
#JAVA_OPT="${JAVA_OPT} -Xdebug -Xrunjdwp:transport=dt_socket,address=9555,server=y,suspend=n"
JAVA_OPT="${JAVA_OPT} ${JAVA_OPT_EXT}"
JAVA_OPT="${JAVA_OPT} -cp ${CLASSPATH}"
numactl --interleave=all pwd > /dev/null 2>&1
if [ $? -eq 0 ]
then
if [ -z "$RMQ_NUMA_NODE" ] ; then
numactl --interleave=all $JAVA ${JAVA_OPT} $@
else
numactl --cpunodebind=$RMQ_NUMA_NODE --membind=$RMQ_NUMA_NODE $JAVA
${JAVA_OPT} $@
fi
else
$JAVA ${JAVA_OPT} --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED $@
fi
```

`/opt/rocketmq/bin/tools.sh`

修改内容

```shell
删除：
JAVA_OPT="${JAVA_OPT} -Djava.ext.dirs=${BASE_DIR}/lib:${JAVA_HOME}/jre/lib/ext"
```

修改结果

```shell
#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#===========================================================================================
# Java Environment Setting
#===========================================================================================
error_exit ()
{
echo "ERROR: $1 !!"
exit 1
}
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java
[ ! -e "$JAVA_HOME/bin/java" ] && error_exit "Please set the JAVA_HOME variable in your environment, We need java(x64)!"
export JAVA_HOME
export JAVA="$JAVA_HOME/bin/java"
export BASE_DIR=$(dirname $0)/..
# export CLASSPATH=.:${BASE_DIR}/conf:${CLASSPATH}
export CLASSPATH=.${JAVA_HOME}/jre/lib/ext:${BASE_DIR}/lib/*:${BASE_DIR}/conf:${CLASSPATH}
#===========================================================================================
# JVM Configuration
#===========================================================================================
JAVA_OPT="${JAVA_OPT} -server -Xms256m -Xmx256m -Xmn256m -XX:PermSize=128m-XX:MaxPermSize=128m"
# JAVA_OPT="${JAVA_OPT} -Djava.ext.dirs=${BASE_DIR}/lib:${JAVA_HOME}/jre/lib/ext"
JAVA_OPT="${JAVA_OPT} -cp ${CLASSPATH}"
$JAVA ${JAVA_OPT} $@
```

启动NameServer

```shell
mqnamesrv
```

启动成功

![输入图片说明](https://images.gitee.com/uploads/images/2020/0916/024510_e333b246_1712191.png "屏幕截图.png")

查看启动日志

```shell
tail -f ~/logs/rocketmqlogs/namesrv.log
```
启动Broker

```shell
mqbroker -n localhost:9876
```

启动成功

![输入图片说明](https://images.gitee.com/uploads/images/2020/0916/025147_c09a5aec_1712191.png "屏幕截图.png")

查看启动日志

```shell
tail -f ~/logs/rocketmqlogs/broker.log
```

环境测试

  * 发送消息

设置环境变量
```shell
export NAMESRV_ADDR=localhost:9876
```

使用安装包的Demo发送消息
```shell
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Producer
```

![输入图片说明](https://images.gitee.com/uploads/images/2020/0916/031546_6cfca352_1712191.png "屏幕截图.png")



  * 接收消息

设置环境变量
```shell
export NAMESRV_ADDR=localhost:9876
```

接收消息
```shell
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Consumer
```

![输入图片说明](https://images.gitee.com/uploads/images/2020/0916/031657_43bb1ee8_1712191.png "屏幕截图.png")


* 关闭RocketMQ

关闭NameServer

```shell
mqshutdown namesrv
```

关闭Broker

```shell
mqshutdown broker
```



5. Tomcat

采用SpringBoot的jar包方式部署运行，已经内嵌了tomcat，所以省略了独立tomcat的安装。



6. MySQL

使用mac版MySQL，安装方式为dmg一键安装，所以省略。



#### 主要代码

获取隐藏接口地址

UrlPathController.java

```java
package com.lagou.rocketmq.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("buy/url")
public class UrlPathController {

    @GetMapping
    public Map getUrl(@RequestParam Integer productId) {

        String urlPath = "";

        if (LocalDateTime.now().compareTo(LocalDateTime.parse("2020-09-16T05:00:00")) >= 0) {
            urlPath = "product/" + productId + "/order";
        }

        Map<String, String> result = new HashMap();
        result.put("urlPath", urlPath);
        return result;
    }
}
```



生成订单

OrderController.java

```java
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
```



消费订单消息

OrderListener.java

```java
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
```



处理超时订单

TimeoutOrderListener.java

```java
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
```



处理订单

OrderService.java

```java
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
```



项目初始化，设置秒杀数量到redis

InitDataRunner.java

```java
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
```



Mapper

OrderMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.lagou.rocketmq.mapper.OrderInfoMapper">

    <insert id="add" useGeneratedKeys="true" keyProperty="orderId" parameterType="com.lagou.rocketmq.entity.OrderInfo">
        INSERT INTO order_info (user_id, product_id, price, amount, status) VALUES (#{userId}, #{productId}, #{price}, #{amount}, #{status})
    </insert>

    <update id="modifyStatusById">
        UPDATE order_info SET status=#{newStatus} WHERE order_id=#{orderId} AND status=#{oldStatus};
    </update>

    <select id="findStatusById" resultType="java.lang.Integer">
        SELECT
            status
        FROM
            order_info
        WHERE
            order_id = #{orderId}
    </select>
</mapper>
```

ProductStockMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.lagou.rocketmq.mapper.ProductStockMapper">

    <select id="findAllStock" resultType="com.lagou.rocketmq.entity.ProductStock">
        SELECT
            product_id, amount
        FROM
            product_stock
    </select>
</mapper>
```



前端页面

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>拉勾秒杀</title>
    <script src="https://cdn.jsdelivr.net/npm/jquery@3.5.1/dist/jquery.min.js"
            crossorigin="anonymous"></script>
</head>
<body>
<div align="center">
    <img src="../img/buy.png" height="1120" width="1919" usemap="#buttonmap"/>
    <map name="buttonmap" id="buttonmap">
        <area id="buyButton" shape="rect" coords="885,400,958,434" href="javascript:buy('1')"/>
    </map>
</div>
<script>

    function buy(productId) {
        var userId = 1;
        $.ajax({
            url: 'http://192.168.0.100:8080/buy/url',
            type: 'GET',
            dataType: 'json',
            data: {
                productId: productId
            },
            success: function (data) {
                if (data.urlPath == '') {
                    alert('活动尚未开始');
                    return false;
                }

                $.ajax({
                    url: 'http://192.168.0.100:8080/' + data.urlPath,
                    type: 'POST',
                    dataType: 'json',
                    data: {
                        userId: userId
                    },
                    success: function (result) {
                        if (result) {
                            alert('秒杀成功');
                        } else {
                            alert('秒杀失败');
                        }
                    }
                })
            }
        })
    }

</script>
</body>
</html>
```



配置文件

application.yml

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lagou-rocketmq?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: root
  redis:
    host: 192.168.0.111
    port: 6379
    database: 0
    timeout: 1000s
    jedis:
      pool:
        max-idle: 500
        min-idle: 50
        max-wait: -1
        max-active: -1
  cache:
    redis:
      time-to-live: -1

mybatis:
  type-aliases-package: com.lagou.rocketmq.entity
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

rocketmq:
  name-server: 192.168.0.111:9876
    group: myGroup
```

数据库

```sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for order_info
-- ----------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `order_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL,
  `product_id` int(11) unsigned NOT NULL,
  `price` int(11) unsigned NOT NULL,
  `amount` int(11) unsigned NOT NULL,
  `status` int(11) unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modify_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of order_info
-- ----------------------------
BEGIN;
INSERT INTO `order_info` VALUES (53, 1, 1, 300000, 1, 2, '2020-09-26 16:43:49', '2020-09-26 16:44:02');
COMMIT;

-- ----------------------------
-- Table structure for product_stock
-- ----------------------------
DROP TABLE IF EXISTS `product_stock`;
CREATE TABLE `product_stock` (
  `product_id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `product_name` varchar(20) NOT NULL,
  `amount` int(11) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of product_stock
-- ----------------------------
BEGIN;
INSERT INTO `product_stock` VALUES (1, 'LagouPhone', 100);
INSERT INTO `product_stock` VALUES (2, 'iPhone', 200);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
```




#### 测试流程

1. 确认初始化信息

   秒杀商品数量

2. 隐藏秒杀链接

   2.1. 未到秒杀时间无法获得秒杀链接，并提示活动未开始

   2.2. 已到秒杀时间获得链接，可以发起下单请求
   
3. 确认下单流程
   
   3.1. 检查redis库存
   
   3.2. 发送rocketmq消息
   
   3.3. 订单处理服务消费消息，生成相应订单
   
   3.4. 扣减库存
   
4. 订单超时处理
   
   4.1. 检查超时订单，设置订单失效状态
   
   4.2. 恢复库存
   
   

#### 视频讲解

![视频讲解](reference/md-videos/rocketmq.mp4)






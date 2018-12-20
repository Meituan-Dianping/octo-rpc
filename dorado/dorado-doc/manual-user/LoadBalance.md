
## Dorado 负载均衡

Dorado目前提供两种负载均衡策略
- 使用者也可以通过SPI扩展自己的LoadBalance
- 框架通过SPI接口前缀判断使用的负载均衡策略

| 算法 | 名称 | 说明 |
| ------ | ------ | ------ |
| RoundRobinLoadBalance | 带权的RoundRobin轮询算法 | 按照节点的权重依次选择每个节点 |
| RandomLoadBalance | 带权的随机轮询算法 | 按照节点的权重随机选择某个节点 |


### 1.配置方式

#### 1.1 xml文件方式

```xml
<bean id="clientProxy" class="com.meituan.dorado.config.service.spring.ReferenceBean" destroy-method="destroy">
  <!-- ...省略其他配置...  -->
  <property name="loadBalancePolicy" value="random"/>   <!-- 权重随机 -->
</bean>
```

#### 1.2 API方式

```java
 ReferenceConfig config = new ReferenceConfig();
 // ...省略其他配置...
 config.setLoadBalancePolicy("roundRobin");      <!-- 权重轮询 -->
```

## Dorado 服务节点预热

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Java编译器特性、服务缓存以及一些初始化操作，使服务一般在刚启动时响应较慢。所以服务端启动时如果不想在注册完成后，立即被调用端按照配置权重打入流量，则可以通过设置预热时间让流量慢慢进来，从而减少因服务节点启动带来的耗时长引发失败率可能变高的问题。

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;实现方式是服务端将预热时间写入注册中心，调用端在服务发现后根据预热时间和服务节点启动时间计算出当前时刻的权重，随着时间的增长线性增加该节点的权重，由此达到预热的效果。

>比如服务端服务节点设置预热时间为180s，权重为120；则调用端在服务端服务发布1min的时候权重为40，2min为80，超过3min之后权重就会恢复为120


使用方式是在服务端配置中设置预热时间，如下所示：

* API方式

```java
 ProviderConfig config = new ProviderConfig();
 // ...省略其他配置...
 config.setWarmup(180); // 默认单位为s
```

* XML方式
```xml
<bean id="serverPublisher" class="com.meituan.dorado.config.service.spring.ServiceBean">
    <!-- ...省略其他配置... -->
    <property name="warmup" value="180"/>
</bean>
```


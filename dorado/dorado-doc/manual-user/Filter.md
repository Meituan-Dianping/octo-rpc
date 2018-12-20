
## Dorado 自定义过滤器

- Dorado支持在Client或Server端创建自定义的过滤器并指定过滤器的优先级生成过滤器链路。
- Dorado支持全局生效的Filter和单个配置生效的Filter，具体使用方式见下面说明。

### 1.过滤器接口定义

```java
package com.meituan.dorado.rpc.handler.filter;
/**
 * 过滤器接口, 可自行实现
 * 
 * 全局生效Filter,通过SPI配置
 * 
 * 局部生效Filter,通过config配置
 * 
 * 注意：同一个Filter重复添加无效, 只执行一次
 */
@SPI
public interface Filter extends Role {
​
    RpcResult filter(RpcInvocation invocation, FilterHandler nextHandler) throws Throwable;
​
    /**
     * 值越大 优先级越高, 接口调用最先执行
     */
    int getPriority();
}
```


### 2.自定义过滤器

```java
public class AccessLogFilter implements Filter {
    private final static Logger logger = LoggerFactory.getLogger(AccessLogFilter.class);
​
    @Override
    public RpcResult filter(RpcInvocation invocation, FilterHandler nextHandler) throws Throwable {
        // 1. （可选）接口调用前的逻辑
        logger.info("AccessLogFilter request({}.{})", invocation.getServiceInterface().getName(), invocation.getMethod().getName());
        // 2. 重点，执行下一个Filter或真实的调用
        RpcResult result = nextHandler.handle(invocation);
        // 3. （可选）接口调用后的逻辑
        logger.info("AccessLogFilter response({}.{})", invocation.getServiceInterface().getName(), invocation.getMethod().getName());
        return result;
    }
​
    @Override
    public int getPriority() {
        // 4. 设置优先级，值越大 优先级越高, 该Filter越先执行，强烈不建议设置为最大值
        return 0;
    }
​
    @Override
    public RpcRole getRole() {
        // 5. 决定是调用端还是服务端的Filter，此处是服务端
        return RpcRole.PROVIDER;
    }
}
```

### 3.配置说明
- 支持全局生效和局部生效，但Filter不会重复添加，全局配置优先生效。根据你的Filter生效范围决定配置方式：

#### 3.1 全局生效Filter

新建扩展文件 META-INF/services/com.meituan.dorado.rpc.handler.filter.Filter

```
## 你的过滤器实现
com.meituan.dorado.rpc.handler.filter.AccessLogFilter
```

#### 3.2 针对生效Filter
- 局部生效，即某一个ReferenceConfig或ProviderConfig配置了，只在该调用端或服务端实例中生效，其他实例不会添加该Filter，前提是没在全局配置

以调用端举例：（服务端同）
```xml
<bean id="clientProxy" class="com.meituan.dorado.config.service.spring.ReferenceBean" destroy-method="destroy">
  <!-- ...省略其他配置... -->
  <property name="filters">
    <list>
      <bean class="com.meituan.dorado.rpc.handler.filter.AccessLogFilter" />
    </list>
  </property>
</bean>
```
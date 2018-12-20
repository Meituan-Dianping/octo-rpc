
## Dorado 方法级别超时

Dorado支持对方法级别的超时时间设置，默认使用统一的值

### 1. XML配置方式

```xml
<bean id="clientProxy" class="com.meituan.dorado.config.service.spring.ReferenceBean" destroy-method="destroy">
  <property name="serviceInterface" value="com.meituan.dorado.test.thrift.api.Echo"/>
  <property name="appkey" value="com.meituan.octo.dorado.client"/>
  <property name="remoteAppkey" value="com.meituan.octo.dorado.server"/>
  <property name="methodTimeout">
    <map>
      <entry key="echo" value="2000"></entry>
    </map>
  </property>
</bean>
```

### 2.API配置方式

```java
 ReferenceConfig<HelloService.Iface> config = new ReferenceConfig<>();
 config.setAppkey("com.meituan.octo.dorado.client");
 config.setRemoteAppkey("com.meituan.octo.dorado.server");
 config.setServiceInterface(HelloService.class);
 Map<String, Integer> methodTimeout = new HashMap<>();
 methodTimeout.put("sayHello", 1000);
 methodTimeout.put("sayBye", 2000);
 config.setMethodTimeout(methodTimeout);
```






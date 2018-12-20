
## Dorado 直连

为了方便进行服务测试和问题定位，Dorado 支持指定地址进行调用的场景。使用方式如下，设置直连地址即可

```xml
<bean id="clientProxy" class="com.meituan.dorado.config.service.spring.ReferenceBean" destroy-method="destroy">
  <property name="serviceInterface" value="com.meituan.dorado.test.thrift.api.Echo"/>
  <property name="directConnAddress" value="localhost:9001"/>   <!--  配置服务端的直连地址, 可配置多个，如 ip1:port,ip2:port  -->
  <property name="appkey" value="com.meituan.octo.dorado.client"/>
  <property name="remoteAppkey" value="com.meituan.octo.dorado.server"/>
</bean>
```

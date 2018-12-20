
## Dorado 单端口多服务

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;通常RPC服务都是一个端口一个服务，而实际应用中，业务方可能要提供大量的接口服务，每个服务都配置一个端口，每个端口都会初始化一套IO线程资源（后续会考虑IO线程共用），除了会创建大量线程外还会增加内存占用，尤其是Netty使用堆外内存，堆外内存问题不是通过dump就能快速排查出来的。线上就曾遇到过业务启用了34个端口导致堆外内存泄漏问题，虽然最后发现是[Netty4.1.26的Bug](https://github.com/netty/netty/pull/8160)，4.1.29已修复，但为了避免类似问题我们更推荐大家在有多个接口时使用单端口多服务。

下面给出使用单端口多服务的xml文件配置，主要通过 ***serviceConfigList*** 参数配置多个serviceConfig，每个serviceConfig代表一个服务，同时可以针对每个服务配置相关的参数，例如线程池，超时设置等。

### 1.配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="helloServiceProcessor" class="com.meituan.dorado.test.thrift.api.HelloServiceImpl"></bean>
    <bean id="echoServiceProcessor" class="com.meituan.dorado.test.thrift.api.EchoImpl"></bean>

    <bean id="serverPublisher" class="com.meituan.dorado.config.service.spring.ServiceBean" destroy-method="destroy">
        <property name="appkey" value="com.meituan.octo.dorado.server"/>
        <property name="port" value="9001"/>
        <property name="registry" value="mock"/>
        <!-- 多个接口配置 -->
        <property name="serviceConfigList">
            <list>
                <ref bean="helloServiceConfig"/>
                <ref bean="echoServiceConfig"/>
            </list>
        </property>
    </bean>

    <!-- HelloService接口配置 -->
    <bean id="helloServiceConfig" class="com.meituan.dorado.config.service.ServiceConfig">
        <property name="serviceInterface" value="com.meituan.dorado.test.thrift.api.HelloService"/>
        <property name="serviceImpl" ref="helloServiceProcessor"/>
        <property name="bizMaxWorkerThreadCount" value="256"/>
    </bean>

    <!-- Echo接口配置 -->
    <bean id="echoServiceConfig" class="com.meituan.dorado.config.service.ServiceConfig">
        <property name="serviceInterface" value="com.meituan.dorado.test.thrift.api.Echo"/>
        <property name="serviceImpl" ref="echoServiceProcessor"/>
        <property name="bizMaxWorkerThreadCount" value="256"/>
    </bean>
</beans>
```
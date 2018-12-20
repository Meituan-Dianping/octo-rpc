
# Dorado 注册中心

OCTO服务治理体系的服务是以Appkey命名，每个服务都必须有一个唯一的Appkey来标识你的服务，比如*com.meituan.{应用名}.{模块名}.{服务名}*，
即OCTO体系的服务注册与发现都基于Appkey进行的。

Dorado 目前支持三种服务注册方式，分别是 MNS、Zookeeper、Mock

## 1.服务注册/发现方式

### 1.1 MNS(OCTO-NS)

MNS是本次开源的OCTO中的命名服务组件(OCTO-NS)，作为注册中心，框架服务节点只需与本地Agent交互，减少网络开销。
具体的实现原理和使用方式详见[OCTO-NS](https://github.com/Meituan-Dianping/octo-ns)

### 1.2 Zookeeper

Dorado同时支持通过Zookeeper来进行服务的注册与发现，使用该模式前需要确保配置相应的Zookeeper集群。
因为OCTO-NS底层也是ZK，只要共用了一个Zookeeper集群，ZK与MNS模块就可以混合使用，便于使用者进行切换迁移。比如：你的服务端务注册到了OCTO-NS，调用端直接使用ZK可以正常的进行服务发现。

### 1.3 Mock

当同时缺少MNS和Zookeeper环境时，服务端可以通过mock的形式进行伪注册，然后通过直连的方式访问服务端。

## 2. 使用说明

### 2.2 XML配置方式

* MNS

```xml
<bean id="serverPublisher" class="com.meituan.dorado.config.service.spring.ServiceBean">
    <!-- ...省略其他配置... -->
    <property name="registry" value="mns"/>                  <!-- 使用OCTO-NS 做注册注册 -->
</bean>
```
```xml
<bean id="clientProxy" class="com.meituan.dorado.config.service.spring.ReferenceBean">
    <!-- ...省略其他配置... -->
    <property name="registry" value="mns"/>                  <!-- 使用OCTO-NS 做服务发现 -->
</bean>
```
* Zookeeper

```xml
<bean id="serverPublisher" class="com.meituan.dorado.config.service.spring.ServiceBean">
    <!-- ...省略其他配置... -->
    <property name="registry" value="zookeeper://ip:port"/>  <!-- 使用Zookeeper 做服务注册 -->
</bean>
```
```xml
<bean id="clientProxy" class="com.meituan.dorado.config.service.spring.ReferenceBean">
    <!-- ...省略其他配置... -->
    <property name="registry" value="zookeeper://ip:port"/> <!-- 使用Zookeeper 做服务发现 -->
</bean>
```

* Mock

```xml
<bean id="serverPublisher" class="com.meituan.dorado.config.service.spring.ServiceBean">
    <!-- ...省略其他配置... -->
    <property name="registry" value="mock"/>                <!-- mock方式, 伪注册 -->
</bean>
```
```xml
<bean id="clientProxy" class="com.meituan.dorado.config.service.spring.ReferenceBean">
    <!-- ...省略其他配置... -->
    <property name="directConnAddress" value="ip:port,ip:port"/> <!-- 直连配置, 可配置多个节点 -->
    <property name="registry" value="mock"/>                <!-- mock方式, 直连访问时，可不配置-->
</bean>
```

### 2.1 API方式

```java
 // 服务端
 ProviderConfig config = new ProviderConfig();
 // ...省略其他配置...
 // config.setRegistry("mns");
```

```java
 // 调用端
 ReferenceConfig config = new ReferenceConfig();
 // ...省略其他配置...
 // config.setRegistry("mns");
```
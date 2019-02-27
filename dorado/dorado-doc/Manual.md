# Dorado使用说明手册

本文档将演示如何应用 Dorado 进行服务的发布和订阅，更多示例参见工程中dorado-demo目录

## 1.创建工程

- 首先需要安装 JDK7+ 和 Maven 3+ 并配置好环境
- 新建一个Maven 工程，pom引入 Dorado 的依赖
   - 获取Dorado Jar见 [Dorado构建Jar](manual-developer/Compile.md)

## 2.快速入门

>OCTO服务治理体系的服务是以*Appkey*命名，每个服务都必须有一个唯一的*Appkey*来标识你的服务，比如 ***com.meituan.{应用名}.{模块名}.{服务名}*** ，
 即OCTO体系的服务注册与发现都是基于Appkey进行的。
 
Dorado的默认使用Thrift作为Body协议，下面的Demo基于Thrift进行介绍：

### 2.1 服务定义

#### 2.1.1 定义Thrift IDL

>更多Thrift IDL语法见：[Thrift指南](manual-thrift/ThriftSpecification.md)
```
namespace java com.meituan.dorado.demo.thrift.api
service HelloService
{  
    string sayHello(1:string username)
    string sayBye(1:string username)
}
```

#### 2.1.2 Thrift文件生成IDL类

>自动生成Java源码方式，见[Thrift指南](manual-thrift/ThriftSpecification.md)

- Dorado使用libthrift **0.9.3**的版本
- 如果使用其他版本的libthrift且出现了不兼容情况，可以对dorado-protocol-octo模块的 ***octoProtocolHeader.thrift*** 使用对应的libthrift版本重新编译生成

运行Demo可以直接使用：[HelloService.java](manual-thrift/api/HelloService.java)

#### 2.1.3 提供接口对应的实现类

```java
public class HelloServiceImpl implements HelloService.Iface {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }
    @Override
    public String sayBye(String username) throws TException {
        return "Bye " + username;
    }
}
```

### 2.2 服务发布

目前Dorado支持XML文件配置和API方式进行服务的发布

#### 2.2.1 XML配置方式

##### 2.2.1.1 定义XML文件

定义xml文件并放在resources目录下

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean id="helloServiceProcessor" class="com.meituan.dorado.demo.thrift.api.HelloServiceAsyncImpl"></bean>
	<bean id="helloServiceConfig" class="com.meituan.dorado.config.service.ServiceConfig">
        <property name="serviceInterface" value="com.meituan.dorado.demo.thrift.api.HelloService"/>   <!-- 服务接口 -->
        <property name="serviceImpl" ref="helloServiceProcessor"/>            <!-- 服务实现类 -->
    </bean>
    <bean id="serverPublisher" class="com.meituan.dorado.config.service.spring.ServiceBean" destroy-method="destroy">
        <property name="appkey" value="com.meituan.octo.dorado.server"/>      <!-- 服务appkey -->
        <property name="port" value="9001"/>                        <!-- 服务端口号 -->
        <property name="registry" value="mock"/>                    <!-- 服务注册 地址 -->
        <property name="serviceConfig" ref="helloServiceConfig"/>   <!-- 服务接口配置 -->
    </bean>
</beans>
```

##### 2.2.1.2 启动服务

```java
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ThriftProvider {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext beanFactory = new ClassPathXmlApplicationContext("xx/server.xml");
    }
}
```

#### 2.2.2 API方式

```java
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.config.service.ServiceConfig;
import com.meituan.dorado.demo.thrift.api.HelloService;
import com.meituan.dorado.demo.thrift.api.HelloServiceImpl;

public class QuickStartServer {
    public static void main(String[] args) {
        ServiceConfig<HelloService.Iface> serviceConfig = new ServiceConfig<>();
        serviceConfig.setServiceImpl(new HelloServiceImpl());         // 服务实现类
        serviceConfig.setServiceInterface(HelloService.class);        // 设置服务名

        ProviderConfig config = new ProviderConfig();
        config.setAppkey("com.meituan.octo.dorado.server");           // 服务appkey
        config.setServiceConfig(serviceConfig);                       // 服务接口配置类           
        config.setRegistry("mns");                                    // 服务注册  [OCTO-NS]服务命名组件方式进行注册
        config.setPort(9001);                                         // 服务端口号
        config.init();                                                // 启动服务
    }
}
```

### 2.3 服务调用

目前Dorado支持XML文件配置和API方式进行服务访问调用

#### 2.3.1 XML配置方式

##### 2.3.1.1 定义XML文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">
    <bean id="helloService" class="com.meituan.dorado.config.service.spring.ReferenceBean">
        <property name="appkey" value="com.meituan.octo.dorado.client"/>       <!-- 调用端appkey -->
        <property name="remoteAppkey" value="com.meituan.octo.dorado.server"/> <!-- 服务端appkey -->
        <property name="serviceInterface" value="com.meituan.dorado.demo.thrift.api.HelloService"/>        <!-- 服务接口类 -->
        <property name="registry" value="mock"/>      <!-- 服务发现的zk地址 -->
    </bean>
</beans>
```

##### 2.3.1.2 XML方式调用

```java
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Client {
    private static HelloService.Iface client;

    public static void main(String[] args) {
        BeanFactory beanFactory = new ClassPathXmlApplicationContext("client.xml");
        client = (HelloService.Iface) beanFactory.getBean("clientProxy");

        // 同步   
        String result = client.sayHello("dorado");
        
        
        // 异步
        ResponseFuture<String> future = AsyncContext.getContext().asyncCall(new Callable<String>() {
          	@Override
          	public String call() throws Exception {
            	    return client.sayHello("dorado async");
          	}
        });
        String result = future.get();
        
        // 回调
        ResponseFuture<String> future2 = AsyncContext.getContext().asyncCall(new Callable<String>() {
          @Override
          public String call() throws Exception {
            return userservice.sayHello("dorado async callback");
          }
        });
        
        future2.setCallback(new ResponseCallback<String>() {
          @Override
          public void onComplete(String result) {
              System.out.println(result);
          }
          @Override
          public void onError(Throwable e) {
              System.out.println("Exception," + e.getMessage());
          }
        });
    }
}
```

#### 2.3.2 API方式

```java
import com.meituan.dorado.config.service.ReferenceConfig;
import com.meituan.dorado.demo.thrift.api.HelloService;
import org.junit.Assert;

public class QuickStartClient {
    private static HelloService.Iface client;

    public static void main(String[] args) {
        ReferenceConfig<HelloService.Iface> config = new ReferenceConfig<>();
        config.setAppkey("com.meituan.octo.dorado.client");
        config.setRemoteAppkey("com.meituan.octo.dorado.server");
        config.setServiceInterface(HelloService.class);
        config.setRegistry("zookeeper://ip:port");

        client = config.get();
        String result = client.sayHello("meituan");       
    }
}
```

## 3. 参数配置

Dorado通过配置调用端和服务端的多个参数来支持不同的实现，具体详见 **[参数配置](manual-user/Config.md)** 

## 4. 特性支持

**[单端口多服务](manual-user/Port2MultiService.md)** 

**[埋点监控](manual-user/Trace.md)** 
 
**[协议支持](manual-user/Protocol.md)** 

**[服务注册发现](manual-user/Registry.md)** 

**[自定义过滤器](manual-user/Filter.md)** 

**[服务节点预热](manual-user/WeightWarmUp.md)** 

**[自定义负载均衡](manual-user/LoadBalance.md)** 

**[直连访问](manual-user/DirectInvoke.md)** 

**[方法级别超时](manual-user/MethodTimeout.md)**

**[优雅关闭](manual-user/ShutdownGracefully.md)** 

## 5. 版本说明

**[Dorado 版本管理](manual-developer/Version.md)**

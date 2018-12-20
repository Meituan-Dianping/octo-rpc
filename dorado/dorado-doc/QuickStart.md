
# Dorado 快速使用指南
**快速使用指南，便于使用者快速了解Dorado的应用，更多详细内容可以阅读[Dorado使用说明手册](Manual.md)**

## 1. 运行准备
### 1.1 提前安装：JDK 7+、Maven 3+ 并配置好环境

### 1.2 下载仓库代码准备Dorado依赖
```
  git clone https://github.com/Meituan-Dianping/octo-rpc.git octo-rpc
```

## 2. 运行（二选一）
以下两种方式选择一种快速运行 dorado
### 2.1 运行Dorado项目Demo

#### 2.1.1 IDE导入dorado 完整项目
**注意：Dorado和Whale(C++框架)在一个仓库，选择dorado目录导入即可**

#### 2.1.2 进入derado-demo模块

#### 2.1.3 选择一个demo, 如simple package下

#### 2.1.4 run ThriftProvider main

#### 2.1.5 run ThriftConsumer main
在ThriftConsumer控制台将看到服务端的返回

### 2.2 独立Project运行
#### 2.2.1 install dorado到本地仓库
切换到dorado目录

```
cd octo-rpc/dorado
```

本地install

```
mvn clean install -Dmaven.test.skip=true
```

**注意：dorado-registry-mns、dorado-trace-cat模块因需要mns和cat的依赖，若构建失败可忽略**

- 默认dorado包未包含需以上模块，若需要使用见[源码编译说明](manual-developer/Compile.md)

- dorado-registry-mns是OCTO-NS注册中心的集成模块，若使用请到[OCTO-NS](https://github.com/Meituan-Dianping/octo-ns/tree/master/mns-invoker)获取依赖mns-invoker依赖（需要有MNS注册服务）

- dorado-trace-cat是Cat应用监控的集成，若使用请到[Cat](https://github.com/dianping/cat)获取依赖（需要有Cat服务）

#### 2.2.2 创建maven工程

pom.xml添加dorado依赖
```xml
<dependency>
    <groupId>com.meituan.octo</groupId>
    <artifactId>dorado</artifactId>
    <version>1.0.0</version>
</dependency>
```
#### 2.2.3 创建接口类 和 实现类

将接口[HelloService.java](manual-thrift/api/HelloService.java) 和 实现类[HelloServiceImpl.java](manual-thrift/api/HelloServiceImpl.java)
拷贝到你的工程中，注意包路径。
**说明：调用端有接口类就行，服务端需要接口类和实现类。**

#### 2.2.4 创建服务端启动类，发布服务

下面的代码通过mock的方式发布实现HelloService接口的服务,**注意**需要对服务端口号、服务于appkey、服务注册地址、服务接口以及服务实现类进行配置，最后通过init方法发布服务
```java
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.config.service.ServiceConfig;
import com.meituan.dorado.demo.thrift.api.HelloService;
import com.meituan.dorado.demo.thrift.api.HelloServiceImpl;

public class QuickStartServer {
    public static void main(String[] args) {
        ServiceConfig<HelloService.Iface> serviceConfig = new ServiceConfig<>();
        serviceConfig.setServiceInterface(HelloService.class);        // 服务接口
        serviceConfig.setServiceImpl(new HelloServiceImpl());         // 服务实现类
        
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setAppkey("com.meituan.octo.dorado.server");           // 服务appkey
        providerConfig.setServiceConfig(serviceConfig);                       // 服务接口类
        providerConfig.setRegistry("mock");                                   // 服务注册, mock伪注册中心
        providerConfig.setPort(9001);                                         // 服务端口号
        providerConfig.init();                                                // 启动服务
    }
}
```

#### 2.2.5 创建调用端启动类，订阅服务，发起调用

下面的代码通过直连方式访问mock发布的服务

```java
import com.meituan.dorado.config.service.ReferenceConfig;
import com.meituan.dorado.demo.thrift.api.HelloService;
import org.apache.thrift.TException;
import org.junit.Assert;

public class QuickStartClient {
    public static void main(String[] args) {
        ReferenceConfig<HelloService.Iface> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setAppkey("com.meituan.octo.dorado.client");           //调用端appkey
        referenceConfig.setRemoteAppkey("com.meituan.octo.dorado.server");     //服务端appkey
        referenceConfig.setServiceInterface(HelloService.class);               //服务接口
        referenceConfig.setRegistry("mock");                                   //服务发现, mock伪注册中心
        referenceConfig.setDirectConnAddress("localhost:9001");                //直连访问
        
        HelloService.Iface client = referenceConfig.get();
        try {
            String result = client.sayHello("OCTO");
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```





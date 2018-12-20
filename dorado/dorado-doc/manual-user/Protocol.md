
# Dorado 通信协议

## 1. 支持的协议
### 1.1 OCTO协议 + Thrift协议
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;美团内部服务间使用OCTO私有协议进行通信，OCTO协议具备良好的扩展性，如下是协议格式：

| 2Byte | 1Byte | 1Byte | 4Byte | 2Byte | header length Byte | body length Byte | 4Byte(可选) |
| --- | --- | --- | --- | --- | --- | --- | --- | 
| magic | version | protocol | total length | header length | header | body | checksum |
| 0xABBA| | | | |  |  | 校验码 |

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;OCTO协议比较灵活的是消息头和消息体，消息头主要是透传一些服务参数信息（比如：TraceInfo），还可以扩展其他数据，为丰富服务治理功能带来了极大便利；消息体是编码后的请求数据，任何编解码协议都可以在Body中进行传输，目前Dorado默认以Thrift作为Body协议，使用者也可以扩展其他编解码协议进行扩展。

### 1.2 Thrift协议
 
 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado同时支持原生Thrift协议，且可以和OCTO协议混合使用，调用端在服务发现的时候会根据节点注册信息来判断使用什么协议；服务端则可以在解码时判断来源请求协议。
 所以Dorado可以和任何原生Thrift服务互通，无论是调用端还是服务端，不管你使用什么语言只要是Thrift协议都可以和Dorado通信。

---------------
## 2 使用说明

### 2.1 配置说明
#### 2.1.1 配置字段: 
- ***protocol***

指Body协议，默认thrift

- ***remoteOctoProtocol***

默认false, 用于直连时不能指定发送OCTO协议（若是服务发现会自动判断）

#### 2.1.2 服务端 和 调用端配置
- 服务端

```xml
<bean id="serverPublisher" class="com.meituan.dorado.config.service.spring.ServiceBean">
    <!-- ...省略其他配置... -->
    <property name="protocol" value="thrift"/>   <!-- 默认值，可不配置 -->
</bean>
```

- 调用端

```xml
<bean id="clientProxy" class="com.meituan.dorado.config.service.spring.ReferenceBean">
    <!-- ...省略其他配置... -->
    <property name="protocol" value="thrift"/>   <!-- 默认值，可不配置 -->
</bean>
```


### 2.2 协议发送说明
- 若服务端支持OCTO协议，则发送OCTO协议

- 若使用直连访问，为了更好的兼容默认是发送Thrift协议，需要发送OCTO协议则配置 ***remoteOctoProtocol*** 为true
 



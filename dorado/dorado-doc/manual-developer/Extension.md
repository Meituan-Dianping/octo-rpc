
## Dorado 扩展点

与业界大多Java服务的扩展机制是一样的，Dorado使用JDK内置的服务提供发现机制。但Dorado在获取扩展点实现上进行了一些改造，比如可以按照名字或者角色（调用端或服务端）获取扩展点实现。所有的SPI接口通过@SPI注解来识别，使用者实现SPI接口并按照Java SPI规范配置即可使用。

Dorado目前提供了以下的SPI扩展点 ：
>标注不支持多个实现的扩展点要注意，使用中只能选择一种实现

| SPI接口 | 说明 | 默认实现 | 是否可以同时支持多个实现 |
| --- | --- | --- | --- |
| com.meituan.dorado.registry.RegistryFactory | 注册中心 | MnsRegistryFactory(集成开源组件MNS)、ZookeeperRegistryFactory、MockRegistryFactory(用于无注册中心服务时测试) | 支持，通过配置选择使用 |
| com.meituan.dorado.cluster.Cluster | 请求容错策略 | FailoverCluster(失败直接返回)、FailbackCluster(失败后重试其他节点)、FailOverCluster(失败重发) | 支持，通过配置选择使用 |
| com.meituan.dorado.cluster.LoadBalance | 负载均衡策略 | RandomLoadBalance(随机权重)、RoundRobinLoadBalance(加权轮询) | 支持，通过配置选择使用 |
| com.meituan.dorado.cluster.Router | 路由策略 | NoneRouter ；路由一般跟节点部署强相关，Dorado暂未提供默认实现，若使用OCTO-NS获取则是经过路由的服务列表| 支持，通过配置选择使用 |
| com.meituan.dorado.rpc.handler.InvokeHandler | 服务请求处理类 | DoradoInvokerInvokeHandler(调用端请求处理)、DoradoProviderInvokeHandler(服务端请求处理) | 支持，通过角色获取，但服务端或调用端只能有一个 |
| com.meituan.dorado.rpc.handler.HeartBeatInvokeHandler | 心跳请求处理 | ScannerHeartBeatInvokeHandler (OCTO-Scanner心跳处理)| 支持，由HandlerFactory按名字选择使用|
| com.meituan.dorado.rpc.handler.HandlerFactory | 根据消息类型获取InvokeHandler和.HeartBeatInvokeHandler | DoradoHandlerFactory | 不支持 |
| com.meituan.dorado.rpc.handler.filter.Filter | 过滤器实现 | DoradoInvokerTraceFilter(用于调用端埋点)、DoradoProviderTraceFilter(用于服务端埋点) | 支持，共同使用 |
| com.meituan.dorado.rpc.handler.http.HttpInvokeHandler | Http接口测试 | DoradoHttpInvokeHandler | 不支持 |
| com.meituan.dorado.check.http.HttpCheckHandler | 服务自检 | DoradoHttpCheckHandler |不支持 |
| com.meituan.dorado.codec.Codec | 编解码 | OctoCodec |不支持 |
| com.meituan.dorado.transport.LengthDecoder | 协议长度解码 | DoradoLengthDecoder | 不支持 |
| com.meituan.dorado.transport.ClientFactory | 调用端传输模块 | NettyClientFactory | 不支持 |
| com.meituan.dorado.transport.ServerFactory | 服务端传输模块 | NettyServerFactory | 不支持 |
| com.meituan.dorado.transport.http.HttpServerFactory | Http服务 | NettyHttpServerFactory | 不支持 |
| com.meituan.dorado.trace.InvokeTrace | 数据埋点 | CatInvokeTrace(集成开源组件Cat) | 支持，共同使用 |
| com.meituan.dorado.rpc.proxy.ProxyFactory | 代理类 | JdkProxyFactory | 不支持|
     
                                                                          


# 1.背景描述
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;随着SOA、微服务架构广泛应用，分布式服务框架的意义愈发重要，为了满足各种服务治理需求，服务框架的周边功能不断衍生，但成百上千的业务线如果没有一套统一的分布式服务框架，服务治理也是很难推进的。

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;OCTO就是致力于为所有业务提供统一的服务通信框架和服务治理系统，Dorado则是OCTO生态中的一员，为Java服务提供具备治理功能的RPC通信框架（C++框架: [Whale](https://github.com/Meituan-Dianping/octo-rpc/tree/master/whale)）。美团内部服务之间使用OCTO协议进行通信，默认支持Thrift，便于不同语言服务之间互通。框架提供了服务注册/发现、路由、负载均衡、容错等丰富功能来满足服务治理需要，目前内部Java框架生产环境有8千+应用，支撑了每天6千亿级+的调用量。

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado的目标是构建一套更易用、更高效、更可靠，具有良好扩展性的分布式通信框架。

# 2.框架介绍
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;首先来看一下美团分布式框架的部署模式，为方便理解如下是一个简单的架构图：紫色方块是注册中心[OCTO-NS](https://github.com/Meituan-Dianping/octo-ns)，各节点不直接与注册中心服务交互，而是与本地Agent交互来进行服务注册发现，减少网络延迟和对注册中心的压力。绿色方块是监控跟踪服务，Dorado开源版本使用[Cat](https://github.com/dianping/cat)作为监控上报。说明一下，这里是以美团开源组件的应用作为示例，OCTO-NS(MNS)和Cat均是Dorado的扩展支持模块。

![ServiceArchitecture](https://github.com/Meituan-Dianping/octo-rpc/blob/master/dorado/dorado-doc/img/ServiceArchitecture.svg)

简单介绍下Dorado端对端交互机制：

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;OCTO服务治理体系的服务是以Appkey命名，每个服务都必须有一个唯一的Appkey来标识你的服务，比如*com.meituan.{应用名}.{模块名}.{服务名}*（使用者可以按照自己的规范定义），所以OCTO体系的注册发现都是基于Appkey进行的。

- **Provider服务端**

  服务端通过注册模块向注册中心发起注册，接收[OCTO-Scanner](https://github.com/Meituan-Dianping/octo-ns/tree/master/scanner)的健康检查心跳数据，确认服务端节点的可用性，如果发现节点异常则会修改节点状态为Dead，调用端会自动将该节点摘除；

  接收到调用端的请求后，会通过InvokeTrace接口向监控中心上报数据；

  服务节点可以通过[OCTO-Portal](https://github.com/Meituan-Dianping/octo-portal)管理，进行节点禁用、启用、权重调整和删除的操作。

- **Invoker调用端**

  调用端通过注册中心获取服务列表并进行过滤筛选得到有效节点（校验状态、协议、服务接口等）;

  从获取的有效节点中通过路由和负载均衡向某一个服务节点发起请求，请求结束后向监控中心上报数据；

  监听注册节点的变化，当有变更则更新当前的服务节点，若状态变更则直接摘除并清理该节点资源。

## 2.1 框架特点

- **模块化，易扩展**

   各个模块拆分实现，提供很多扩展点，可以根据需要扩展自己的实现模块，打造出适合自己服务的框架；

- **微内核，可插拔**

   核心模块不会依赖于任何具体扩展，每个实现模块都可以自由组合，按需引入；

- **实现简洁，链路清晰**

   框架设计简洁，主干调用链路清晰；

- **高性能，高可用**

  默认提供Netty作为网络传输框架和Thrift协议，在目前的Java框架中表现较优，服务端1K数据压测QPS稳定在**12W+**；

  服务端节点异常自动降级探测，提升调用端服务的可用性。
  
## 2.2 框架设计
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado的设计原则是尽可能简洁地实现治理功能丰富、高性能、高可用、可扩展的RPC通信框架。如图，左右侧分别是调用端（Invoker）和服务端（Provider），主干调用链路由图中绿色线条所示，调用端自上而下，服务端自下而上的逐层调用。接口调用链路和层级简洁清晰，降低了开发者的入门成本。
   
   ![FrameworkDesign](https://github.com/Meituan-Dianping/octo-rpc/blob/master/dorado/dorado-doc/img/FrameworkDesign.svg)
   
## 2.3 模块分包

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado的核心模块是dorado-core，包含所有功能模块的接口定义，和各功能模块的通用实现，基于该模块可以随意实现其扩展并自由组合得到你想要的功能包。如图绿色模块是Dorado开源版本针对注册中心、协议、网络传输、数据监控提供的默认扩展，以后还会根据需要继续扩展更多模块。
   
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;扩展模块都是可插拔的，你可以选择二次开发或实现自己的模块进行替换，简单便捷，无需考虑核心链路的代码改造，当然除了图中示例的模块，你还可以基于Dorado的其它SPI接口进行扩展。具体扩展接口见功能介绍中的SPI扩展点。
   
   ![FrameworkPackage](https://github.com/Meituan-Dianping/octo-rpc/blob/master/dorado/dorado-doc/img/FrameworkPackage.svg)
   
## 2.4 功能介绍
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;以下对Dorado部分功能做简要介绍：

### 2.4.1 服务注册发现
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado提供了两种服务注册发现的模块，分别集成了OCTO-NS和Zookeeper，另外提供了一个mock模块用于在没有注册中心服务时直连测试使用。

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;无论使用OCTO-NS还是Zookeeper进行注册，服务节点均可以在OCTO-Portal进行管理；

- **MNS**：OCTO-NS命名服务

   在前面介绍中，给出的架构图即使用MNS作为注册中心，作为美团的内部实践，也是我们建议的使用方式。框架服务节点只需与本地Agent交互，减少网络开销。

- **Zookeeper**

   如果你没有OCTO-NS服务，没关系，只要有部署好的Zookeeper服务，配置地址后就可以直接使用Dorado。而且因为OCTO-NS底层也是ZK，只要共用了一个Zookeeper集群，ZK与MNS模块就可以混合使用，便于使用者进行切换迁移。比如：你的服务端务注册到了OCTO-NS，调用端直接使用ZK可以正常的进行服务发现。

   当然若你的节点数量非常多，那我们还是推荐使用MNS作为注册中心。

- **Mock**

   如果你没有MNS也没有Zookeeper，但又想使用Dorado，可以将注册配置设置为mock，调用端配置直连，就可以简单的发起自己的RPC调用。

[服务注册发现说明](https://github.com/Meituan-Dianping/octo-rpc/tree/master/dorado/dorado-doc/manual-user/Registry.md)

### 2.4.2 协议支持
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;美团内部服务间使用OCTO私有协议进行通信，OCTO协议具备良好的扩展性，如下是二进制协议格式：

| 2Byte | 1Byte | 1Byte | 4Byte | 2Byte | header length Byte | body length Byte | 4Byte(可选) |
| --- | --- | --- | --- | --- | --- | --- | --- | 
| magic | version | protocol | total length | header length | header | body | checksum |
| 0xABBA| | | | |  |  | 校验码 |

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;OCTO协议比较灵活的是消息头和消息体，消息头主要是透传一些服务参数信息（比如：TraceInfo），还可以扩展其他数据，为丰富服务治理功能带来了极大便利；消息体是编码后的请求数据，任何编解码协议都可以在Body中进行传输，目前Dorado默认以Thrift作为Body协议，使用者也可以扩展其他编解码协议进行扩展。

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado同时支持OCTO协议和原生Thrift协议且可以混合使用，调用端在服务发现的时候会根据节点注册信息来判断使用什么协议；服务端可以在解码时判断来源请求协议，所以Dorado可以和任何语言的原生Thrift服务互通。

[协议支持说明](https://github.com/Meituan-Dianping/octo-rpc/tree/master/dorado/dorado-doc/manual-user/Protocol.md)

### 2.4.3 网络通信
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;美团内部早期使用ThriftIO进行通信，但ThriftIO有以下几个问题：
   
   > 1. 同步场景下是BIO，与每个节点都要创建很多连接维护一个连接池，业务方要根据自己的需求配置连接池参数，使用成本较高；
   > 2. 异步场景下，并发很高时很容易出现超时不准的情况，线上出现过200ms超时设置实际超时返回时已经10s，感兴趣的朋友可以了解下TAsyncClientManager实现；
   > 3. 服务端性能较差，同机器环境下1K数据压测是8W QPS，NettyIO可以稳定到12W QPS；
   > 4. 编解码和传输模块紧耦合，不便于扩展；
   
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Netty是目前对NIO封装最成熟易用的网络通信框架，它的地位众所周知的，优势是：在主流NIO框架中性能最优、API使用简单、ChannelHandler可灵活扩展、社区活跃Bug会及时修复等。既然Netty这么优秀，我们也将ThriftIO切换到Netty进行通信，不负众望在压测中服务端QPS提升了50%。

### 2.4.4 监控跟踪
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado是分布式通信框架，并不包括性能监控系统，但更细粒度服务性能数据必须来自于框架的数据采集。通常使用监控系统的API上报数据的做法是，在起始和结束的位置埋点，由监控服务来计算耗时做数据统计，但我们平时在运维时经常遇到的一个问题就是，“为什么我的请求调用端比服务端耗时高出那么多”、“为什么框架上报耗时与我在接口实现中的耗时差异很大”。其实一个请求从发起到收到返回，中间经历了好几个阶段的耗时：客户端编解码序列化耗时 + 网络耗时 + 服务端编解码序列化耗时 + 业务逻辑耗时；还有调用端和服务端的gc卡顿，尤其发生fgc时是对请求耗时影响很大； 短连接的话还有取连接耗时；如果框架嵌入了其他功能还有额外的耗时，比如鉴权耗时。
   
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;所以通常的埋点都比较笼统或部分的采集到了前面所提阶段的耗时，那么对于这种耗时差异问题我们怎么排查，其实50%以上的问题都是gc导致的，看下问题时间段机器的gc监控就行，还有一部分是网络抖动，一般也是有监控的，但剩下的呢，怎么查，对于偶发性问题都是过去时了，也不能用BTrace或Greys这类神器，而且这些工具提供的耗时粒度很细，我可能只想知道编解码序列化是不是带来较多耗时损耗。其实解决这类过去时问题很简单，就是精细化分阶段埋点和链路跟踪。
   
- **分阶段数据埋点**

   详细的数据埋点，会对核心逻辑植入太多代码，这也是为什么一开始不提供的原因，无法避免就尽量优雅实现。Dorado的埋点阶段信息、埋点逻辑和阶段耗时计算都在TraceTimeline这个类中，TraceTimeline是TraceParam的属性字段，TraceParam作为RpcInvocation的附属参数贯穿调用端或服务端整个链路。
   
   如下是调用端同步请求时的埋点位置以及耗时阶段：另外，框架默认是基本埋点，分阶段埋点是关闭的，如果你有需求可以通过设置*timelineTrace*来启用。
   
   ![Timeline](https://github.com/Meituan-Dianping/octo-rpc/blob/master/dorado/dorado-doc/img/Timeline.svg)
   
   Dorado开源版本提供的监控模块是对[Cat](https://github.com/dianping/cat)的集成，你需要先部署Cat服务才能使用，如果你有自己的性能监控服务，也可以扩展Trace模块（只需要实现InvokeTrace接口）使用自己服务的API做数据上报，InvokeTrace提供了TraceParam参数，开启分阶段埋点你就可以获取框架的耗时统计了。
   
   当然监控模块不是必须的，如果你没有性能监控服务，埋点的逻辑都不会执行。

- **链路跟踪**

   若一个请求调用端耗时高，想对应查看该请求服务端的耗时请求，就需要将链路跟踪，实现链路跟踪就要依托于框架的参数透传，前面提到的TraceParam里有traceId属性，该信息会从调用端透传到服务端，由此就可以将一个请求的调用端和服务端关联起来。但是需要链路跟踪服务，Dorado开源版本暂未提供扩展支持，敬请期待。如果你有自己的链路跟踪服务，同样实现InvokeTrace接口就可以对接到你的服务中。Dorado框架可以同时支持多种上报服务，只要你将这些模块都包含进来，配置好InvokeTrace的SPI，就可以同时使用。
   
[监控跟踪说明](https://github.com/Meituan-Dianping/octo-rpc/tree/master/dorado/dorado-doc/manual-user/Trace.md)

### 2.4.5 故障降级/摘除
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado目前是两种方式判断节点状态：

- **主动失败检查**

  具体场景是调用端发送请求失败或创建连接失败后，会将该服务节点权重临时降级为0，后台任务探测节点连接状态，直到异常节点可用后恢复权重。

- **外部OCTO-Scanner检查**

  若你部署了OCTO-NS相关服务，服务端无论是使用OCTO-NS还是Zookeeper进行注册，都会接收到来自OCTO-Scanner的心跳检测，若发现节点不可用则会变更注册节点的状态，各个调用端都会收到变更从而摘除该节点；Scanner继续向不可用节点发心跳，若检测OK则恢复状态，各个调用端则会将该节点重新加会到服务列表。
  
### 2.4.6 单端口多服务
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;通常RPC服务都是一个端口一个服务，而实际应用中，业务方可能要提供大量的接口服务，每个服务都配置一个端口，每个端口都会初始化一套IO线程资源（后续会考虑IO线程共用），除了会创建大量线程外还会增加内存占用，尤其是Netty使用堆外内存，堆外内存问题不是通过dump就能快速排查出来的。线上就曾遇到过业务启用了34个端口导致堆外内存泄漏问题，虽然最后发现是Netty4.1.26的bug（https://github.com/netty/netty/pull/8160，4.1.29已修复），但为了避免类似问题我们更推荐大家在有多个接口时使用单端口多服务。

[单端口多服务说明](https://github.com/Meituan-Dianping/octo-rpc/tree/master/dorado/dorado-doc/manual-user/Port2MultiService.md)

### 2.4.7 服务启动预热
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Java编译器特性、服务缓存以及一些初始化操作，使服务一般在刚启动时响应较慢。所以服务端启动时如果不想在注册完成后，立即被调用端按照配置权重打入流量，则可以通过设置预热时间让流量慢慢进来，从而减少因服务节点启动带来的耗时长引发失败率可能变高的问题。
   
[服务启动预热说明](https://github.com/Meituan-Dianping/octo-rpc/tree/master/dorado/dorado-doc/manual-user/WeightWarmUp.md)

### 2.4.8 SPI扩展点
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;与业界大多Java服务的扩展机制是一样的，Dorado使用JDK内置的服务提供发现机制。但Dorado在获取扩展点实现上进行了一些改造，比如可以按照名字或者角色（调用端或服务端）获取扩展点实现。所有的SPI接口通过@SPI注解来识别，使用者实现SPI接口并按照Java SPI规范配置即可使用。
   
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado目前提供了以下的SPI扩展点（对于标注不支持多个实现的扩展点要注意，也就是使用中只能选择一种实现）：
   
  | SPI接口 | 说明 | 默认实现 |
  | --- | --- | --- |
  | com.meituan.dorado.registry.RegistryFactory | 注册中心 | MnsRegistryFactory(集成开源组件MNS)、ZookeeperRegistryFactory、MockRegistryFactory(用于无注册中心服务时测试) |
  | com.meituan.dorado.cluster.Cluster | 请求容错策略 | FailoverCluster(失败直接返回)、FailbackCluster(失败后重试其他节点)、FailOverCluster(失败重发) |
  | com.meituan.dorado.cluster.LoadBalance | 负载均衡策略 | RandomLoadBalance(随机权重)、RoundRobinLoadBalance(加权轮询) |
  | com.meituan.dorado.cluster.Router | 路由策略 | NoneRouter ；路由一般跟节点部署强相关，Dorado暂未提供默认实现，若使用OCTO-NS获取则是经过路由的服务列表|
  | com.meituan.dorado.rpc.handler.InvokeHandler | 服务请求处理类 | DoradoInvokerInvokeHandler(调用端请求处理)、DoradoProviderInvokeHandler(服务端请求处理) |
  | com.meituan.dorado.rpc.handler.HeartBeatInvokeHandler | 心跳请求处理 | ScannerHeartBeatInvokeHandler (OCTO-Scanner心跳处理)|
  | com.meituan.dorado.rpc.handler.HandlerFactory | 根据消息类型获取InvokeHandler和.HeartBeatInvokeHandler | DoradoHandlerFactory |
  | com.meituan.dorado.rpc.handler.filter.Filter | 过滤器实现 | DoradoInvokerTraceFilter(用于调用端埋点)、DoradoProviderTraceFilter(用于服务端埋点) |
  | com.meituan.dorado.rpc.handler.http.HttpInvokeHandler | Http接口测试 | DoradoHttpInvokeHandler |
  | com.meituan.dorado.check.http.HttpCheckHandler | 服务自检 | DoradoHttpCheckHandler |
  | com.meituan.dorado.codec.Codec | 编解码 | OctoCodec |
  | com.meituan.dorado.transport.LengthDecoder | 协议长度解码 | DoradoLengthDecoder |
  | com.meituan.dorado.transport.ClientFactory | 调用端传输模块 | NettyClientFactory |
  | com.meituan.dorado.transport.ServerFactory | 服务端传输模块 | NettyServerFactory |
  | com.meituan.dorado.transport.http.HttpServerFactory | Http服务 | NettyHttpServerFactory |
  | com.meituan.dorado.trace.InvokeTrace | 数据埋点 | CatInvokeTrace(集成开源组件Cat) |
  | com.meituan.dorado.rpc.proxy.ProxyFactory | 代理类 | JdkProxyFactory |

[扩展点说明](https://github.com/Meituan-Dianping/octo-rpc/tree/master/dorado/dorado-doc/manual-developer/Extension.md)

# 3. 性能指标
  |  |  |
  | --- | --- |
  | **压测环境** | 服务端：4C8G 虚拟机，双万兆网卡 <br> jdk1.7.0_76  CPU型号：E5-2650v2@2.60GHz <br> 并发：8（八个调用端直连）|
  | **协议和序列化** | OCTOProtocol + thrift |
  | **传输层** | Netty |
  | **测试方式** | 1K String 直接返回|
  | **性能** | 服务端QPS稳定在12W+|
  
# 4. 未来规划
- **使用方式多样化**

  后续我们将提供Thrift注解支持、集成Spring-Boot，为研发同学提供多种选择，提高研发效率。

- **多协议支持**

  暂时只支持OCTO协议或纯Thrift协议，后续我们将考虑提供其他协议并结合一些高效序列化框架如Protobuff、Kryo为大家提供更多高效的选择方案。

- **可靠性保障**

  在目前基本的故障降级基础上，增加策略性的熔断降级、限流功能，用户可以根据服务特性配置相关参数更好的保障服务的可用性。

- **路由策略**

  逐步提供美团内部的同城、同中心、同机房、灰度链路等多种灵活的路由策略。

- **性能探索**

  引入协程、支持服务端异步、连接池等，在性能上持续探索。

- **开源建设**

  随着内部版本的演进，我们会持续维护更新开源仓库代码和文档，逐步将美团服务治理的实践开放出去，更欢迎大家共建。

目前还有部分功能未完全开放出来，后续我们将提供更多丰富的功能和选择策略，在高性能、高可用上我们也会继续探索，追求极致不断迭代Dorado，为社区贡献一份力量。


# 5. 联系我们
- Email: octo@meituan.com
- Issues: [Issues](https://github.com/Meituan-Dianping/octo-rpc/issues)


## Dorado 埋点、调用追踪

### 1. 功能说明

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado是分布式通信框架，并不包括性能监控系统，但更细粒度服务性能数据必须来自于框架的数据采集。通常使用监控系统的API上报数据的做法是，在起始和结束的位置埋点，由监控服务来计算耗时做数据统计，但我们平时在运维时经常遇到的一个问题就是，“为什么我的请求调用端比服务端耗时高出那么多”、“为什么框架上报耗时与我在接口实现中的耗时差异很大”。其实一个请求从发起到收到返回，中间经历了好几个阶段的耗时：客户端编解码序列化耗时 + 网络耗时 + 服务端编解码序列化耗时 + 业务逻辑耗时；还有调用端和服务端的gc卡顿，尤其发生fgc时是对请求耗时影响很大； 短连接的话还有取连接耗时；如果框架嵌入了其他功能还有额外的耗时，比如鉴权耗时。

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;所以通常的埋点都比较笼统或部分的采集到了前面所提阶段的耗时，那么对于这种耗时差异问题我们怎么排查，其实50%以上的问题都是gc导致的，看下问题时间段机器的gc监控就行，还有一部分是网络抖动，一般也是有监控的，但剩下的呢，怎么查，对于偶发性问题都是过去时了，也不能用BTrace或Greys这类神器，而且这些工具提供的耗时粒度很细，我可能只想知道编解码序列化是不是带来较多耗时损耗。其实解决这类过去时问题很简单，就是精细化分阶段埋点和链路跟踪。

* 分阶段数据埋点

   详细的数据埋点，会对核心逻辑植入太多代码，这也是为什么一开始不提供的原因，无法避免就尽量优雅实现。Dorado的埋点阶段信息、埋点逻辑和阶段耗时计算都在TraceTimeline这个类中，TraceTimeline是TraceParam的属性字段，TraceParam作为RpcInvocation的附属参数贯穿调用端或服务端整个链路。
如下是调用端同步请求时的埋点位置以及耗时阶段：另外，框架默认是基本埋点，分阶段埋点是关闭的，如果你有需求可以通过设置timelineTrace来启用。

![框架分阶段埋点](../img/Timeline.svg)

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Dorado开源版本提供的监控模块是对[Cat](https://github.com/dianping/cat)的集成，你需要先部署Cat服务才能使用，如果你有自己的性能监控服务，也可以扩展Trace模块（只需要实现InvokeTrace接口）使用自己服务的API做数据上报，InvokeTrace提供了TraceParam参数，开启分阶段埋点你就可以获取框架的耗时统计了。
当然监控模块不是必须的，如果你没有性能监控服务，埋点的逻辑都不会执行。

* 链路跟踪

   若一个请求调用端耗时高，想对应查看该请求服务端的耗时请求，就需要将链路跟踪，实现链路跟踪就要依托于框架的参数透传，前面提到的TraceParam里有traceId属性，该信息会从调用端透传到服务端，由此就可以将一个请求的调用端和服务端关联起来。但是需要链路跟踪服务，Dorado开源版本暂未提供扩展支持，敬请期待。如果你有自己的链路跟踪服务，同样实现InvokeTrace接口就可以对接到你的服务中。Dorado框架可以同时支持多种上报服务，只要你将这些模块都包含进来，配置好InvokeTrace的SPI，就可以同时使用。


### 2.使用说明
>若使用Cat作数据监控，请先部署[Cat服务](https://github.com/dianping/cat)

#### 2.1 基本的埋点上报
使用了dorado-trace-cat模块，默认会在过滤器中上报数据，无需额外配置

#### 2.2 分阶段埋点上报
若启用分阶段埋点，需要配置***timelineTrace***为true
如下以XML配置为例

- 服务端
```xml
<bean id="serverPublisher" class="com.meituan.dorado.config.service.spring.ServiceBean">
    <!-- ...省略其他配置... -->
    <property name="timelineTrace" value="true"/>
</bean>
```
- 调用端
```xml
<bean id="clientProxy" class="com.meituan.dorado.config.service.spring.ReferenceBean">
    <!-- ...省略其他配置... -->
    <property name="timelineTrace" value="true"/>
</bean>
```

#### 2.3 不上报数据
Dorado目前只与Cat集成，若不想做数据埋点，剔除dorado-trace-cat模块即可。

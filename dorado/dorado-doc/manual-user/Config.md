
## Dorado 参数配置

Dorado提供下列的参数配置

### 1.调用端配置

| | 配置参数 | 含义 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| 必配配置 | appkey | 调用端appkey | | |                
|         | remoteAppkey | 服务端appkey | | |
|         | serviceInterface | 服务接口 | | 接口的全限定名，不包括iface | 
|         | registry | 服务发现配置 |  | [服务注册说明](Registry.md) |             
| 可选配置 | protocol | 协议类型 | thrift | 默认OCTO协议 + Thrift协议 |   
|         | serialize | 序列化方式 | thrift | |                
|         | timeout | 调用超时 | 1000 |  |                
|         | methodTimeout | 方法级别超时 | | [方法超时配置说明](MethodTimeout.md) |                
|         | connTimeout | 连接超时设置 | 1000 | |                
|         | directConnAddress | 直连配置 | | ip:port形式，可配置多个，逗号分隔 |
|         | remoteOctoProtocol| 直连使用统一协议 | false | 只在直连时生效 |               
|         | clusterPolicy | 集群容错策略 | failfast | 可以配置failover、failback | 
|         | failoverRetryTimes | failover策略重试次数 | 3 | |              
|         | loadBalancePolicy | 负载均衡策略 | random | 可以配置roundRobin |                
|         | routerPolicy | 路由策略 | none | | 
|         | filters | 过滤器配置 | | [过滤器链配置说明](Filter.md) |    
|         | timelineTrace | 开启分阶段耗时统计 | false | |
|         | env | 服务环境 | test | 服务当前所处的环境 |        
               

### 2.服务端配置

| | 配置参数 | 含义 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| 必配配置 | appkey | 服务端appkey | | |                
|         | port | 服务端口号 | | |
|         | serviceInterface | 服务接口 | | 接口的全限定名，包含类路径 | 
|         | serviceImpl | 接口实现类 | | 类的全限定名 |
|         | registry | 服务注册配置 | mock | [服务注册说明](Registry.md) |                      
| 可选配置 | protocol | 协议类型 | thrift | 默认OCTO协议 + Thrift协议 |                
|         | serialize | 序列化方式 | thrift | |                
|         | weight | 服务节点权重 | 10 |  |   
|         | warmup | 预热时间 | 0 | 单位为s ，[服务节点预热](WeightWarmUp.md) |                
|         | ioWorkerThreadCount | IO读写线程数 | cpucore * 2 |  |                
|         | bizCoreWorkerThreadCount | 业务线程的核心线程数 | 10 |  |
|         | bizMaxWorkerThreadCount |  业务线程的最大线程数 | 256 | |              
|         | bizWorkerQueueSize | 线程池队列大小 | 0 |  |
|         | bizWorkerExecutor | 线程池配置 | | 自行配置线程池实现业务线程隔离，若未配置，所有服务共用一个线程池 |
|         | methodWorkerExecutors | 方法级别线程池 | | 方法级别的业务线程隔离 |                              
|         | filters | 过滤器配置 | | [过滤器链配置说明](Filter.md) |                
|         | timelineTrace | 开启分阶段耗时统计 | false | |
|         | env | 服务环境 | test | 服务当前所处的环境 |
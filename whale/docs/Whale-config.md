# 配置文件

Whale 指定配置文件为当前目录下 conf.json 文件

# 配置格式
 
    {   
      "ns": {
               "origin": "10.24.41.248:2188",
               "ismns": 0,
               "env":"test"
             },
      "client.Appkey": "com.sankuai.inf.newct.client",
      "server.Version": "3.0.0",
      "server.ListenPort": 16888,
      "server.Appkey": "com.sankuai.inf.newct",
      "server.register": 1,
      "server.WorkThreadNum": 4,
      "server.MaxConnNum": 10000,
      "server.ServerTimeOut": 100,
      "server.ConnGCTime": 10,
      "server.ConnThreadNum": 4
    }

# 配置详解
| 配置项 | 子项| 含义 | 是否允许为空 | 备注 |
| ------ | ------ | ------ |------ | ------ |
| ns | | 服务注册发现 | 服务端不进行注册时可以为空或客户端使用直连方式可以为空，其他不可为空 |
|  | ismns | 注册中心模式 | 不可为空 |两种，0为simple模式zk注册中心，1位mns注册方式
|  | origin | 注册中心来源 | 不可为空 | 根据ismns的模式决定含义，当是zk注册中心时，内容是zk集群地址，当时mns注册中心时，为octo配置文件路径，根据文件来决定注册|
|  | env | 环境 | 不可为空 | 根据ismns的模式决定含义，当是zk注册中心时，内容业务自己定义，当时mns注册中心时，具体内容根据文件来决定环境具体值（该值被忽略）|
| client.Appkey | | 客户端app名字 | 可以为空 | 有内置默认appkey，只做测试时用，具体需要用户确定 |
| server.Version | | 服务端Version | 可以为空 | 有内置默认版本，只做测试时用，具体需要用户确定 |
| server.Appkey | | 服务端app名字 | 可以为空 | 有内置默认版本，只做测试时用，具体需要用户确定 |
| server.register | | 服务端是否需要注册 | 可以为空 | 默认不注册，使用直连方式比较方便 |
| server.WorkThreadNum | | 服务端工作线程数 | 可以为空 | 默认4，只做测试时用，具体需要用户确定 |
| server.MaxConnNum | | 服务端最大连接数 | 可以为空 | 默认10000，达到链接拒绝服务（过载保护），只做测试时用，具体需要用户确定 |
| server.ServerTimeOut | | 服务端超时 | 可以为空 | 默认100ms，只做测试时用，具体需要用户确定 |
| server.ConnGCTime | | 服务端清理空闲连接数时长 | 可以为空 | 默认100s，只做测试时用，具体需要用户确定 |
| server.ConnThreadNum | | 服务端IO线程数 | 可以为空 | 默认4，只做测试时用，具体需要用户确定 |

> 注意  客户端一般会有多个下游，所以客户端采用的是api的方式，而不是配置的方式决定参数    

客户端服务注册发现设置
  
```
／*
* 注册中心模式客户端初始化
* param   str_svr_appkey svr端appkey
* param   i32_timeout    请求超时时间 
* ret     返回值 成功返回0 不成功返回-1 
*／ 
int CthriftClient(const std::string &str_svr_appkey, const int32_t &i32_timeout);

／*
* 直连模式客户端初始化
* param   str_ip         svr端ip
* param   i16_port       svr端port 
* param   i32_timeout    请求超时时间 
* ret     返回值 成功返回0 不成功返回-1 
*／ 
int CthriftClient(const std::string &str_ip, const int16_t &i16_port, const int32_t &i32_timeout_ms);
```

### 客户端服务链接参数设置 ###
```
／*
* 设置客户端app名字，做追踪日志使用
* param   str_appkey     客户端app name
* ret     返回值 成功返回0 不成功返回-1 
*／ 
 int SetClientAppkey(const std::string &str_appkey);

／*
* 对服务列表进行端口过滤
* param   i32_port       要过滤的端口
* ret     返回值 成功返回0 不成功返回-1 
*／ 
  int SetFilterPort(const unsigned int &i32_port);

／*
* 对服务列表进行服务名过滤
* param   str_serviceName  要过滤的服务名
* ret     返回值 成功返回0   不成功返回-1 
*／ 
  int SetFilterService(const std::string &str_serviceName);

／*
* 设置客户端IO线程并行化  在并发的模式下使用，多个IO线程进行并行请求
* param   b_par  是否并行处理
* ret     返回值 成功返回0   不成功返回-1 
*／ 
  inline void SetParallel(const bool &b_par)；
  
／*
* 设置客户端IO线程上线  在客户端使用SetParallel的时候需要设置
* param   num        线程数上限
*／ 
  inline void SetWorkerNumber(const unsigned int &num);
  
／*
* 设置客户端异步化设置  在客户端使用异步请求的时候需要设置
* param   b_async    是否使用异步
*／ 
  inline void SetAsync(const bool &b_async);
  
／*
* 设置客户端异步请求时最大的消息积压个数，在SetAsync设置异步的时候使用
* param   b_par  是否并行处理
* ret     返回值 成功返回0   不成功返回-1 
*／ 
  inline void SetThreshold(const int &threshold);  
  
```

### 总初始化 ###
```
／*
* 初始化  设置完参数后 进行最后的初始化
* ret     返回值 成功返回0   不成功返回-1 
*／ 
  int Init(void);
```

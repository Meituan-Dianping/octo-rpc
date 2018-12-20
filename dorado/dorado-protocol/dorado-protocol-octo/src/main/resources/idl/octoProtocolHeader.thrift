namespace java com.meituan.dorado.codec.octo.meta

enum MessageType { // 消息类型
    Normal = 0,             // 正常消息
    NormalHeartbeat = 1,    // 端对端心跳消息
    ScannerHeartbeat = 2    //scanner心跳消息
}

enum CompressType { // 压缩类型
    None = 0,        // 不压缩
    Snappy = 1,      // Snappy
    Gzip = 2         // Gzip
}

struct RequestInfo {                // 请求信息
    1: required string serviceName; // 服务名
    2: required i64 sequenceId;     // 消息序列号
    3: required byte callType;      // 调用类型
    4: required i32 timeout;        // 请求超时时间
}

enum CallType {   // 调用类型
    Reply = 0,     // 需要响应
    NoReply = 1,   // 不需要响应
}

struct ResponseInfo {           // 响应信息
    1: required i64 sequenceId; // 消息序列号
    2: required byte status;    // 消息返回状态
    3: optional string message; //异常消息
}

enum StatusCode{
    Success = 0,              // 成功
    ApplicationException = 1, // 业务异常，业务接口方法定义抛出的异常
    RuntimeException = 2,     // 运行时异常，一般由业务抛出
    RpcException = 3,         // 框架异常，包含没有被下列异常覆盖到的框架异常
    TransportException = 4,   // 传输异常
    ProtocolException = 5,    // 协议异常
    DegradeException = 6,     // 降级异常
    SecurityException = 7,    // 安全异常
    ServiceException = 8,     // 服务异常，如服务端找不到对应的服务或方法
    RemoteException = 9,      // 远程异常
}



struct TraceInfo {                        // Mtrace 跟踪信息，原 MTthrift 中的 RequestHeader
    1: required string clientAppkey;      // 客户端应用名
    2: optional string traceId;           // Mtrace 的 traceId
    3: optional string spanId;            // Mtrace 的 spanId
    4: optional string rootMessageId;     // Cat 的 rootMessageId
    5: optional string currentMessageId;  // Cat 的 currentMessageId
    6: optional string serverMessageId;   // Cat 的 serverMessageId
    7: optional bool debug;               // 是否强制采样
    8: optional bool sample;              // 是否采样
    9: optional string clientIp;              // 客户端IP
}


struct LoadInfo{
     1: optional double averageLoad;
     2: optional i32 oldGC;
     3: optional i32 threadNum;                      //默认线程池
     4: optional i32 queueSize;                      //主IO线程队列长度
     5: optional map<string, double> methodQpsMap;   //key为ServiceName.methodName，value为1分钟内的对应的qps值（key为all，value则为所有方法的qps）
}

struct HeartbeatInfo {
    1:  optional string appkey;      // 解决重复注册，修改错误appkey状态的问题
    2:  optional i64 sendTime;       // 发送心跳时间，微秒，方便业务剔除历史心跳
    3:  optional LoadInfo loadInfo;  // 负载信息
    4:  required i32 status;         // 0：DEAD（未启动）， 2：ALIVE（正常），4：STOPPED（禁用）
}

typedef map<string, string> Context // 消息上下文，用于传递自定义数据

struct Header {                                                // 消息头
    1: optional byte messageType = MessageType.Normal;         // 消息类型
    2: optional RequestInfo requestInfo;                       // 请求信息
    3: optional ResponseInfo responseInfo;                     // 响应信息
    4: optional TraceInfo traceInfo;                           // 跟踪信息
    5: optional Context globalContext;                         // 全链路消息上下文，总大小不超过 512 Bytes
    6: optional Context localContext;                          // 单次消息上下文，总大小不超过 2K Bytes
    7: optional HeartbeatInfo heartbeatInfo;                   // 心跳信息
}
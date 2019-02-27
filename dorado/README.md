# OCTO-Dorado

## 简要介绍
- Dorado是OCTO生态中的一员，为Java服务提供具备治理功能的RPC通信框架。美团内部服务之间使用OCTO协议进行通信，默认支持Thrift，便于不同语言服务之间互通。

- Dorado提供了丰富的服务注册/发现、路由、负载均衡、容错等功能来满足服务治理需要。

- Dorado易扩展和简洁的设计可以让使用者和开发者更容易且灵活地对Dorado进行功能扩展和改造。

- Dorado的目标是构建一套更易用、更高效、更可靠，具有良好扩展性的分布式通信框架。


## 框架特点
- **模块化，易扩展**

   各个模块拆分实现，提供很多扩展点，可以根据需要扩展自己的实现模块，打造出适合自己服务的框架；

- **微内核，可插拔**

   核心模块不会依赖于任何具体扩展，每个实现模块都可以自由组合，按需引入；

- **实现简洁，链路清晰**

   框架设计简洁，主干调用链路清晰；

- **高性能，高可用**

   默认提供Netty作为网络传输框架和Thrift协议，在目前的Java框架中表现较优，服务端1K数据压测QPS稳定在**12W+**；
   服务端节点异常自动降级探测，提升调用端服务的可用性。

## 详细介绍
更多关于框架的介绍见：[OCTO-Dorado通信框架介绍](https://github.com/Meituan-Dianping/octo-rpc/wiki/OCTO-Dorado%E9%80%9A%E4%BF%A1%E6%A1%86%E6%9E%B6%E4%BB%8B%E7%BB%8D)

## 使用文档
- [快速开始](dorado-doc/QuickStart.md)

- [使用说明手册](dorado-doc/Manual.md)

- [构建Jar](dorado-doc/manual-developer/Compile.md)

## 开源协议
Dorado基于[Apache License 2.0](LICENSE)协议。

## 联系我们
- Email: inf.octo.os@meituan.com
- Issues: [Issues](https://github.com/Meituan-Dianping/octo-rpc/issues)

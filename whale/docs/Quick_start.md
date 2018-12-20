# 构建

Whale 以静态链接方式编译就行

Whale 依赖下列组件:

* [muduo](https://github.com/chenshuo/muduo): 网络工具库
* [boost](https://github.com/boostorg/boost): 基础的C++工具库
* [zlib](https://github.com/madler/zlib): 压缩工具库
* [thrift](https://github.com/apache/thrift): rpc通讯框架
* [log4cplus](https://github.com/log4cplus/log4cplus): 日志库
* [zookeeper](https://github.com/apache/zookeeper): zookeeper c api
* [rapidjson](https://github.com/Tencent/rapidjson): json解析库
* [octoidl](https://github.com/Meituan-Dianping/octo-ns): octo-mns 通用公共依赖
* [mns-sdk](https://github.com/Meituan-Dianping/octo-ns): octo-mns 通用c++ 服务注册发现sdk


# 依赖支持

| 库依赖 | 版本 | 备注 |
| ------ | ------ | ------ |
| muduo | 1.1.0 | 有http服务补丁，见仓库目录 patch/0001-add-patch-for-http.patch |
| zookeeper | 3.4.6 |  |
| boost | 1.45 和 1.55 | 建议centos6 使用1.45,建议centos7 使用1.55  |
| zlib | 1.2.3 |  |
| thrift | 0.8.0 |  |
| log4cplus | 1.1.3 |  |
| rapidjson | 1.1.0 |  |
| octoidl | 0.1 |  |
| mns-sdk | 0.1 |  |


# 支持环境

* [CentOS6 和 CentOS7](https://www.centos.org/)

### 准备依赖


CentOS通常需要安装EPEL，否则许多软件包默认不可用：  
```
sudo yum install epel-release
```

安装通用依赖库：  
```
sudo yum install git gcc-c++ cmake
```

安装 [boost](https://github.com/boostorg/boost), [zlib](https://github.com/madler/zlib), [log4cplus](https://github.com/log4cplus/log4cplus),[rapidjson](https://github.com/Tencent/rapidjson):
```shell
sudo yum install boost-devel zlib-devel log4cplus-devel rapidjson-devel
```

安装 [zookeeper c api](https://github.com/apache/zookeeper), [thrift](https://github.com/apache/thrift):  
```
thrift 和 zookeeper 一般很少有yum源，请自行参照官方使用文档安装
```

本机搭建 zookeeper注册中心 [zookeeper](https://github.com/apache/zookeeper):  
```
参照zookeeper官方文档文档搭建集群(默认是本机配置，在示例里面用的是127.0.0.1，不是本机的话，需要配置zk注册中心地址)
```


### 使用 build.sh 编译使用 Whale 
**clone下载仓库** 
 
```bash   
> git clone https://github.com/Meituan-Dianping/octo-rpc.git      
```

**使用 build.sh 初始化**

```bash   
> cd octo-rpc/whale       
> build.sh init 
```
 

**使用 build.sh 编译 库文件**

```bash 
> cd octo-rpc/whale    
> build.sh only_lib 
```

**使用 build.sh 编译 example**   

```bash  
> cd octo-rpc/whale     
> build.sh with_example    
```

**使用 build.sh run example**   
*注意：如果zk注册中心搭建的是同机环境的话，不需要做修改，如果不是同机的话，需要修改 conf.json 里面的ns:origin 字段*

```bash 
> cd build/bin/  
> nohup ./cthrift_svr_example >output.txt 2>&1 &  
> ./cthrift_cli_example     
```
 
成功标志，客户端输出  

```bash 
> **********run rpc for echo 1000 times suceess*********
```


### Whale 和 OCTO-mns一起使用
> 见 [Whale 配置手册](Whale-config.md) 和 [OCTO-mns环境搭建](https://github.com/Meituan-Dianping/octo-ns)






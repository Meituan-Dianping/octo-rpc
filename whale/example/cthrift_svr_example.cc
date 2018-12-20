/*
 * Copyright (c) 2011-2018, Meituan Dianping. All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#include <muduo/base/TimeZone.h>
#include <muduo/net/EventLoop.h>

#include <cthrift/cthrift_svr.h>
#include "Echo.h"

using namespace std;

using namespace echo;
using namespace muduo;
using namespace muduo::net;
using namespace meituan_cthrift;

//重载处理handler类
class EchoHandler : virtual public EchoIf {
 public:
  EchoHandler() {
  }

  //业务真正的处理业务逻辑
  void echo(std::string &str_ret, const std::string &str_req) {
    str_ret.assign(str_req);
  }
};

int
main(int argc, char **argv) {

  //初始化日志
  log4cplus::PropertyConfigurator::doConfigure(LOG4CPLUS_TEXT("log4cplus.conf"));

  muduo::string name("EchoServer");

  //初试化处理handler
  boost::shared_ptr<EchoHandler> handler(new EchoHandler());
  boost::shared_ptr<TProcessor> processor(new EchoProcessor(handler));

  try {

    //声明定义server类对象
    CthriftSvr server(processor);

    //server初始化
    if (server.Init() != 0) {
      cerr << "server init error" << endl;
      return -1;
    }

    //server对外服务，服务会陷入该函数，直到服务退出
    server.serve();

    //服务退出之后执行清理工作
    server.stop();

  } catch (TException &tx) {
    cerr << tx.what() << endl;

    return -1;
  }

  return 0;
}

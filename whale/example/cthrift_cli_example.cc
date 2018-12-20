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

#include <stdio.h>

#include <muduo/base/TimeZone.h>
#include <muduo/net/EventLoop.h>
#include <muduo/net/EventLoopThread.h>

#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>
#include <sstream>
#include <cthrift/cthrift_svr.h>
#include <cthrift/cthrift_client.h>
#include "Echo.h"

using namespace std;

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

using namespace muduo;
using namespace muduo::net;

using namespace boost;
using namespace echo;

using namespace meituan_cthrift;

//1K 数据echo测试
void Work(const string &str_svr_appkey,
          const string &str_cli_appkey,
          const int32_t &i32_timeout_ms,
          muduo::CountDownLatch *p_countdown) {

  //建议CthriftClient生命期也和线程保持一致，不要一次请求创建销毁一次
  CthriftClient cthrift_client(str_svr_appkey, i32_timeout_ms);

  //cthrift_client.SetFilterService("echo.Echo");

  //设置client appkey，方便服务治理识别来源
  if (SUCCESS != cthrift_client.SetClientAppkey(str_cli_appkey)) {
    cerr << "SetClientAppkey error" << endl;
    p_countdown->countDown();
    return;
  }


  //显示调用初始化，初始化内部流程
  if (SUCCESS != cthrift_client.Init()) {
    cerr << "Init error" << endl;
    p_countdown->countDown();
    return;
  }
  //确保业务EchoClient的生命期和线程生命期同，不要一次请求创建销毁一次EchoClient！！
  EchoClient
      client
      (cthrift_client.GetCthriftProtocol());
  string strRet;
  string str_tmp;
  size_t sz;

  char buf[1025];  //1K数据
  memset(buf, 1, sizeof(buf));
  int num = 1000;

  for (int i = muduo::CurrentThread::tid() * num;
       i < muduo::CurrentThread::tid() * num + num; i++) {

    try {
      str_tmp = boost::lexical_cast<std::string>(i);
    } catch (boost::bad_lexical_cast &e) {

      cerr << "boost::bad_lexical_cast :" << e.what()
           << "i : " << i;
      continue;
    }

    sz = str_tmp.size();
    str_tmp += string(buf, 0, sizeof(buf) - 1 - sz);

    try {
      //标准RPC调用
      client.echo(strRet, str_tmp);
    } catch (TException &tx) {
      cerr << "ERROR: " << tx.what() << endl;
      p_countdown->countDown();
      return;
    }

    if (str_tmp != strRet) {
      cerr << "tid: " << muduo::CurrentThread::tid() << "strRet " << strRet
           << " str_tmp " << "test" + str_tmp << endl;
      p_countdown->countDown();
      return;
    }
  }

  cout << "tid: " << muduo::CurrentThread::tid() << " END" << endl;
  cout << "**********run rpc for echo 1000 times suceess*********" << endl;

  p_countdown->countDown();
}

int main(int argc, char **argv) {

  log4cplus::PropertyConfigurator::doConfigure(LOG4CPLUS_TEXT("log4cplus.conf"));

  string str_svr_appkey("com.sankuai.inf.newct"); //服务端的appkey
  string str_cli_appkey("com.sankuai.inf.newct.client"); //客户端的appkey
  int32_t i32_timeout_ms = 20;

  switch (argc) {
    case 1:std::cout << "no input arg, use defalut" << std::endl;
      break;

    case 4:str_svr_appkey.assign(argv[1]);
      str_cli_appkey.assign(argv[2]);
      i32_timeout_ms = static_cast<int32_t>(atoi(argv[3]));
      break;
    default:
      cerr << "prog <svr appkey> <client appkey> <timeout ms> but argc " << argc
           << endl;
      exit(-1);
  }

  std::cout << "svr appkey " << str_svr_appkey << std::endl;
  std::cout << "client appkey " << str_cli_appkey << std::endl;
  std::cout << "timeout ms " << i32_timeout_ms << std::endl;

  //10个线程并发
  int32_t i32_thread_num = 2;  //线程数视任务占用CPU时间而定，建议不要超过2*CPU核数
  muduo::CountDownLatch countdown_thread_finish(i32_thread_num);
  for (int i = 0; i < i32_thread_num; i++) {
    muduo::net::EventLoopThread *pt = new muduo::net::EventLoopThread;
    pt->startLoop()->runInLoop(boost::bind(Work,
                                           str_svr_appkey, //服务端Appkey必须填写，不可为空，寻求服务
                                           str_cli_appkey, //客户端Appkey必须填写，不可为空，以便于问题追踪
                                           i32_timeout_ms,
                                           &countdown_thread_finish));
  }

  countdown_thread_finish.wait();

  std::cout << "exit" << std::endl;

  //need wait some time for resources clean.
  sleep(1);
  return 0;
}

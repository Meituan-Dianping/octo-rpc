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
#include <thrift/async/TAsyncChannel.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>
#include <sstream>
#include <cthrift/cthrift_svr.h>
#include <cthrift/cthrift_client.h>
#include <cthrift/cthrift_client_channel.h>
#include <cthrift/cthrift_async_callback.h>
#include "Echo.h"

using namespace std;

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::async;

using namespace muduo;
using namespace muduo::net;

using namespace boost;
using namespace echo;

using namespace meituan_cthrift;

//callback函数，业务代码实际逻辑。
void my_echo(EchoCobClient *client) {
  std::string pong = "";
  //这行必不可少，请业务参考idl产生的源代码，填写revc函数
  client->recv_echo(pong);
  //业务实际的处理逻辑
  std::cout << pong << std::endl;
}

//1K 数据echo测试
void Work(const string &str_svr_appkey,
          const string &str_cli_appkey,
          const int32_t &i32_timeout_ms,
          muduo::CountDownLatch *p_countdown) {

  //建议CthriftClient生命期也和线程保持一致，不要一次请求创建销毁一次
  boost::shared_ptr<CthriftClient> cthrift_client =
      boost::make_shared<CthriftClient>(str_svr_appkey, i32_timeout_ms);

  //开启异步化，并设置异步场景下队列阈值，任务超过阈值后会被丢弃，并且向该业务线程抛出异常
  cthrift_client->SetAsync(true);
  cthrift_client->SetThreshold(100);
  //设置client appkey，方便服务治理识别来源
  if (SUCCESS != cthrift_client->SetClientAppkey(str_cli_appkey)) {
    cerr << "SetClientAppkey error" << endl;
    p_countdown->countDown();
    return;
  }

  //显示调用初始化，初始化内部流程
  if (SUCCESS != cthrift_client->Init()) {
    cerr << "Init error" << endl;
    p_countdown->countDown();
    return;
  }

  /* 注意：72行～84行关键代码段的Cthrift Client的初始化流程，请保证线程安全问题；
  *   避免多线程同时初始化，同一个cthrift client场景
  */

  boost::shared_ptr<TAsyncChannel>
      channel = boost::make_shared<CthriftClientChannel>(cthrift_client);
  EchoCobClient client(channel, new CthriftTAsyncProtocolFactory());

  //业务借助AsyncCallback<class type>模版, 传入业务自己的回调处理callback: Success、Timeout ...
  function<void(EchoCobClient *client)> echo_cob = boost::bind(&my_echo, _1);
  AsyncCallback <EchoCobClient> *pecho = new AsyncCallback<EchoCobClient>();
  //传入成功处理callback
  pecho->Success(echo_cob);
  function<void(EchoCobClient *client)>
      cob = boost::bind(&AsyncCallback<EchoCobClient>::Callback, pecho, _1);

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

    retry:
    try {
      //thrift标准的异步化
      client.echo(cob, str_tmp);
    } catch (TException &tx) {
      cerr << "ERROR: " << tx.what() << endl;
      sleep(1);
      //抛出超出发送队列阈值异常，业务线程应该减速，并且重试本次数据发送操作
      goto retry;
    }

  }

  cout << "tid: " << muduo::CurrentThread::tid() << " END" << endl;

  //受到thrift本身的限制，异步化channel相关对象的生命周期需要和进程一致
  //等待异步回调线程排空数据
  sleep(10);
  p_countdown->countDown();
}

int main(int argc, char **argv) {
  log4cplus::PropertyConfigurator::doConfigure(LOG4CPLUS_TEXT("log4cplus.conf"));
  string str_svr_appkey("com.sankuai.inf.newct"); //服务端的appkey
  string str_cli_appkey("com.sankuai.inf.client"); //客户端的appkey
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
  int32_t i32_thread_num = 10;  //线程数视任务占用CPU时间而定，建议不要超过2*CPU核数
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

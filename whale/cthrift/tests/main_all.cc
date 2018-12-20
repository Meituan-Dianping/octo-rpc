//
// Created by Chao Shu on 16/4/5.
//

#include <gtest/gtest.h>

#include <boost/bind.hpp>
#include <boost/make_shared.hpp>

#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>

#include <muduo/base/AsyncLogging.h>
#include <muduo/base/Logging.h>
#include <muduo/base/TimeZone.h>
#include <muduo/net/EventLoopThread.h>
#include <muduo/net/InetAddress.h>
#include <thrift/async/TAsyncChannel.h>
#include <cthrift/cthrift_client_channel.h>
#include <cthrift/cthrift_async_callback.h>

#define private public
#define protected public
#include <cthrift/cthrift_svr.h>
#include <cthrift/cthrift_client.h>
#undef private
#undef protected

#include "Echo.h"

using namespace std;

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::async;


using namespace echo;
using namespace meituan_cthrift;

using namespace muduo;
using namespace muduo::net;
using namespace boost;

using testing::Types;

boost::shared_ptr<muduo::Thread> g_sp_thread_svr;
boost::shared_ptr<muduo::Thread> g_sp_thread_cli;
boost::shared_ptr<muduo::Thread> g_sp_thread_asyc_cli;

muduo::AsyncLogging *g_asyncLog = NULL;
muduo::net::EventLoop event_loop;

void asyncOutput(const char *msg, int len) {
  g_asyncLog->append(msg, len);
}

void Quit(void) {
  event_loop.quit();
  g_sp_thread_cli.reset();
  g_sp_thread_asyc_cli.reset();

  CTHRIFT_LOG_INFO("exit");
}

//callback函数，业务代码实际逻辑。
void my_echo(EchoCobClient *client) {
  std::string pong = "";
  //这行必不可少，请业务参考idl产生的源代码，填写revc函数
  client->recv_echo(pong);
  //业务实际的处理逻辑
  std::cout << pong << std::endl;
}

void TestGetSvrList(const string &str_svr_appkey,
                    const string &str_cli_appkey,
                    const int32_t &i32_loop_num) {
  //建议CthriftClient生命期也和线程保持一致，不要一次请求创建销毁一次
  CthriftClient cthrift_client(str_svr_appkey, 30);

  //设置client appkey，方便服务治理识别来源
  if (SUCCESS != cthrift_client.SetClientAppkey(str_cli_appkey)) {
    return;
  }
  if (SUCCESS != cthrift_client.Init()) {
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

  std::string arg = "arg";

  test arg_all;
  arg_all.__set_arg("arg1");
  arg_all.__set_arg2(10.0);

  std::vector<std::string> arg3;
  arg3.push_back(arg);
  arg3.push_back(arg);
  arg3.push_back(arg);

  arg_all.__set_arg3(arg3);

  std::map<std::string, std::string> arg4;
  arg4["1"] = arg;
  arg4["2"] = arg;
  arg4["3"] = arg;

  arg_all.__set_arg4(arg4);

  arg_all.__set_arg5(false);

  std::set<int64_t> arg6;
  arg6.insert(1);
  arg6.insert(2);
  arg6.insert(3);

  arg_all.__set_arg6(arg6);

  arg_all.__set_arg7(7);

  arg_all.__set_arg8(TweetType::REPLY);

  for (int i = 0; i < 100; i++) {

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
      client.echo(strRet, str_tmp, arg_all);
    } catch (TException &tx) {
      cerr << "ERROR: " << tx.what() << endl;
      return;
    }

    if (str_tmp != strRet) {
      cerr << "tid: " << muduo::CurrentThread::tid() << "strRet " << strRet
           << " str_tmp " << "test" + str_tmp << endl;
      return;
    }
  }

}

void TestAsyncSvrList(const string &str_svr_appkey,
                      const string &str_cli_appkey,
                      const int32_t &i32_loop_num) {
  //建议CthriftClient生命期也和线程保持一致，不要一次请求创建销毁一次
  boost::shared_ptr<CthriftClient>
      cthrift_client = boost::make_shared<CthriftClient>(str_svr_appkey, 30);


  //开启异步化，并设置异步场景下队列阈值，任务超过阈值后会被丢弃，并且向该业务线程抛出异常
  cthrift_client->SetAsync(true);
  cthrift_client->SetThreshold(100);
  //设置client appkey，方便服务治理识别来源
  if (SUCCESS != cthrift_client->SetClientAppkey(str_cli_appkey)) {
    return;
  }
  if (SUCCESS != cthrift_client->Init()) {
    return;
  }
  /* 注意：72行～84行关键代码段的Cthrift Client的初始化流程，请保证线程安全问题；
 *   避免多线程同时初始化，同一个cthrift client场景
 * */

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

  std::string arg = "arg";

  test arg_all;
  arg_all.__set_arg("arg1");
  arg_all.__set_arg2(10.0);

  std::vector<std::string> arg3;
  arg3.push_back(arg);
  arg3.push_back(arg);
  arg3.push_back(arg);

  arg_all.__set_arg3(arg3);

  std::map<std::string, std::string> arg4;
  arg4["1"] = arg;
  arg4["2"] = arg;
  arg4["3"] = arg;

  arg_all.__set_arg4(arg4);

  arg_all.__set_arg5(false);

  std::set<int64_t> arg6;
  arg6.insert(1);
  arg6.insert(2);
  arg6.insert(3);

  arg_all.__set_arg6(arg6);

  arg_all.__set_arg7(7);

  arg_all.__set_arg8(TweetType::REPLY);

  for (int i = muduo::CurrentThread::tid() * 1000;
       i < muduo::CurrentThread::tid() * 1000 + 100; i++) {

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
      client.echo(cob, str_tmp, arg_all);
    } catch (TException &tx) {
      cerr << "ERROR: " << tx.what() << endl;
      sleep(1);
      //抛出超出发送队列阈值异常，业务线程应该减速，并且重试本次数据发送操作
      goto retry;
    }

  }

  //等待异步回调线程排空数据
  sleep(20);
}

class EchoHandler : virtual public EchoIf {
 public:
  EchoHandler() {
  }

  void echo(std::string &_return, const std::string &arg, const test &arg2) {
    CTHRIFT_LOG_DEBUG("EchoHandler::echo:" << arg);
    _return = arg;
  }
};

void TestRegSvr(muduo::net::EventLoop *p_event_loop) {
  boost::shared_ptr<EchoHandler> handler(new EchoHandler());
  boost::shared_ptr<TProcessor> processor(new EchoProcessor(handler));

  try {
    CthriftSvr svr(processor);
    if (svr.Init() != 0) {
      CTHRIFT_LOG_ERROR("CthriftSvr init failed: ");
      return;
    }

    svr.serve();
  } catch (TException &tx) {
    EXPECT_EQ(tx.what(), "str_app_key empty, reason ");
    CTHRIFT_LOG_ERROR("CthriftSvr failed: " << tx.what());
    cerr << "CthriftSvr failed: " << tx.what() << endl;
    FAIL();

    p_event_loop->runInLoop(boost::bind(&Quit));
  }
}

int main(int argc, char **argv) {

  testing::InitGoogleTest(&argc, argv);

  log4cplus::PropertyConfigurator::doConfigure(LOG4CPLUS_TEXT("log4cplus.conf"));

  if (CTHRIFT_UNLIKELY(4 < argc)) {
    CTHRIFT_LOG_ERROR(
        "prog <appkey,default \"com.sankuai.inf.newct\"> <echo loop time, default 100> <port, defalut 6666>");
    cerr
        << "prog <appkey,default \"com.sankuai.inf.newct\"> <echo loop time, default 100> <port, defalut 6666>"
        << endl;
    exit(-1);
  }

  string str_svr_appkey("com.sankuai.inf.newct");
  string str_cli_appkey("com.sankuai.inf.newct.client");
  int32_t i32_echo_time = 100;

  if (2 <= argc) {
    str_svr_appkey.assign(argv[1]);
    if (3 <= argc) {
      i32_echo_time = atoi(argv[2]);
    }
  }

  CTHRIFT_LOG_DEBUG(
      "str_svr_appkey: " << str_svr_appkey << " i32_echo_time: " <<
                         i32_echo_time);

  g_sp_thread_svr =
      boost::make_shared<muduo::Thread>(boost::bind(&TestRegSvr, &event_loop));
  g_sp_thread_svr->start();

  muduo::CurrentThread::sleepUsec(3000 * 1000); //wait 3s for reg svr

  g_sp_thread_cli =
      boost::make_shared<muduo::Thread>(boost::bind(&TestGetSvrList,
                                                    str_svr_appkey,
                                                    str_cli_appkey,
                                                    i32_echo_time));
  g_sp_thread_cli->start();

  g_sp_thread_asyc_cli =
      boost::make_shared<muduo::Thread>(boost::bind(&TestAsyncSvrList,
                                                    str_svr_appkey,
                                                    str_cli_appkey,
                                                    i32_echo_time));
  g_sp_thread_asyc_cli->start();

  event_loop.runAfter(60.0, boost::bind(&Quit));

  event_loop.loop();

  CTHRIFT_LOG_INFO("EXIT loop");

  return RUN_ALL_TESTS();;
}


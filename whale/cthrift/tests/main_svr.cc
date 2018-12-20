//
// Created by Chao Shu on 16/4/5.
//

#include <gtest/gtest.h>

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

using namespace std;

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::async;

int main(int argc, char **argv) {

  testing::InitGoogleTest(&argc, argv);

  log4cplus::PropertyConfigurator::doConfigure(LOG4CPLUS_TEXT("log4cplus.conf"));

  return RUN_ALL_TESTS();;
}



//
// Created by huixiangbo on 2017/10/12.
//

#include <string>
#include <vector>
#include <gtest/gtest.h>
#define private public
#define protected public
#include <cthrift/cthrift_client_channel.h>
#include <thrift/async/TAsyncChannel.h>
#include <cthrift/cthrift_async_callback.h>
#undef private
#undef protected

using namespace std;
using namespace meituan_cthrift;
using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::async;

class CthriftChannelTest : public testing::Test {
 public:
  CthriftChannelTest() {
    boost::shared_ptr<CthriftClient>
        cthrift_client = boost::make_shared<CthriftClient>("test.appkey", 30);
    p_cthrift_channel_ = new CthriftClientChannel(cthrift_client);
  }
  ~CthriftChannelTest() {
    if (p_cthrift_channel_) {
      delete p_cthrift_channel_;
    }
  }
  virtual void SetUp() {
  }
  virtual void TearDown() {
  }
 public:
  CthriftClientChannel *p_cthrift_channel_;
};

TEST_F(CthriftChannelTest, Handle_Good) {
  EXPECT_TRUE(p_cthrift_channel_->good()
  );
}

TEST_F(CthriftChannelTest, Handle_Error) {
  EXPECT_TRUE(p_cthrift_channel_->error() != true);
}

TEST_F(CthriftChannelTest, Handle_TimeOut) {
  EXPECT_TRUE(p_cthrift_channel_->timedOut() != true);
}

TEST_F(CthriftChannelTest, Handle_sendMessage) {

  TAsyncChannel::VoidCallback cob;
  apache::thrift::transport::TMemoryBuffer *message = NULL;
  std::string error;

  try {
    p_cthrift_channel_->sendMessage(cob, message);
  } catch (TException &tx) {
    error = tx.what();
  }

  EXPECT_TRUE(error == "Unexpected call to TEvhttpClientChannel::sendMessage");

}

TEST_F(CthriftChannelTest, Handle_recvMessage) {

  TAsyncChannel::VoidCallback cob;
  apache::thrift::transport::TMemoryBuffer *message = NULL;
  std::string error;

  try {
    p_cthrift_channel_->recvMessage(cob, message);
  } catch (TException &tx) {
    error = tx.what();
  }

  EXPECT_TRUE(error == "Unexpected call to TEvhttpClientChannel::recvMessage");

}

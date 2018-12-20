
//
// Created by huixiangbo on 2017/10/12.
//

#include <string>
#include <gtest/gtest.h>
#define private public
#define protected public
#include <cthrift/cthrift_transport.h>
#include <cthrift/cthrift_client_worker.h>
#undef private
#undef protected

using namespace std;
using namespace meituan_cthrift;
using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::async;

class CthriftTransportTest : public testing::Test {
 public:
  CthriftTransportTest() {

    sp_cthrift_client_worker_ = boost::make_shared<CthriftClientWorker>(
        "test.server",
        "test.client",
        "test.name",
        16888,
        "",
        0);

    tmp_buf_ = boost::make_shared<CthriftTransport>("test.server",
                                                    30,
                                                    "test.client",
                                                    sp_cthrift_client_worker_);

    tmp_buf_->SetID2Transport("testid");
  }
  ~CthriftTransportTest() {
  }
  virtual void SetUp() {
  }
  virtual void TearDown() {
  }
 public:
  boost::shared_ptr <CthriftClientWorker> sp_cthrift_client_worker_;
  boost::shared_ptr <CthriftTransport> tmp_buf_;
};

TEST_F(CthriftTransportTest, Handle_Reset) {
  uint8_t *p_buf = 0;
  uint32_t u32_len = 0;

  tmp_buf_->ResetWriteBuf();
  tmp_buf_->sp_write_tmembuf_->getBuffer(&p_buf, &u32_len);

  EXPECT_TRUE(0 == u32_len);
}



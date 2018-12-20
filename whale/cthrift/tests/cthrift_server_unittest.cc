
//
// Created by huixiangbo on 2017/10/12.
//

#include <string>
#include <vector>
#include <gtest/gtest.h>
#define private public
#define protected public
#include <cthrift/cthrift_svr.h>
#undef private
#undef protected
#include "Echo.h"
using namespace std;
using namespace echo;
using namespace meituan_cthrift;

class EchoHandler : virtual public EchoIf {
 public:
  EchoHandler() {
  }

  //echo测试
  void echo(std::string& str_ret, const std::string& str_req, const echo::test& x) {
    str_ret.assign(str_req);
  }
};

class CthriftSvrTest : public testing::Test {
 public:
  CthriftSvrTest() {

    muduo::string name("EchoServer");

    boost::shared_ptr <EchoHandler> handler(new EchoHandler());
    boost::shared_ptr <TProcessor> processor(new EchoProcessor(handler));

    try {

      p_cthrift_svr_ = new CthriftSvr(processor);
      p_cthrift_svr_->Init();

    } catch (TException &tx) {
      CTHRIFT_LOG_ERROR(tx.what());
    }

  }
  ~CthriftSvrTest() {
    if (p_cthrift_svr_) {
      p_cthrift_svr_->stop();
      delete p_cthrift_svr_;
    }
  }
  virtual void SetUp() {
  }
  virtual void TearDown() {
  }
 public:
  CthriftSvr *p_cthrift_svr_;
};

TEST_F(CthriftSvrTest, Handle_name) {
  const std::string str_name = "com.sankuai.inf.newct_cthrift_svr";

  p_cthrift_svr_->StatMsgNumPerMin();
  p_cthrift_svr_->InitWorkerThreadPos();

  EXPECT_TRUE(p_cthrift_svr_->name() == str_name);
}

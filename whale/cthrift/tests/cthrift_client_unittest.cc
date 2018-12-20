
//
// Created by huixiangbo on 2017/10/12.
//

#include <string>
#include <vector>
#include <gtest/gtest.h>
#define private public
#define protected public
#include <cthrift/cthrift_client.h>
#undef private
#undef protected

using namespace std;
using namespace meituan_cthrift;

class CThriftClientTest : public testing::Test {
 public:
  CThriftClientTest() {
    std::string str_svr_appkey = "test_server_appkey";
    std::string str_cli_appkey = "test_client_appkey";
    int32_t i32_timeout = 20;
    p_cthrift_client_ = new CthriftClient(str_svr_appkey, i32_timeout);
  }
  ~CThriftClientTest() {
    if (p_cthrift_client_) {
      delete p_cthrift_client_;
    }
  }
  virtual void SetUp() {
  }
  virtual void TearDown() {
  }
 public:
  CthriftClient *p_cthrift_client_;
};

TEST_F(CThriftClientTest, Handle_SetClientAppkey) {
  const std::string str_appkey = "cthrit_test";
  EXPECT_TRUE(p_cthrift_client_->SetClientAppkey(str_appkey) == 0);
}

TEST_F(CThriftClientTest, Handle_SetFilterService) {
  const std::string str_servicename = "cthrit_name";
  EXPECT_TRUE(p_cthrift_client_->SetFilterService(str_servicename) == 0);
}

TEST_F(CThriftClientTest, Handle_SetFilterPort) {
  const int32_t i32_port = 8080;
  EXPECT_TRUE(p_cthrift_client_->SetFilterPort(i32_port) == 0);

}

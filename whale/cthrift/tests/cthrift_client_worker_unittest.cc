
//
// Created by huixiangbo on 2017/10/12.
//

#include <string>
#include <vector>
#include <gtest/gtest.h>
#define private public
#define protected public
#include <cthrift/cthrift_client_worker.h>
#undef private
#undef protected

using namespace std;
using namespace meituan_cthrift;

class CthriftClientWorkerTest : public testing::Test {
 public:
  CthriftClientWorkerTest() {
    const std::string str_svr_appkey = "test_appkey_server";
    const std::string str_cli_appkey = "test_appkey_client";
    const std::string str_serviceName_filter = "test_servicename";
    const int32_t i32_port_filter = 16888;
    p_cthrift_client_worker_ = new CthriftClientWorker(str_svr_appkey,
                                                       str_cli_appkey,
                                                       str_serviceName_filter,
                                                       i32_port_filter,
                                                       "",
                                                       0);
  }

  ~CthriftClientWorkerTest() {
    if (p_cthrift_client_worker_) {
      delete p_cthrift_client_worker_;
    }
  }
  virtual void SetUp() {
  }
  virtual void TearDown() {
  }
 public:
  CthriftClientWorker *p_cthrift_client_worker_;
};

TEST_F(CthriftClientWorkerTest, Handle_DelContextMapByID) {
  std::string str_id = "test_id";
  p_cthrift_client_worker_->
      DelContextMapByID(str_id);

  boost::unordered_map<std::string, SharedContSharedPtr>::iterator
      map_iter = p_cthrift_client_worker_->map_id_sharedcontextsp_.find(str_id);
  EXPECT_TRUE(
      map_iter == p_cthrift_client_worker_->map_id_sharedcontextsp_.end());
}

TEST_F(CthriftClientWorkerTest, Handle_FilterPort) {
  const std::string str_svr_appkey = "test.appkey";
  const std::string str_local_ip = "127.0.0.1";
  const uint16_t u16_port = 16888;
  meituan_mns::SGService sgservice;

  sgservice.port = u16_port;
  sgservice.ip == str_local_ip;

  EXPECT_TRUE(p_cthrift_client_worker_->FilterPort(sgservice) == false);
}

TEST_F(CthriftClientWorkerTest, Handle_FilterOther) {
  const std::string str_svr_appkey = "test.appkey";
  const std::string str_local_ip = "127.0.0.1";
  const uint16_t u16_port = 16888;
  const std::string str_svr_name = "servicename";
  meituan_mns::SGService sgservice;

  meituan_mns::ServiceDetail detail;
  detail.unifiedProto = true;

  sgservice.port = u16_port;
  sgservice.ip == str_local_ip;
  sgservice.serviceInfo.insert(make_pair(str_svr_name, detail));

  EXPECT_TRUE(p_cthrift_client_worker_->FilterAll(sgservice));

  EXPECT_TRUE(p_cthrift_client_worker_->FilterService(sgservice));
}

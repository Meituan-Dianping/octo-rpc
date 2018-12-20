
//
// Created by huixiangbo on 2017/10/12.
//

#include <string>
#include <vector>
#include <gtest/gtest.h>
#define private public
#define protected public
#include <cthrift/cthrift_name_service.h>
#undef private
#undef protected

using namespace std;
using namespace meituan_cthrift;

class CthriftNameServiceTest : public testing::Test {
 public:
  CthriftNameServiceTest() {
    p_cthrift_ns_ = new CthriftNameService();
  }
  ~CthriftNameServiceTest() {
    if (p_cthrift_ns_) {
      delete p_cthrift_ns_;
    }
  }
  virtual void SetUp() {
  }
  virtual void TearDown() {
  }
 public:
  CthriftNameService *p_cthrift_ns_;
};

TEST_F(CthriftNameServiceTest, Handle_PackDefaultSgservice) {

  const std::string str_svr_appkey = "test.appkey";
  const std::string str_local_ip = "127.0.0.1";
  const uint16_t u16_port = 16888;
  meituan_mns::SGService sgservice;
  CthriftNameService::PackDefaultSgservice(str_svr_appkey,
                                           str_local_ip,
                                           u16_port,
                                           &sgservice);

  EXPECT_TRUE(sgservice.port == u16_port);
  EXPECT_TRUE(sgservice.ip == str_local_ip);
  EXPECT_TRUE(sgservice.appkey == str_svr_appkey);
}

TEST_F(CthriftNameServiceTest, Handle_FetchOctoWeight) {
  EXPECT_TRUE(CthriftNameService::FetchOctoWeight(10.0, 10.0) == 10.0);
}

TEST_F(CthriftNameServiceTest, Handle_RegisterService) {
  const std::string str_svr_appkey = "test.appkey";
  const std::string str_local_ip = "127.0.0.1";
  const uint16_t u16_port = 16888;
  meituan_mns::SGService sgservice;

  sgservice.port = u16_port;
  sgservice.ip == str_local_ip;
  sgservice.appkey == str_svr_appkey;
  EXPECT_TRUE(CthriftNameService::RegisterService(sgservice) == -1);
}

TEST_F(CthriftNameServiceTest, Handle_GetSrvListFrom) {

  ServicePtr service = boost::make_shared<meituan_mns::getservice_res_param_t>();

  const std::string str_client_appkey = "test.client.appkey";
  const std::string str_srv_appkey = "test.srv.appkey";
  service->__set_localAppkey(str_client_appkey);
  service->__set_remoteAppkey(str_srv_appkey);
  service->__set_protocol("thrift");

  int ret = CthriftNameService::GetSrvListFrom(service);

  EXPECT_TRUE(ret == -1);
}


//
// Created by huixiangbo on 2017/10/12.
//

#include <string>
#include <vector>
#include <gtest/gtest.h>
#define private public
#define protected public
#include <cthrift/util/cthrift_common.h>
#undef private
#undef protected

using namespace std;
using namespace meituan_cthrift;

class CThriftCommonTest : public testing::Test {
 public:
  CThriftCommonTest() {

  }
  ~CThriftCommonTest() {

  }
  virtual void SetUp() {
  }
  virtual void TearDown() {
  }
 public:
};

TEST_F(CThriftCommonTest, Handle_ValidatePort) {
  EXPECT_TRUE(ValidatePort(200));
}

TEST_F(CThriftCommonTest, Handle_GetStringLimit) {
  EXPECT_TRUE (GetStringLimit() == 16 * 1024 * 1024);
}

TEST_F(CThriftCommonTest, Handle_strToLower) {
  std::string str_ret_high = "LOW";
  std::string str_ret_low = "low";
  std::string str_ret_ret = StrToLower(str_ret_high);
  EXPECT_TRUE(str_ret_ret == str_ret_low);
}

TEST_F(CThriftCommonTest, Handle_CheckDoubleEqual) {
  EXPECT_TRUE(CheckDoubleEqual(200.0, 200.0));
}

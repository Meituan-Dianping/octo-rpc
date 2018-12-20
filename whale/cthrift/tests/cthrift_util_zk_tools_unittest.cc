
//
// Created by huixiangbo on 2017/10/12.
//

#include <string>
#include <vector>
#include <gtest/gtest.h>
#define private public
#define protected public
#include <cthrift/util/zk_tools.h>
#undef private
#undef protected

using namespace std;
using namespace meituan_cthrift;

class ZkToolsTest : public testing::Test {
 public:
  ZkToolsTest() {
    p_zk_tools_ = new ZkTools();
  }
  ~ZkToolsTest() {
    if (p_zk_tools_) {
      delete p_zk_tools_;
    }
  }
  virtual void SetUp() {
  }
  virtual void TearDown() {
  }
 public:
  ZkTools *p_zk_tools_;
};

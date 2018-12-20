/*
 * Copyright (c) 2011-2018, Meituan Dianping. All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


#include <netdb.h>
#include <boost/algorithm/string.hpp>

#include "cthrift_common.h"

using namespace std;
using namespace muduo;
using namespace meituan_cthrift;

const int16_t kI16CpuNum = NumCPU();

const int32_t g_string_limit = 16 * 1024 * 1024;

AtomicInt32 g_atomic_i32_seq_id;

int16_t meituan_cthrift::NumCPU(void) {
  int16_t i16_cpu_num = static_cast<int16_t>(sysconf(_SC_NPROCESSORS_ONLN));
  return i16_cpu_num;
}

bool meituan_cthrift::CheckDoubleEqual(const double &d1, const double &d2) {
  return fabs(d1 - d2) < numeric_limits<double>::epsilon();
}

void meituan_cthrift::ReplaceAllDistinct(const string &old_value,
                                         const string &new_value,
                                         string *p_str) {
  for (string::size_type pos(0); pos != string::npos;
       pos += new_value.length()) {
    if ((pos = p_str->find(old_value, pos)) != string::npos)
      p_str->replace(pos, old_value.length(), new_value);
    else break;
  }
}

int32_t meituan_cthrift::GetStringLimit() {
  return g_string_limit;
}

bool meituan_cthrift::CheckOverTime(const muduo::Timestamp &timestamp,
                                    const double &d_overtime_secs,
                                    double *p_d_left_secs) {
  double
      d_time_diff_secs = timeDifference(Timestamp::now(), timestamp);

  CTHRIFT_LOG_DEBUG("d_time_diff_secs " << d_time_diff_secs);

  if (p_d_left_secs) {
    *p_d_left_secs = d_overtime_secs >
        d_time_diff_secs ? d_overtime_secs - d_time_diff_secs : 0;
  }

  if (d_overtime_secs < d_time_diff_secs
      || (CheckDoubleEqual(
          d_overtime_secs,
          d_time_diff_secs))) {
    CTHRIFT_LOG_WARN("overtime " << d_overtime_secs << "secs, timediff "
                                 << d_time_diff_secs << " secs");

    return true;
  }

  return false;
}

bool meituan_cthrift::ValidatePort(const unsigned int &port) {
  if (port < 1 || port > 65535) {
    return false;
  }
  return true;
}

string meituan_cthrift::StrToLower(const string &str_tmp) {
  string str_lower(str_tmp);
  transform(str_lower.begin(), str_lower.end(), str_lower.begin(), ::tolower);

  return str_lower;
}

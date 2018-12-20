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

#include "log4cplus.h"

namespace meituan_cthrift {

// root Logger
Logger debug_instance =
    log4cplus::Logger::getInstance(LOG4CPLUS_TEXT("debug"));
Logger info_instance = log4cplus::Logger::getInstance(LOG4CPLUS_TEXT("info"));
Logger warn_instance = log4cplus::Logger::getInstance(LOG4CPLUS_TEXT("warn"));
Logger error_instance =
    log4cplus::Logger::getInstance(LOG4CPLUS_TEXT("error"));
Logger fatal_instance =
    log4cplus::Logger::getInstance(LOG4CPLUS_TEXT("fatal"));

// non root Logger
Logger stat_instance =
    log4cplus::Logger::getInstance(LOG4CPLUS_TEXT("statLogger"));

}  // namespace cthrift
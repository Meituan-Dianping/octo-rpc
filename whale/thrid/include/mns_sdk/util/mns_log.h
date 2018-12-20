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

#ifndef OCTO_OPEN_SOURCE_MNS_SDK_SRC_UTIL_MNS_LOG_H_
#define OCTO_OPEN_SOURCE_MNS_SDK_SRC_UTIL_MNS_LOG_H_

#include <log4cplus/logger.h>
#include <log4cplus/loggingmacros.h>
#include <log4cplus/configurator.h>
#include <log4cplus/fileappender.h>
#include <log4cplus/consoleappender.h>
#include <log4cplus/layout.h>
#include <log4cplus/helpers/stringhelper.h>
#include <log4cplus/helpers/property.h>

namespace mns_sdk {

using std::auto_ptr;
using log4cplus::Logger;
using log4cplus::ConsoleAppender;
using log4cplus::FileAppender;
using log4cplus::Appender;
using log4cplus::Layout;
using log4cplus::PatternLayout;
using log4cplus::helpers::SharedObjectPtr;

extern Logger debug_instance;
extern Logger info_instance;
extern Logger warn_instance;
extern Logger error_instance;
extern Logger fatal_instance;
extern Logger stat_instance;

#define MNS_LOG_DEBUG(debugContent) \
    { LOG4CPLUS_DEBUG(debug_instance, debugContent); }
#define MNS_LOG_INFO(infoContent) \
    { LOG4CPLUS_INFO(info_instance, infoContent); }
#define MNS_LOG_WARN(warnContent) \
    { LOG4CPLUS_WARN(warn_instance, warnContent); }
#define MNS_LOG_ERROR(errorContent) \
    { LOG4CPLUS_ERROR(error_instance, errorContent); }
#define MNS_LOG_FATAL(fatalContent) \
    { LOG4CPLUS_ERROR(fatal_instance, fatalContent); }
#define MNS_LOG_STAT(statContent) \
    { LOG4CPLUS_INFO(stat_instance, statContent); }

}  // namespace mns_sdk

#endif  // OCTO_OPEN_SOURCE_MNS_SDK_SRC_UTIL_MNS_LOG_H_

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

#ifndef OCTO_OPEN_SOURCE_MNS_SDK_H_
#define OCTO_OPEN_SOURCE_MNS_SDK_H_

#include "mns_worker.h"

namespace mns_sdk {

//获取服务列表回调函数，允许为空，另外注意MNS库的子线程在执行该函数，注意线程安全问题
typedef boost::function<void(const std::vector<meituan_mns::SGService> &vec,
                             const std::string &str_svr_appkey)>
    SvrListCallback;

//获取服务列表的回调函数, 三个vector分别代表服务列表中新增的,需要删除的,
//内容需要改变的, 三者允许任意为空. 另外注意是MNS库的子线程在执行这个函数, 注意线程安全问题
typedef boost::function<void(const std::vector<meituan_mns::SGService> &vec_add,
                             const std::vector<meituan_mns::SGService> &vec_del,
                             const std::vector<meituan_mns::SGService> &vec_chg,
                             const std::string &str_svr_appkey)>
    UpdateSvrListCallback;

//启用clog统一日志，初始化日志系统，程序进程调用一次即可

int InitMNS(const std::string &mns_path, const double &sec,
            const double &timeout = 0.5);//初始化mns_sdk客户端，mns_path 环境配置文件,
// 并设置服务列表拉取间隔(s)，仅初始化一次即可。

//服务在一个进程中可以既调用StartSvr注册服务,又调用StartClient获取服务列表
//同步注册服务, 默认等待时间不超过50ms, 返回非0表示注册失败

//注册服务
int8_t StartSvr(const std::string &str_appkey,
                const int16_t &i16_port,
                const int32_t &i32_svr_type,   //0:thrift, 1:http, 2:other
                const std::string &str_proto_type = "thrift");

int8_t StartSvr(const std::string &str_appkey,
                const std::vector<std::string> &service_name_list,
                const int16_t &i16_port,
                const int32_t &i32_svr_type,   //0:thrift, 1:http, 2:other
                const std::string &str_proto_type,//"thrift", "http"
                const bool &b_is_uniform = false);


//返回服务列表，存入用户传入的res_svr_list中
int8_t getSvrList(const std::string &str_svr_appkey,
                  const std::string &str_cli_appkey,
                  const std::string &str_proto_type,
                  const std::string &str_service_name,
                  std::vector<meituan_mns::SGService> *p_svr_list);

//获取所有类型服务节点，推荐，支持在一条业务线程中多次绑定不同的str_svr_appkey。
int8_t StartClient(const std::string &str_svr_appkey,
                   const std::string &str_cli_appkey,
                   const std::string &str_proto_type, //client支持的协议类型 thrift/http/cellar..., 定期来取的svrlist将按照这个类型进行过滤
                   const std::string &str_service_name, //IDL文件中的service名字,可按这个名字来过滤服务节点,可填空串返回全部服务节点
                   const SvrListCallback &cb); //异步用回调定期获取服务列表, 返回非0表示参数错误(str_svr_appkey为空等)

int8_t StartClient(const std::string &str_svr_appkey,
                   const std::string &str_cli_appkey,
                   const std::string &str_proto_type, //client支持的协议类型 thrift/http/cellar..., 定期来取的svrlist将按照这个类型进行过滤
                   const std::string &str_service_name, //IDL文件中的service名字,可按这个名字来过滤服务节点,可填空串返回全部服务节点
                   const UpdateSvrListCallback &cb); //异步用回调定期获取服务列表, 返回非0表示参数错误(str_svr_appkey为空等)

int8_t AddSvrListCallback(const std::string &str_svr_appkey,
                          const SvrListCallback &cb,
                          std::string *p_err_info);

int8_t AddUpdateSvrListCallback(const std::string &str_svr_appkey,
                                const UpdateSvrListCallback &cb,
                                std::string *p_err_info);

void DestroyMNS(void);//退出时调用
}
#endif //OCTO_OPEN_SOURCE_MNS_SDK_H_


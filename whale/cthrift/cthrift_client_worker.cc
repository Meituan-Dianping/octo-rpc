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


#include "cthrift_tbinary_protocol.h"
#include "cthrift_client_worker.h"

using namespace std;
using namespace muduo::net;
using namespace meituan_cthrift;

const int32_t CthriftClientWorker::kI32HighWaterSize = 64 * 1024;  // 64K
const int8_t CthriftClientWorker::kI8TimeWheelNum = 2;

void ConnInfo::UptSgservice(const meituan_mns::SGService &sgservice) {
  double d_old_weight = CthriftNameService::FetchOctoWeight(sgservice_.fweight,
                                                            static_cast<double>(sgservice_.weight));
  double d_new_weight = CthriftNameService::FetchOctoWeight(sgservice.fweight,
                                                            static_cast<double>(sgservice.weight));

  CTHRIFT_LOG_DEBUG("d_old_weight " << d_old_weight << " d_new_weight "
                                    << d_new_weight);

  if (!CheckDoubleEqual(d_old_weight, d_new_weight)) {
    CTHRIFT_LOG_DEBUG("need update weight buf");

    // real conn NOT erase, just del index
    p_map_weight_tcpclientwp_->erase(it_map_weight_tcpclientwp_index_);

    it_map_weight_tcpclientwp_index_ =
        p_map_weight_tcpclientwp_->insert(std::make_pair(
            d_new_weight,
            sp_tcpclient_));
  }

  sgservice_ = sgservice;
}

void ConnInfo::setSp_tcpclient_(const TcpClientSharedPtr &sp_tcpclient) {
  if (CTHRIFT_UNLIKELY(sp_tcpclient_.get())) {
    CTHRIFT_LOG_ERROR("client ip: " << (sp_tcpclient_->connection()
        ->peerAddress()).toIp() << " port: "
                                    << (sp_tcpclient_->connection()->peerAddress()).toPort()
                                    << " replace");

    p_map_weight_tcpclientwp_->erase(it_map_weight_tcpclientwp_index_);
  }

  sp_tcpclient_ = sp_tcpclient;

  double d_weight =
      CthriftNameService::FetchOctoWeight(sgservice_.fweight,
                                          static_cast<double>(sgservice_.weight));
  CTHRIFT_LOG_DEBUG("dweight " << d_weight);

  it_map_weight_tcpclientwp_index_ =
      p_map_weight_tcpclientwp_->insert(std::make_pair(d_weight,
                                                       sp_tcpclient_));
}

CthriftClientWorker::CthriftClientWorker(
    const std::string &str_svr_appkey,
    const std::string &str_cli_appkey,
    const std::string &str_serviceName_filter,
    const int32_t &i32_port_filter,
    const std::string &str_server_ip,
    const int16_t &i16_server_port)
    : cond_avaliable_conn_ready_(mutexlock_avaliable_conn_ready_),
      str_svr_appkey_(str_svr_appkey),
      str_client_appkey_(str_cli_appkey),
      str_serviceName_filter_(str_serviceName_filter),
      i32_port_filter_(i32_port_filter),
      i8_destructor_flag_(0),
      str_server_ip_(str_server_ip),
      i16_server_port_(i16_server_port),
      b_user_set_ip(!str_server_ip.empty()),
      p_async_event_loop_(NULL) {
  // atomic_avaliable_conn_num_ defalut init by value
  // start real worker thread
  sp_event_thread_ =
      boost::make_shared<muduo::net::EventLoopThread>();
  p_event_loop_ = sp_event_thread_->startLoop();
  p_event_loop_->runInLoop(boost::bind(&CthriftClientWorker::InitWorker,
                                       this));  // will use event_loop in

  boost::shared_ptr<TMemoryBuffer>
      tmp_buf = boost::make_shared<TMemoryBuffer>();
  sp_p_tmemorybuffer_ = new boost::shared_ptr<TMemoryBuffer>();
  *sp_p_tmemorybuffer_ = tmp_buf;

  boost::shared_ptr<CthriftTBinaryProtocolWithTMemoryBuf>
      tmp_prot = boost::make_shared<
      CthriftTBinaryProtocolWithTMemoryBuf>(*sp_p_tmemorybuffer_);

  // A memory buffer is a tranpsort, NO SERVER/CLIENT specified since just serial
  sp_p_cthrift_tbinary_protocol_ =
      new boost::shared_ptr<CthriftTBinaryProtocolWithTMemoryBuf>();
  *sp_p_cthrift_tbinary_protocol_ = tmp_prot;
}

int8_t CthriftClientWorker::CheckRegion(const double &d_weight) {
  if (d_weight < CthriftNameService::kDSecondRegionMin) {
    return kRegionTypeThree;
  } else if (d_weight < CthriftNameService::kDFirstRegionMin) {
    return kRegionTypeTwo;
  }

  return kRegionTypeOne;
}

bool ConnInfo::CheckConnHealthy(void) const {
  if (CTHRIFT_UNLIKELY(!(sp_tcpclient_.get()))) {
    CTHRIFT_LOG_ERROR("sp_tcpconn invalid appkey: " << sgservice_.appkey
                                                    << " ip:" << sgservice_.ip
                                                    << " port: "
                                                    << sgservice_.port);
    return false;
  }

  muduo::net::TcpConnectionPtr sp_tcpconn = sp_tcpclient_->connection();
  if (CTHRIFT_UNLIKELY(!sp_tcpconn || !(sp_tcpconn.get()))) {
    CTHRIFT_LOG_ERROR("sp_tcpconn invalid appkey: " << sgservice_.appkey
                                                    << " ip:" << sgservice_.ip
                                                    << " port: "
                                                    << sgservice_.port);
    return false;
  }

  if (CTHRIFT_UNLIKELY(!(sp_tcpconn->connected()))) {
    CTHRIFT_LOG_DEBUG("address: " << (sp_tcpconn->peerAddress()).toIpPort()
                                  << "NOT connected");
    return false;
  }

  if (CTHRIFT_UNLIKELY((sp_tcpconn->getContext()).empty())) {
    CTHRIFT_LOG_ERROR("address: " << (sp_tcpconn->peerAddress()).toIpPort()
                                  << " context empty");    // NOT clear here
    return false;
  }

  Context4WorkerSharedPtr sp_context;
  try {
    sp_context = boost::any_cast<Context4WorkerSharedPtr>
        (sp_tcpconn->getContext());
  } catch (boost::bad_any_cast &e) {
    CTHRIFT_LOG_ERROR("bad_any_cast:" << e.what() << " peer address "
                                      << (sp_tcpconn->peerAddress()).toIpPort());
    return false;
  }

  if (CTHRIFT_UNLIKELY(sp_context->b_highwater || sp_context->b_occupied)) {
    CTHRIFT_LOG_WARN("address: " << (sp_tcpconn->peerAddress()).toIpPort() <<
                                 " b_highwater " << sp_context->b_highwater
                                 << " b_occupied "
                                 << sp_context->b_occupied << " ignore");
    return false;
  }

  return true;
}

int8_t CthriftClientWorker::ChooseNextReadyConn(
    TcpClientWeakPtr *p_wp_tcpcli) {
  if (CTHRIFT_UNLIKELY(p_multimap_weight_wptcpcli_->empty())) {
    CTHRIFT_LOG_ERROR("multimap_weight_wptcpcli_ empty");
    return -1;
  }

  UnorderedMapIpPort2ConnInfoSP iter_ipport_spconninfo;

  boost::unordered_map<double, vector<TcpClientWeakPtr> > map_weight_vec;
  vector<double> vec_weight;

  double d_last_weight = -1.0;
  double d_total_weight = 0.0;
  int8_t i8_stop_region = kRegionTypeTwo;  // init not necessary, but for safe
  string str_port;

  muduo::net::TcpConnectionPtr sp_tcpconn;

  MultiMapIter iter = p_multimap_weight_wptcpcli_->begin();
  while (p_multimap_weight_wptcpcli_->end() != iter) {
    TcpClientSharedPtr sp_tcpcli((iter->second).lock());
    if (CTHRIFT_UNLIKELY(!sp_tcpcli || !(sp_tcpcli.get()))) {
      CTHRIFT_LOG_ERROR("tcpclient NOT avaliable");

      p_multimap_weight_wptcpcli_->erase(iter++);
      continue;
    }

    sp_tcpconn = sp_tcpcli->connection();
    if (CTHRIFT_UNLIKELY(!sp_tcpconn)) {
      CTHRIFT_LOG_INFO("NOT connected yet");
      ++iter;
      continue;
    }

    CTHRIFT_LOG_DEBUG("Address: " << (sp_tcpconn->peerAddress()).toIpPort()
                                  << " weight " << iter->first);

    try {
      str_port = boost::lexical_cast<std::string>(
          (sp_tcpconn->peerAddress()).toPort());
    } catch (boost::bad_lexical_cast &e) {

      CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                    << "tcp connnect peer port : "
                                                    << (sp_tcpconn->peerAddress()).toPort());

      ++iter;
      continue;
    }

    iter_ipport_spconninfo = map_ipport_spconninfo_.find(
        (sp_tcpconn->peerAddress()).toIp() + ":" + str_port);
    if (CTHRIFT_UNLIKELY(
        iter_ipport_spconninfo == map_ipport_spconninfo_.end())) {
      CTHRIFT_LOG_ERROR("Not find ip:"
                            << (sp_tcpconn->peerAddress()).toIp() << " port:"
                            << str_port << " in map_ipport_spconninfo_");

      p_multimap_weight_wptcpcli_->erase(iter++);
      continue;
    }

    if (!(iter_ipport_spconninfo->second->CheckConnHealthy())) {
      ++iter;
      continue;
    }

    // weight random choose algorithm
    // 1. sum all weight(one weight = single weight * same weight conn num)
    // 2. random total_weight,get a random_weight
    // 3. choose random_weight region from all weight.
    if (!CheckDoubleEqual(d_last_weight, iter->first)) {  // new weight
      if (CTHRIFT_LIKELY(
          !CheckDoubleEqual(d_last_weight, -1.0))) {  // NOT init
        if (i8_stop_region <= CheckRegion(iter->first)) {  //i f already get
          // conn and next region reach, stop
          CTHRIFT_LOG_DEBUG("stop region " << i8_stop_region << " "
              "iter->first " << iter->first);
          break;
        }

        d_total_weight += d_last_weight * static_cast<double>
        (map_weight_vec[d_last_weight]
                .size
                    ());
      } else {
        i8_stop_region =
            static_cast<int8_t>(CheckRegion(iter->first)
                + 1);  // set stop region by the first weight

        CTHRIFT_LOG_DEBUG("i8_stop_region set to be " << i8_stop_region);
      }

      vec_weight.push_back(iter->first);
      d_last_weight = iter->first;
    }

    map_weight_vec[iter->first].push_back(iter->second);
    ++iter;
  }

  if (CTHRIFT_UNLIKELY(0 == vec_weight.size())) {
    CTHRIFT_LOG_INFO("Not avaliable conn can be choosed, maybe all occupied");
    return 1;
  }
  // 将所有候选节点的权重进行求和：d_total_weight
  // 将权重看作一条线段，d_total_weight是线段长度
  // 不同权重的节点占据这条线段的不同部分
  // 产生0~d_total_weight一个随机数，落入的权重线段某个区域;
  // 选择该区域归属的节点列表；从这个节点列表中随机选择一个节点。
  d_total_weight +=
      d_last_weight * static_cast<double>(map_weight_vec[d_last_weight].size
          ());

  CTHRIFT_LOG_DEBUG("d_total_weight " << d_total_weight);
  // 伪随机数在小范围下不均匀（0~1)，放大1000倍解决该问题
  d_total_weight *= static_cast<double>(1000.0);

  double d_choose = 0.0;
  double d_tmp = 0.0;
  // 产生的随机数落入权重线段某个区域，后面的while其实是在找这个“区域”
  double d_random_weight = fmod(static_cast<double>(rand()), d_total_weight);
  vector<double>::iterator it_vec = vec_weight.begin();
  while (vec_weight.end() != it_vec) {
    // 伪随机数在小范围下不均匀（0~1)，放大1000倍进行平滑处理
    d_tmp += (*it_vec) * static_cast<double>(map_weight_vec[*it_vec].size()) *
        static_cast<double>(1000.0);
    if (d_tmp > d_random_weight) {
      d_choose = *it_vec;
      break;
    }

    ++it_vec;
  }

  boost::unordered_map<double, vector<TcpClientWeakPtr> >::iterator
      it_map = map_weight_vec.find(d_choose);
  if (CTHRIFT_UNLIKELY(it_map == map_weight_vec.end())) {
    CTHRIFT_LOG_ERROR("not find weight " << d_choose);
    return -1;
  }

  if (1 == (it_map->second).size()) {
    *p_wp_tcpcli = *((it_map->second).begin());
  } else {
    CTHRIFT_LOG_DEBUG((it_map->second).size() << " conn need be choose one "
        "equally");

    *p_wp_tcpcli = (it_map->second)[rand() % ((it_map->second).size())];
  }

  return 0;
}

void
CthriftClientWorker::UpdateSvrList(
    const vector<meituan_mns::SGService> &vec_sgservice) {
  vector<meituan_mns::SGService>::const_iterator it_vec;
  boost::unordered_map<string, meituan_mns::SGService>::iterator
      it_map_sgservice;

  string str_port;

  vector<meituan_mns::SGService> vec_sgservice_add;
  vector<meituan_mns::SGService> vec_sgservice_del;
  vector<meituan_mns::SGService> vec_sgservice_chg;

  if (CTHRIFT_UNLIKELY(
      0 == map_ipport_sgservice_.size() && 0 == vec_sgservice.size())) {
    CTHRIFT_LOG_WARN("Init svr list but empty srvlist");
  } else if (CTHRIFT_UNLIKELY(0 == map_ipport_sgservice_.size())) {
    it_vec = vec_sgservice.begin();
    CTHRIFT_LOG_INFO("Init svr list for appkey " << it_vec->appkey);

    while (it_vec != vec_sgservice.end()) {
      if (CTHRIFT_UNLIKELY(2 != it_vec->status)) {
        CTHRIFT_LOG_DEBUG("svr info: "
                              << CthriftNameService::SGService2String(*it_vec)
                              << " IGNORED");
        ++it_vec;
        continue;
      }

      try {
        str_port = boost::lexical_cast<std::string>(it_vec->port);
      } catch (boost::bad_lexical_cast &e) {
        CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                      << "it_vec->port "
                                                      << it_vec->port);

        ++it_vec;
        continue;
      }

      map_ipport_sgservice_.insert(make_pair(it_vec->ip + ":" + str_port,
                                             *it_vec));

      vec_sgservice_add.push_back(*(it_vec++));
    }
  } else if (CTHRIFT_UNLIKELY(0 == vec_sgservice.size())) {
    CTHRIFT_LOG_WARN("vec_sgservice empty");

    it_map_sgservice = map_ipport_sgservice_.begin();
    while (it_map_sgservice != map_ipport_sgservice_.end()) {
      vec_sgservice_del.push_back(it_map_sgservice->second);
      map_ipport_sgservice_.erase(it_map_sgservice++);
    }
  } else {
    boost::unordered_map<string, meituan_mns::SGService>
        map_tmp_locate_del(map_ipport_sgservice_);

    it_vec = vec_sgservice.begin();
    while (it_vec != vec_sgservice.end()) {
      if (CTHRIFT_UNLIKELY(2 != it_vec->status)) {
        CTHRIFT_LOG_DEBUG("svr info: "
                              << CthriftNameService::SGService2String(*it_vec)
                              << " IGNORED");
        ++it_vec;
        continue;
      }

      try {
        str_port = boost::lexical_cast<std::string>(it_vec->port);
      } catch (boost::bad_lexical_cast &e) {
        CTHRIFT_LOG_DEBUG("boost::bad_lexical_cast :" << e.what()
                                                      << "it_vec->port "
                                                      << it_vec->port);

        ++it_vec;
        continue;
      }

      string str_key(it_vec->ip + ":" + str_port);
      it_map_sgservice = map_ipport_sgservice_.find(str_key);
      if (map_ipport_sgservice_.end() == it_map_sgservice) {
        CTHRIFT_LOG_DEBUG("ADD svr list info: "
                              << CthriftNameService::SGService2String(*it_vec));

        vec_sgservice_add.push_back(*it_vec);
        map_ipport_sgservice_.insert(make_pair(str_key, *it_vec));
      } else {
        map_tmp_locate_del.erase(str_key);

        if (it_map_sgservice->second != *it_vec) {
          CTHRIFT_LOG_DEBUG("UPDATE svr list. old info: "
                                << CthriftNameService::SGService2String(
                                    it_map_sgservice->second));
          CTHRIFT_LOG_DEBUG(" new info: "
                                << CthriftNameService::SGService2String(*it_vec));

          it_map_sgservice->second = *it_vec;

          vec_sgservice_chg.push_back(*it_vec);

          map_tmp_locate_del.erase(str_key);
        }
      }

      ++it_vec;
    }

    if (map_tmp_locate_del.size()) {
      CTHRIFT_LOG_DEBUG("DEL svr list");

      it_map_sgservice = map_tmp_locate_del.begin();
      while (it_map_sgservice != map_tmp_locate_del.end()) {
        CTHRIFT_LOG_DEBUG("del svr info: "
                              << CthriftNameService::SGService2String(
                                  it_map_sgservice->second));

        vec_sgservice_del.push_back(it_map_sgservice->second);
        map_ipport_sgservice_.erase((it_map_sgservice++)->first);
      }
    }
  }

  AddSrv(vec_sgservice_add);
  DelSrv(vec_sgservice_del);
  ChgSrv(vec_sgservice_chg);
}

void CthriftClientWorker::InitSgagentHandlerThread(void) {
  p_event_loop_sgagent_->runInLoop(
      boost::bind(&CthriftClientWorker::GetSvrList, this));

  p_event_loop_sgagent_->runEvery(CthriftNameService::kDGetSvrListIntervalSecs,
                                  boost::bind(&CthriftClientWorker::GetSvrList,
                                              this));
}

void CthriftClientWorker::InitWorker(void) {
  CTHRIFT_LOG_INFO("InitWorker begin");
  p_multimap_weight_wptcpcli_ =
      new multimap<double, TcpClientWeakPtr, WeightSort>;  // exit del, safe

  if (b_user_set_ip) {
    CTHRIFT_LOG_INFO("InitWorker b_user_set_ip  ip:"
                         << str_server_ip_ << " port:" << str_server_ip_);

    vector<meituan_mns::SGService> list;
    meituan_mns::SGService sg;
    sg.ip = str_server_ip_;
    sg.status = 2;
    sg.fweight = 10.0;
    sg.weight = 10;
    sg.port = i16_server_port_;
    list.push_back(sg);

    p_event_loop_->runInLoop(boost::bind(&CthriftClientWorker::UpdateSvrList,
                                         this,
                                         list));
  } else {
    sp_event_thread_sgagent_ =
        boost::make_shared<muduo::net::EventLoopThread>();

    p_event_loop_sgagent_ = sp_event_thread_sgagent_->startLoop();
    p_event_loop_sgagent_->runInLoop(boost::bind(
        &CthriftClientWorker::InitSgagentHandlerThread,
        this));
  }

  CTHRIFT_LOG_INFO("InitWorker end");
}

bool CthriftClientWorker::FilterAll(const meituan_mns::SGService &sg) {
  return sg.serviceInfo.find(str_serviceName_filter_) == sg.serviceInfo.end()
      || i32_port_filter_ != sg.port;
}

bool CthriftClientWorker::FilterService(const meituan_mns::SGService &sg) {
  return sg.serviceInfo.find(str_serviceName_filter_) == sg.serviceInfo.end();
}

bool CthriftClientWorker::FilterPort(const meituan_mns::SGService &sg) {
  return i32_port_filter_ != sg.port;
}

void CthriftClientWorker::GetSvrList(void) {
  ServicePtr service = boost::make_shared<meituan_mns::getservice_res_param_t>();

  service->__set_localAppkey(str_client_appkey_);
  service->__set_remoteAppkey(str_svr_appkey_);
  service->__set_protocol("thrift");

  int ret = CthriftNameService::GetSrvListFrom(service);

  if (0 != ret) {
    CTHRIFT_LOG_WARN("GetSrvListFrom failed");
    return;
  }

  std::vector<meituan_mns::SGService> servicelist = service->serviceList;

  if (CTHRIFT_LIKELY(str_serviceName_filter_.empty()
                         && -1 == i32_port_filter_)) {
    // 正常case
    CTHRIFT_LOG_DEBUG("recv vec_sgservice.size " << servicelist.size());
    for (size_t i = 0; i < servicelist.size(); i++) {
      CTHRIFT_LOG_DEBUG("[" << i << "]" << ": "
                            << CthriftNameService::SGService2String(servicelist[i]));
    }
  } else {
    // 过滤case
    vector<meituan_mns::SGService> &filter_list = servicelist;
    if (!str_serviceName_filter_.empty() && -1 != i32_port_filter_) {
      filter_list.erase(remove_if(filter_list.begin(), filter_list.end(),
                                  boost::bind(&CthriftClientWorker::FilterAll,
                                              this,
                                              _1)),
                        servicelist.end());

    } else if (str_serviceName_filter_.empty()) {
      filter_list.erase(remove_if(filter_list.begin(), filter_list.end(),
                                  boost::bind(&CthriftClientWorker::FilterPort,
                                              this,
                                              _1)),
                        servicelist.end());
    } else {
      filter_list.erase(remove_if(filter_list.begin(), filter_list.end(),
                                  boost::bind(&CthriftClientWorker::FilterService,
                                              this,
                                              _1)),
                        servicelist.end());
    }
    CTHRIFT_LOG_DEBUG("filter serviceName vec_sgservice.size "
                          << servicelist.size());
  }
  p_event_loop_->runInLoop(boost::bind(&CthriftClientWorker::UpdateSvrList,
                                       this,
                                       servicelist));
}

void CthriftClientWorker::AddSrv(
    const vector<meituan_mns::SGService> &vec_add_sgservice) {
  string str_port;
  vector<meituan_mns::SGService> vec_chg_sgservice;
  MultiMapIter it_multimap;

  vector<meituan_mns::SGService>::const_iterator
      it_sgservice = vec_add_sgservice
      .begin();
  while (it_sgservice != vec_add_sgservice.end()) {
    const meituan_mns::SGService &sgservice = *it_sgservice;

    try {
      str_port = boost::lexical_cast<std::string>(sgservice.port);
    } catch (boost::bad_lexical_cast &e) {
      CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                    << "sgservice.port "
                                                    << sgservice.port);

      ++it_sgservice;
      continue;
    }

    ConnInfoSharedPtr
        &sp_conninfo = map_ipport_spconninfo_[sgservice.ip + ":" + str_port];

    if (CTHRIFT_UNLIKELY(sp_conninfo.get())) {
      CTHRIFT_LOG_WARN("svr " << CthriftNameService::SGService2String(sgservice)
                              << " already exist in map_ipport_sptcpcli, just change it");

      vec_chg_sgservice.push_back(sgservice);
      ++it_sgservice;

      continue;
    }

    sp_conninfo = boost::make_shared<ConnInfo>(sgservice,
                                               p_multimap_weight_wptcpcli_);

    boost::shared_ptr<muduo::net::TcpClient>
        sp_tcp_cli_tmp = boost::make_shared<muduo::net::TcpClient>(
        p_event_loop_,
        muduo::net::InetAddress(
            sgservice.ip,
            static_cast<uint16_t>(sgservice.port)),
        "client worker for appkey "
            + sgservice.appkey);

    sp_conninfo->setSp_tcpclient_(sp_tcp_cli_tmp);  // will set weight buf inside

    TcpClientSharedPtr &sp_tcpcli = sp_conninfo->getSp_tcpclient_();

    sp_tcpcli->setConnectionCallback(
        boost::bind(&CthriftClientWorker::OnConn,
                    this,
                    _1));

    sp_tcpcli->setMessageCallback(
        boost::bind(&CthriftClientWorker::OnMsg,
                    this,
                    _1,
                    _2,
                    _3));

    sp_tcpcli->setWriteCompleteCallback(
        boost::bind(&CthriftClientWorker::OnWriteComplete,
                    this,
                    _1));

    sp_tcpcli->enableRetry();
    sp_tcpcli->connect();

    ++it_sgservice;
  }

  if (vec_chg_sgservice.size()) {
    CTHRIFT_LOG_ERROR("Add trans to Chg");
    ChgSrv(vec_chg_sgservice);
  }
}

void CthriftClientWorker::DelSrv(
    const vector<meituan_mns::SGService> &vec_del_sgservice) {
  string str_port;

  vector<meituan_mns::SGService>::const_iterator it_sgservice
      = vec_del_sgservice.begin();
  while (it_sgservice != vec_del_sgservice.end()) {
    const meituan_mns::SGService &sgservice = *it_sgservice;

    try {
      str_port = boost::lexical_cast<std::string>(sgservice.port);
    } catch (boost::bad_lexical_cast &e) {
      CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                    << "sgservice.port "
                                                    << sgservice.port);

      ++it_sgservice;
      continue;
    }

    // TODO(grace exit??)
    // tcpclient exit will close connection, conninfo exit will clear weight buf
    map_ipport_spconninfo_.erase(sgservice.ip + ":" + str_port);

    ++it_sgservice;
  }
}

void CthriftClientWorker::ChgSrv(
    const vector<meituan_mns::SGService> &vec_chg_sgservice) {
  string str_port;
  string str_key;

  vector<meituan_mns::SGService> vec_add_sgservice;

  vector<meituan_mns::SGService>::const_iterator
      it_sgservice = vec_chg_sgservice.begin();
  while (it_sgservice != vec_chg_sgservice.end()) {
    const meituan_mns::SGService &sgservice = *it_sgservice;

    try {
      str_port = boost::lexical_cast<std::string>(sgservice.port);
    } catch (boost::bad_lexical_cast &e) {
      CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                    << "sgservice.port "
                                                    << sgservice.port);

      ++it_sgservice;
      continue;
    }

    str_key.assign(sgservice.ip + ":" + str_port);
    UnorderedMapStr2SpConnInfoIter
        it_map = map_ipport_spconninfo_.find(str_key);
    if (it_map == map_ipport_spconninfo_.end()) {
      CTHRIFT_LOG_WARN("Not find " << str_key << " for appkey "
                                   << sgservice.appkey
                                   << " in map_ipport_spconninfo_, readd it");

      vec_add_sgservice.push_back(sgservice);
    } else {
      it_map->second->UptSgservice(sgservice);
    }

    ++it_sgservice;
  }

  if (vec_add_sgservice.size()) {
    CTHRIFT_LOG_ERROR("Chg trans to Add");
    AddSrv(vec_add_sgservice);
  }
}

void CthriftClientWorker::OnConn(
    const muduo::net::TcpConnectionPtr &conn) {
  CTHRIFT_LOG_INFO(conn->localAddress().toIpPort() << " -> "
                                                   << conn->peerAddress().toIpPort()
                                                   << " is "
                                                   << (conn->connected() ? "UP"
                                                                         : "DOWN"));

  if (conn->connected()) {
    string str_port;

    try {
      str_port
          = boost::lexical_cast<std::string>((conn->peerAddress()).toPort());
    } catch (boost::bad_lexical_cast &e) {
      CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                    << "toPort "
                                                    << (conn->peerAddress()).toPort()
                                                    << "conn peerAddr "
                                                    << (conn->peerAddress()).toIpPort());

      conn->shutdown();
      return;
    }

    // check in map
    UnorderedMapIpPort2ConnInfoSP unordered_map_iter =
        map_ipport_spconninfo_.find(
            (conn->peerAddress()).toIp() + ":" + str_port);
    if (CTHRIFT_UNLIKELY(
        unordered_map_iter == map_ipport_spconninfo_.end())) {
      CTHRIFT_LOG_ERROR("conn peerAddr " << (conn->peerAddress()).toIpPort()
                                         << " localaddr "
                                         << (conn->localAddress()).toIpPort()
                                         << " NOT find key in map_ipport_spconninfo_");

      conn->shutdown();
      return;
    }

    conn->setTcpNoDelay(true);
    conn->setHighWaterMarkCallback(
        boost::bind(&CthriftClientWorker::OnHighWaterMark,
                    this,
                    _1,
                    _2),
        kI32HighWaterSize);  // every conn, 64K buff

    boost::shared_ptr<ConnContext4Worker> conn_context_ptr =
        boost::make_shared<ConnContext4Worker>(unordered_map_iter->second);
    conn->setContext(conn_context_ptr);

    Context4WorkerSharedPtr tmp;
    try {
      tmp = boost::any_cast<Context4WorkerSharedPtr>(conn->getContext());
    } catch (boost::bad_any_cast &e) {
      CTHRIFT_LOG_ERROR("bad_any_cast:" << e.what());
      return;
    }

    // tmp->t_last_conn_time_ = time(0);

    if (CTHRIFT_UNLIKELY(1 == atomic_avaliable_conn_num_.incrementAndGet())) {
      muduo::MutexLockGuard lock(mutexlock_avaliable_conn_ready_);
      cond_avaliable_conn_ready_.notifyAll();
    }
  } else {
    if (CTHRIFT_UNLIKELY((conn->getContext()).empty())) {
      CTHRIFT_LOG_WARN("conn context empty, maybe shutdown when conn");
    } else {
      Context4WorkerSharedPtr sp_context;
      try {
        sp_context =
            boost::any_cast<Context4WorkerSharedPtr>(conn->getContext());
      } catch (boost::bad_any_cast &e) {
        CTHRIFT_LOG_ERROR("bad_any_cast:" << e.what());
        return;
      }

      /* clear send queue
      for (int i = 0; i < static_cast<int>((sp_context->queue_send).size());
           i++) {
        map_id_sharedcontextsp_.erase((sp_context->queue_send).front());
        (sp_context->queue_send).pop();
      }*/

      // make sure this conn NOT decrement num before, and then check current
      // available num
      if (!(sp_context->b_highwater) && !(sp_context->b_occupied)
          && (0 >= (atomic_avaliable_conn_num_.decrementAndGet()))) {
        atomic_avaliable_conn_num_.getAndSet(0);  // adjust for safe

        CTHRIFT_LOG_WARN("atomic_avaliable_conn_num_ 0");
      }
    }
  }
}

void CthriftClientWorker::HandleThriftMsg(
    const muduo::net::TcpConnectionPtr &conn,
    const int32_t &length,
    uint8_t *buf) {
  // deserial seqid for map requset
  (*sp_p_tmemorybuffer_)->resetBuffer(buf, static_cast<uint32_t>(length),
                                      TMemoryBuffer::COPY);

  int32_t i32_seqid = (*sp_p_cthrift_tbinary_protocol_)->GetSeqID();

  if (CTHRIFT_UNLIKELY(0 >= i32_seqid)) {
    CTHRIFT_LOG_ERROR("seqid " << i32_seqid << " str_appkey " <<
                               str_svr_appkey_ << " close connection to "
                               << (conn->peerAddress()).toIpPort());

    conn->shutdown();
    return;
  }

  string str_id;
  try {
    str_id = boost::lexical_cast<std::string>(i32_seqid);
  } catch (boost::bad_lexical_cast &e) {
    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << "seqid " << i32_seqid
                                                  << " str_appkey "
                                                  << str_svr_appkey_
                                                  << " close connection to "
                                                  << (conn->peerAddress()).toIpPort());

    conn->shutdown();
    return;
  }

  MapID2SharedPointerIter
      map_iter = map_id_sharedcontextsp_.find(str_id);
  if (CTHRIFT_UNLIKELY(map_id_sharedcontextsp_.end() == map_iter)) {
    CTHRIFT_LOG_ERROR("Not find id " << str_id << " maybe timeout"
                                     "conn from:"<<(conn->peerAddress()).toIpPort());

  } else {
    SharedContSharedPtr &sp_shared = map_iter->second;

    CTHRIFT_LOG_DEBUG("id " << str_id << " send & recv cost "
                            << timeDifference(Timestamp::now(),
                                              sp_shared->timestamp_cliworker_send)
                            << " secs");
    if (sp_shared->async_flag) {
      // copy一份recv数据，避免异步回调线程与IO线程竞争读/写
      // mutex不能保证这里逻辑的正确性，因为OnMsg一直被调用，muduo::buffer一直被重复填充数据
      // rpc通信的message可能较大，采用栈上存储存在撑爆线程栈风险
      uint32_t buf_size = static_cast<uint32_t>(length);
      uint8_t *recv_buf = reinterpret_cast<uint8_t *>(std::malloc(buf_size));
      std::memcpy(recv_buf, buf, buf_size);

      p_async_event_loop_->runInLoop(boost::bind(
          &CthriftClientWorker::AsyncCallback,
          this,
          buf_size,
          recv_buf,
          sp_shared));  // will use event_loop in
    } else if (sp_shared->IsTimeout()) {
      CTHRIFT_LOG_WARN("seq id "
                           << str_id << " already expire, discard the msg");
    } else {      // when transport timeout during write readbuf, still
      // safe
      sp_shared->ResetReadBuf(buf, static_cast<uint32_t>(length));
      muduo::MutexLockGuard lock(*(sp_shared->p_mutexlock_conn_ready));
      sp_shared->p_cond_ready_read->notifyAll();

      // int clientStatus = 0;   NOT used by cmtrace, just fill
      // CLIENT_RECV(clientStatus);

      CTHRIFT_LOG_DEBUG("write and notify for id " << str_id
                                                   << " done");
    }

    // 异步、同步删除map中item都在这里处理；异步化的回调中传递了shared_ptr，这里删除后异步回调中内存仍然可用
    map_id_sharedcontextsp_.erase(sp_shared->str_id);
  }
}

void CthriftClientWorker::HandleMsg(const muduo::net::TcpConnectionPtr &conn,
                                    Context4WorkerSharedPtr &sp_context_worker,
                                    muduo::net::Buffer *buffer) {
  const int32_t length = sp_context_worker->i32_want_size;
  uint8_t *p_ui8_req_buf
      = reinterpret_cast<uint8_t *>(const_cast<char *>(buffer->peek()));

  HandleThriftMsg(conn, length, p_ui8_req_buf);

  buffer->retrieve(static_cast<size_t>(length));
  sp_context_worker->enum_state = kExpectFrameSize;

  if (CTHRIFT_UNLIKELY(buffer->readableBytes())) {
    CTHRIFT_LOG_DEBUG("still " << buffer->readableBytes()
                               << " left in receive buf");
  } else {
    CTHRIFT_LOG_DEBUG("retrieve all");
    sp_context_worker->b_occupied = false;
  }
}

void CthriftClientWorker::OnMsg(const muduo::net::TcpConnectionPtr &conn,
                                muduo::net::Buffer *buffer,
                                muduo::Timestamp receiveTime) {
  CTHRIFT_LOG_DEBUG((conn->peerAddress()).toIpPort() << " msg received "
                                                     << (buffer->toStringPiece()).data()
                                                     << " len "
                                                     << buffer->readableBytes());

  if (CTHRIFT_UNLIKELY((conn->getContext()).empty())) {
    CTHRIFT_LOG_ERROR("peer address " << conn->peerAddress().toIpPort()
                                      << " context empty");
    conn->shutdown();
    return;
  }

  Context4WorkerSharedPtr sp_context_worker;
  try {
    sp_context_worker =
        boost::any_cast<Context4WorkerSharedPtr>(conn->getContext());
  } catch (boost::bad_any_cast &e) {
    CTHRIFT_LOG_ERROR("bad_any_cast:" << e.what());
    return;
  }

  while (1) {
    if (sp_context_worker->enum_state == kExpectFrameSize) {
      if (sizeof(int32_t) <= buffer->readableBytes()) {
        sp_context_worker->i32_want_size =
            static_cast<uint32_t>(buffer->readInt32());
        sp_context_worker->enum_state = kExpectFrame;
      } else {
        CTHRIFT_LOG_WARN("not enough size for protocol "
                             "thrift total length, wait for more");
        return;
      }
    } else if (sp_context_worker->enum_state == kExpectFrame) {
      if (buffer->readableBytes()
          >= static_cast<size_t>(sp_context_worker->i32_want_size)) {
        // sp_context_worker->t_last_recv_time_ = time(0);
        HandleMsg(conn, sp_context_worker, buffer);

      } else {
        CTHRIFT_LOG_DEBUG("body len " << buffer->readableBytes()
                                      << " < want len "
                                      << sp_context_worker->i32_want_size
                                      << " continue wait");
        break;
      }
    }
  }
}

void
CthriftClientWorker::OnWriteComplete(
    const muduo::net::TcpConnectionPtr &conn) {
  CTHRIFT_LOG_DEBUG(conn->localAddress().toIpPort() << " -> "
                                                    << conn->peerAddress().toIpPort()
                                                    << " OnWriteComplete");

  if (CTHRIFT_UNLIKELY((conn->getContext()).empty())) {
    CTHRIFT_LOG_ERROR("address: " << (conn->peerAddress()).toIpPort() << " "
        "context empty");    // NOT clear here
    return;
  }

  Context4WorkerSharedPtr conn_info;
  try {
    conn_info = boost::any_cast<Context4WorkerSharedPtr>(conn->getContext());
  } catch (boost::bad_any_cast &e) {
    CTHRIFT_LOG_ERROR("bad_any_cast:" << e.what());
    return;
  }

  conn_info->b_highwater = false;

  if (CTHRIFT_UNLIKELY(1 == atomic_avaliable_conn_num_.incrementAndGet())) {
    muduo::MutexLockGuard lock(mutexlock_avaliable_conn_ready_);
    cond_avaliable_conn_ready_.notifyAll();
  }
}

void
CthriftClientWorker::OnHighWaterMark(const muduo::net::TcpConnectionPtr &conn,
                                     size_t len) {
  CTHRIFT_LOG_INFO((conn->localAddress()).toIpPort() << " -> "
                                                     << (conn->peerAddress()).toIpPort()
                                                     << " OnHighWaterMark");

  if (CTHRIFT_UNLIKELY((conn->getContext()).empty())) {
    CTHRIFT_LOG_ERROR("address: " << (conn->peerAddress()).toIpPort() << " "
        "context empty");    // NOT clear here
    return;
  }

  Context4WorkerSharedPtr conn_info;
  try {
    conn_info =
        boost::any_cast<Context4WorkerSharedPtr>(conn->getContext());
  } catch (boost::bad_any_cast &e) {
    CTHRIFT_LOG_ERROR("bad_any_cast:" << e.what());
    return;
  }

  conn_info->b_highwater = true;

  if (0 >= atomic_avaliable_conn_num_.decrementAndGet()) {
    atomic_avaliable_conn_num_.getAndSet(0);  // adjust for safe

    CTHRIFT_LOG_WARN("atomic_avaliable_conn_num_ 0");
  }
}

void CthriftClientWorker::SendTransportReq(SharedContSharedPtr sp_shared) {
  Buffer send_buf;
  if (0 != sp_shared->GetWriteBuf(&send_buf)) {
    return;
  }

  CTHRIFT_LOG_DEBUG("send_buf.size " << send_buf.readableBytes());

  TcpClientWeakPtr wp_tcpcli;
  if (ChooseNextReadyConn(&wp_tcpcli)) {
    return;
  }

  TcpClientSharedPtr sp_tcpcli
      (wp_tcpcli.lock());  // already check valid in ChooseNextReadyConn


  string str_port;

  try {
    str_port = boost::lexical_cast<std::string>(
        (sp_tcpcli->connection()->peerAddress()).toPort());
  } catch (boost::bad_lexical_cast &e) {
    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << " ip:"
                                                  << (sp_tcpcli->connection()->peerAddress()).toIp()
                                                  << " port:"
                                                  << (sp_tcpcli->connection()->peerAddress()).toPort());
    return;
  }

  UnorderedMapIpPort2ConnInfoSP
      iter_ipport_spconninfo = map_ipport_spconninfo_.find(
      (sp_tcpcli->connection()->peerAddress()).toIp() + ":" + str_port);
  if (CTHRIFT_UNLIKELY(
      iter_ipport_spconninfo == map_ipport_spconninfo_.end())) {
    CTHRIFT_LOG_ERROR("Not find ip:"
                          << (sp_tcpcli->connection()->peerAddress()).toIp()
                          << " port:"
                          << str_port << " in map_ipport_spconninfo_");
    return;
  }

  if (CTHRIFT_UNLIKELY((sp_tcpcli->connection()->getContext()).empty())) {
    CTHRIFT_LOG_ERROR("conn context empty");
    return;
  }

  if (CTHRIFT_UNLIKELY(CheckOverTime(sp_shared->timestamp_start,
                                     static_cast<double>(sp_shared->i32_timeout_ms)
                                         / MILLISENCOND_COUNT_IN_SENCOND,
                                     0))) {
    CTHRIFT_LOG_WARN("before send, appkey " << str_svr_appkey_ << " id "
                                            << sp_shared->str_id
                                            << " timeout return");
    return;
  }

  sp_tcpcli->connection()->send(&send_buf);

  CTHRIFT_LOG_DEBUG("send id " << sp_shared->str_id << " done");

  Context4WorkerSharedPtr sp_context;
  try {
    sp_context =
        boost::any_cast<Context4WorkerSharedPtr>(
            sp_tcpcli->connection()->getContext());
  } catch (boost::bad_any_cast &e) {
    CTHRIFT_LOG_ERROR("bad_any_cast:" << e.what());
    return;
  }

  // sp_context->t_last_send_time_ = time(0);

  sp_shared->wp_send_conn = sp_tcpcli->connection();
  sp_shared->timestamp_cliworker_send = Timestamp::now();

  // sp_context->b_occupied = true;

  // (sp_context->queue_send).push(sp_shared->str_id);

  map_id_sharedcontextsp_[sp_shared->str_id] = sp_shared;
}

void CthriftClientWorker::TimewheelKick() {
  cnxt_entry_circul_buf_.push_back(CnxtEntryBucket());
}

// 触发式资源分配
void CthriftClientWorker::EnableAsync(const int32_t &i32_timeout_ms) {
  if (!p_async_event_loop_) {
    sp_async_event_thread_ =
        boost::make_shared<muduo::net::EventLoopThread>();
    p_async_event_loop_ = sp_async_event_thread_->startLoop();
    p_async_event_loop_->setContext(kTaskStateInit);
  }

  // Init Garbage Collection timewheel
  cnxt_entry_circul_buf_.resize(kI8TimeWheelNum);
  double timeout = i32_timeout_ms > MILLISENCOND_COUNT_IN_SENCOND ?
                   static_cast<double >(i32_timeout_ms)
                       / MILLISENCOND_COUNT_IN_SENCOND :
                   1.0;
  p_event_loop_->runEvery(timeout,
                          boost::bind(&CthriftClientWorker::TimewheelKick,
                                      this));
}

void CthriftClientWorker::AsyncCallback(const uint32_t &size,
                                        uint8_t *recv_buf,
                                        SharedContSharedPtr sp_shared) {
  double d_left_secs = 0.0;
  if (CheckOverTime(sp_shared->timestamp_start,
                    static_cast<double>(sp_shared->i32_timeout_ms)
                        / MILLISENCOND_COUNT_IN_SENCOND, &d_left_secs)) {
    CTHRIFT_LOG_WARN("async wait appkey " << str_svr_appkey_ << " id "
                                          << sp_shared->str_id
                                          << " already "
                                          << sp_shared->i32_timeout_ms
                                          << " ms for readbuf, timeout");
    p_async_event_loop_->setContext(kTaskStateTimeOut);
  } else {
    p_async_event_loop_->setContext(kTaskStateSuccess);
  }
  // 避免手动释放内存；内存统一由TMemoryBuffer对象管理
  sp_shared->ResetRecvBuf(recv_buf, size);
  // 回调用户业务逻辑
  try {
    sp_shared->cob_();
  } catch (...) {
    CTHRIFT_LOG_ERROR("Catch exception in async callback");
  }
}

void CthriftClientWorker::AsyncSendReq(SharedContSharedPtr sp_shared) {
  // 发送时检测任务是否已经超时，如果超时不进行网络传输直接调用callback，反馈超时
  double d_left_secs = 0.0;
  if (CheckOverTime(sp_shared->timestamp_start,
                    static_cast<double>(sp_shared->i32_timeout_ms)
                        / MILLISENCOND_COUNT_IN_SENCOND, &d_left_secs)) {
    CTHRIFT_LOG_WARN("async task already timeout, before send packet");

    uint32_t buf_size = 0;
    uint8_t *recv_buf = NULL;
    p_async_event_loop_->runInLoop(
        boost::bind(&CthriftClientWorker::AsyncCallback,
                    this, buf_size, recv_buf, sp_shared));
    return;
  }

  Buffer send_buf;
  if (0 != sp_shared->GetAsyncWriteBuf(&send_buf)) {
    return;
  }

  CTHRIFT_LOG_DEBUG("send_buf.size " << send_buf.readableBytes());

  TcpClientWeakPtr wp_tcpcli;
  if (ChooseNextReadyConn(&wp_tcpcli)) {
    CTHRIFT_LOG_ERROR("No candidate connection to send packet, "
                          "async task will be dropped");
    return;
  }

  TcpClientSharedPtr sp_tcpcli
      (wp_tcpcli.lock());  // already check valid in ChooseNextReadyConn


  string str_port;

  try {
    str_port = boost::lexical_cast<std::string>(
        (sp_tcpcli->connection()->peerAddress()).toPort());
  } catch (boost::bad_lexical_cast &e) {
    CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                  << " ip:"
                                                  << (sp_tcpcli->connection()->peerAddress()).toIp()
                                                  << " port:"
                                                  << (sp_tcpcli->connection()->peerAddress()).toPort());
    return;
  }

  UnorderedMapIpPort2ConnInfoSP
      iter_ipport_spconninfo = map_ipport_spconninfo_.find(
      (sp_tcpcli->connection()->peerAddress()).toIp() + ":" + str_port);
  if (CTHRIFT_UNLIKELY(
      iter_ipport_spconninfo == map_ipport_spconninfo_.end())) {
    CTHRIFT_LOG_ERROR("Not find ip:"
                          << (sp_tcpcli->connection()->peerAddress()).toIp()
                          << " port:"
                          << str_port << " in map_ipport_spconninfo_");
    return;
  }

  if (CTHRIFT_UNLIKELY((sp_tcpcli->connection()->getContext()).empty())) {
    CTHRIFT_LOG_ERROR("conn context empty");
    return;
  }

  if (CTHRIFT_UNLIKELY(CheckOverTime(sp_shared->timestamp_start,
                                     static_cast<double>(
                                         sp_shared->i32_timeout_ms)
                                         / MILLISENCOND_COUNT_IN_SENCOND,
                                     0))) {
    CTHRIFT_LOG_WARN("before send, appkey " << str_svr_appkey_ << " id "
                                            << sp_shared->str_id
                                            << " timeout return");
    return;
  }

  sp_tcpcli->connection()->send(&send_buf);

  CTHRIFT_LOG_DEBUG("send id " << sp_shared->str_id << " done");

  Context4WorkerSharedPtr sp_context;
  try {
    sp_context =
        boost::any_cast<Context4WorkerSharedPtr>(
            sp_tcpcli->connection()->getContext());
  } catch (boost::bad_any_cast &e) {
    CTHRIFT_LOG_ERROR("bad_any_cast:" << e.what());
    return;
  }

  sp_shared->wp_send_conn = sp_tcpcli->connection();
  sp_shared->timestamp_cliworker_send = Timestamp::now();

  map_id_sharedcontextsp_[sp_shared->str_id] = sp_shared;
  cnxt_entry_circul_buf_.back().insert(
      boost::make_shared<CnxtEntry>(sp_shared, this));
}

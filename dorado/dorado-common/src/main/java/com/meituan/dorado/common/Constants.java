/*
 * Copyright 2018 Meituan Dianping. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meituan.dorado.common;

import org.apache.commons.lang3.StringUtils;

public class Constants {

    public static final String INFRA_NAME = "Dorado";
    public static final String THRIFT_IFACE = "Iface";
    // context信息 key
    public static final String LOCAL_IP = "localIp";
    public static final String LOCAL_PORT = "localPort";
    public static final String REMOTE_IP = "remoteIp";
    public static final String REMOTE_PORT = "remotePort";
    public static final String REQUEST_SIZE = "requestSize";
    public static final String RESPONSE_SIZE = "responseSize";
    public static final String RPC_REQUEST = "rpcRequest";
    public static final String TRACE_PARAM = "traceParam";
    public static final String SERVICE_IFACE = "serviceInterface";
    public static final String TRACE_FILTER_FINISHED = "traceFilterFinished";
    public static final String TRACE_REPORT_FINISHED = "traceReportFinished";
    public static final String TRUE = "true";

    // 分阶段时间戳记录
    public static final String TRACE_TIMELINE = "traceTimeline";
    // 是否分阶段记录时间戳
    public static final String TRACE_IS_RECORD_TIMELINE = "traceRecordTimeline";

    public static final int MESSAGE_TYPE_SERVICE = 0;
    public static final int MESSAGE_TYPE_HEART = 1;

    public static final int DEFAULT_SERVER_PORT = 9001;
    public static final int DEFAULT_HTTP_SERVER_PORT = 5080;

    public static final int DEFAULT_WEIGHT = 10;
    public static final String DEFAULT_SERIALIZE = "thriftCodec";

    // 注册
    public static final String REGISTRY_WAY_KEY = "registryWay";
    public static final String REGISTRY_ADDRESS_KEY = "registryAddress";
    public static final String DEFAULT_REGISTRY_WAY = "mns";
    public static final String REGISTRY_MOCK_WAY = "mock";
    public static final String DEFAULT_REGISTRY_POLICY_TYPE = "failback";
    // 注册重试周期
    public static final int DEFAULT_REGISTRY_RETRY_PERIOD = 1000 * 5;

    // 集群容错策略
    public static final String DEFAULT_CLUSTER_POLICY = "failfast";
    // 负载均衡
    public static final String DEFAULT_LOADBALANCE_POLICY = "random";
    // 路由策略
    public static final String DEFAULT_ROUTER_POLICY = "none";

    public static final int DEFAULT_CONN_TIMEOUT = 1000;
    public static final int DEFAULT_RECONN_INTERVAL = 2000;
    public static final int DEFAULT_TIMEOUT = 1000;
    public static final int DEFAULT_SHUTDOWN_TIMEOUT = 1000;
    public static final int RECONN_FAIL_DEGRADE_TIME = DEFAULT_RECONN_INTERVAL * 5;

    public static final int NIO_CONN_THREADS = 1;
    public static final int DEFAULT_IO_WORKER_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2;
    public static final int DEFAULT_BIZ_CORE_WORKER_THREAD_COUNT = 10;
    public static final int DEFAULT_BIZ_MAX_WORKER_THREAD_COUNT = 256;
    public static final int DEFAULT_BIZ_WORKER_QUEUES = 0;
    public static final long IDLE_THREAD_KEEP_ALIVE_SECONDS = 0L;
    public static final int DEFAULT_FILTER_PRIORITY = 0;

    public static final String CONTENT_TYPE_JSON = "application/json";

    public static final String ASYNC = "async";

    public static final String RESPONSE_FUTURE = "responseFuture";

    public static final String COLON = ":";

    public static final String LINK_SUB_CLASS_SYMBOL = "$";

    public static final String NORMAL_DISCONNCT_INFO = "Connection reset by peer";

    public static final String UNKNOWN = "unknown";

    public enum ProtocolType {
        Thrift("thrift");

        String name;

        ProtocolType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static boolean isThrift(String protocolName) {
            if (StringUtils.isBlank(protocolName) ||
                    Thrift.getName().equalsIgnoreCase(protocolName)) {
                return true;
            }
            return false;
        }
    }

    public enum EnvType {
        PROD("prod", 3), STAGE("stage", 2), TEST("test", 1);

        private String envName;
        private int envCode;

        EnvType(String envName, int envCode) {
            this.envName = envName;
            this.envCode = envCode;
        }

        public String getEnvName() {
            return envName;
        }

        public int getEnvCode() {
            return envCode;
        }

        public static EnvType getEnvType(String envName) {
            switch (envName) {
                case "prod":
                    return PROD;
                case "stage":
                    return STAGE;
                case "test":
                    return TEST;
                default:
                    return TEST;

            }
        }

        public static String getEnvName(int envCode) {
            switch (envCode) {
                case 3:
                    return PROD.envName;
                case 2:
                    return STAGE.envName;
                case 1:
                    return TEST.envName;
                default:
                    return TEST.envName;

            }
        }
    }
}

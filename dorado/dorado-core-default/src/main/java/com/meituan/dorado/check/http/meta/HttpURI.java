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

package com.meituan.dorado.check.http.meta;

import com.meituan.dorado.common.RpcRole;

import java.util.HashSet;
import java.util.Set;

public enum HttpURI {

    // 服务端  基本信息
    SERVICE_BASE_INFO("/service.info", RpcRole.PROVIDER),

    /**
     * 以下待开源支持
     */
    // 接口调用
    SERVICE_INVOKE_PREFIX("/invoke", RpcRole.PROVIDER),

    // 服务端  服务方法信息
    SERVICE_METHOD_INFO("/method.info", RpcRole.PROVIDER),
    // 调用端  服务提供者信息
//    PROVIDER_INFO("/provider.info", RpcRole.INVOKER),
    // 服务端/调用端  鉴权信息
//    AUTH_INFO("/auth.info", RpcRole.MULTIROLE),
    // 调用端 降级节点信息
//    DEGRADE_PROVIDER_INFO("/degradedProvider.info", RpcRole.INVOKER),
    // 服务端 接口信息的json schema
//    SERVICE_JSON_SCHEMA("/service.jsonSchema", RpcRole.PROVIDER),

    UNKNOWN("", null);

    private String uri;
    private RpcRole role;

    HttpURI(String uri, RpcRole role) {
        this.uri = uri;
        this.role = role;
    }

    public String uri() {
        return uri;
    }

    @Override
    public String toString() {
        return uri;
    }

    public static Set<String> getSupportUriOfRole(RpcRole role) {
        Set<String> uris = new HashSet<String>();
        for (HttpURI uri : HttpURI.values()) {
            if (uri == UNKNOWN) {
                continue;
            }
            if (RpcRole.MULTIROLE == role || RpcRole.MULTIROLE == uri.role || uri.role == role) {
                uris.add(uri.uri);
            }
        }
        return uris;
    }

    public static HttpURI toHttpCheckURI(String path) {
        for (HttpURI uri : HttpURI.values()) {
            if (uri.uri.equals(path)) {
                return uri;
            }
        }
        return UNKNOWN;
    }
}

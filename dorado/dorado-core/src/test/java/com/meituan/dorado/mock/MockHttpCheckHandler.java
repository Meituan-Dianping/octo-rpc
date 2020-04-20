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
package com.meituan.dorado.mock;

import com.meituan.dorado.check.http.HttpCheckHandler;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.transport.http.HttpSender;

import java.util.Map;

public class MockHttpCheckHandler implements HttpCheckHandler {

    private RpcRole rpcRole;


    @Override
    public void handle(HttpSender httpSender, String uri, byte[] content, Map<String, String> headers) {
    }

    @Override
    public void setRole(RpcRole role) {
        this.rpcRole = role;
    }

    @Override
    public RpcRole getRole() {
        return rpcRole;
    }
}

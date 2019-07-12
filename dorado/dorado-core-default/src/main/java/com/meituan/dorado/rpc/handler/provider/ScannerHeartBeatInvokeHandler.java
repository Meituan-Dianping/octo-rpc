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
package com.meituan.dorado.rpc.handler.provider;

import com.meituan.dorado.codec.octo.meta.MessageType;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.rpc.handler.HeartBeatInvokeHandler;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.DefaultResponse;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;
import com.meituan.dorado.util.ClazzUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScannerHeartBeatInvokeHandler implements HeartBeatInvokeHandler {

    private static final Logger logger = LoggerFactory.getLogger(ScannerHeartBeatInvokeHandler.class);

    @Override
    public Response handle(Request request) {
        if (request.getMessageType() != MessageType.ScannerHeartbeat.getValue()) {
            throw new RpcException("Message type do not match " + ClazzUtil.getClazzSimpleName(this.getClass()));
        }
        Response response = buildResponse(request);
        logger.debug("Scanner heartbeat from {}", NetUtil.toIpPort(request.getRemoteAddress()));
        return response;
    }

    @Override
    public Response buildResponse(Request request) {
        return new DefaultResponse((DefaultRequest) request);
    }

    @Override
    public RpcRole getRole() {
        return RpcRole.PROVIDER;
    }
}

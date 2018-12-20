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
package com.meituan.dorado.transport.support;

import com.meituan.dorado.bootstrap.invoker.ServiceInvocationRepository;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.transport.Channel;
import com.meituan.dorado.transport.ChannelHandler;
import com.meituan.dorado.transport.meta.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 每个client的Channel handler
 */
public class InvokerChannelHandler implements ChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(InvokerChannelHandler.class);

    private void doReceived(Channel channel, Response response) {
        ResponseFuture future = ServiceInvocationRepository.removeAndGetFuture(response.getSeq());
        if (future != null) {
            future.received(response);
        }
    }

    @Override
    public void received(Channel channel, Object message) {
        if (message instanceof Response) {
            doReceived(channel, ((Response) message));
        } else {
            logger.warn("Should not reach here, it means you message({}) is ignored", message);
        }
    }

    @Override
    public void send(Channel channel, Object message) {
        channel.send(message);
    }

    @Override
    public void destroy() {
        // client do nothing
    }

    @Override
    public void exceptionCaught(Channel channel, Throwable exception) {
        if (exception.getCause() != null && exception.getCause() instanceof TimeoutException) {
            return;
        }
        String serverIP = channel.getRemoteAddress().getHostName();
        String message = exception.getMessage();
        if (message != null && message.contains(Constants.NORMAL_DISCONNCT_INFO)) {
            logger.warn("ExceptionCaught(serverIP IP:{}): {}", serverIP, message);
        } else {
            logger.error("ExceptionCaught(serverIP IP:{})", serverIP, exception);
        }
    }

    @Override
    public void connected(Channel channel) {
        // do nothing
    }

    @Override
    public void disconnected(Channel channel) {
        // do nothing
    }

    @Override
    public void closed(Channel channel) {
        // do nothing , channel will be closed in transport module
    }
}

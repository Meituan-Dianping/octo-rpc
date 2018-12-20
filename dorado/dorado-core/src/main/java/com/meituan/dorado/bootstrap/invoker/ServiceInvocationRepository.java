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
package com.meituan.dorado.bootstrap.invoker;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.common.thread.DefaultThreadFactory;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.rpc.handler.HandlerFactory;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 调用端请求记录 && 时间轮超时检查
 */
public class ServiceInvocationRepository {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInvocationRepository.class);

    private static final ConcurrentMap<Long, Request> invocations = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, ResponseFuture> futures = new ConcurrentHashMap<>();

    // 时间间隔：10ms  槽数：512
    private static HashedWheelTimer hashedWheelTimer;

    private static final HandlerFactory handlerFactory;

    static {
        try {
            if (hashedWheelTimer == null) {
                hashedWheelTimer = new HashedWheelTimer(new DefaultThreadFactory("DoradoTimeoutScheduler"),
                        10, TimeUnit.MILLISECONDS);
                hashedWheelTimer.start();
            }

            handlerFactory = ExtensionLoader.getExtension(HandlerFactory.class);
        } catch (Throwable e) {
            logger.error("Init hashedWheelTimer Exception.", e);
            throw new RuntimeException(e);
        }
    }

    public static void putRequestAndFuture(Request request, ResponseFuture future) {
        long seqId = request.getSeq();
        invocations.put(seqId, request);
        futures.put(seqId, future);
    }

    public static Request getRequest(long sequence) {
        return invocations.get(sequence);
    }

    public static ResponseFuture removeAndGetFuture(long sequence) {
        invocations.remove(sequence);
        return futures.remove(sequence);
    }

    public static void addTimeoutTask(final Request request, final ResponseFuture future) {
        try {
            if (hashedWheelTimer != null) {
                hashedWheelTimer.newTimeout(new TimerTask() {
                    @Override
                    public void run(Timeout timeout) {
                        try {
                            if (invocations.get(request.getSeq()) != null) {
                                Response timeoutResponse = handlerFactory
                                        .getInvocationHandler(Constants.MESSAGE_TYPE_SERVICE, RpcRole.INVOKER)
                                        .buildResponse(invocations.get(request.getSeq()));

                                timeoutResponse.setException(new TimeoutException(
                                        "Request timeout[" + future.getTimeout() + "ms]" +
                                                ", interface=" + request.getServiceName() +
                                                "|method=" + request.getData().getMethod().getName() +
                                                "|provider=" + (request.getClient() != null ?
                                                request.getClient().getRemoteAddress() : "unKnown")));
                                removeAndGetFuture(request.getSeq());
                                future.received(timeoutResponse);
                            }
                        } catch (Exception e) {
                            logger.error("HashedWheelTimer running exception.", e);
                        }
                    }
                }, future.getTimeout(), TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("Add new timeout Task exception.", e);
        }
    }

    public static void stopTimeoutTask() {
        if (hashedWheelTimer != null) {
            hashedWheelTimer.stop();
            hashedWheelTimer = null;
            logger.info("Stop HashedWheelTimer");
        }
    }
}

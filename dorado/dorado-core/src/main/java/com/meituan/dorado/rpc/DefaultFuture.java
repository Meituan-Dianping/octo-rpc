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
package com.meituan.dorado.rpc;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.exception.RemoteException;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.common.util.CommonUtil;
import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.rpc.handler.HandlerFactory;
import com.meituan.dorado.trace.InvokerAsyncTrace;
import com.meituan.dorado.transport.Channel;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;

import java.util.concurrent.*;

public class DefaultFuture<V> implements ResponseFuture<V> {

    private final long seq;
    private final long genTimestamp;
    private int timeout;
    private ResponseCallback callback;
    private Executor executor;

    private final CountDownLatch finished;
    private V value = null;
    private Throwable error = null;
    private Channel channel;
    private Request request;
    private Response response;

    public DefaultFuture(Request request) {
        this.finished = new CountDownLatch(1);
        this.genTimestamp = System.currentTimeMillis();
        this.seq = request.getSeq();
        this.request = request;
        this.timeout = request.getTimeout();
    }

    public DefaultFuture(Request request, Channel channel, int timeout) {
        this(request);
        this.channel = channel;
        this.timeout = timeout;
    }

    public DefaultFuture(Executor executor, Request request, Channel channel, int timeout) {
        this(request, channel, timeout);
        this.executor = executor;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    public long getSeq() {
        return seq;
    }

    @Override
    public void received(Response response) {
        if (response == null) {
            // 不应该到此, 防御性判断
            response = ExtensionLoader.getExtension(HandlerFactory.class)
                    .getInvocationHandler(Constants.MESSAGE_TYPE_SERVICE, RpcRole.INVOKER).buildResponse(request);
            response.setException(new RpcException("Response is null, it shouldn't happen"));
        }
        this.response = response;
        wrapException();
        if (response.getResult() != null) {
            this.value = (V) response.getResult().getReturnVal();
        }
        richResponse(request);
        finished.countDown();

        if (AsyncContext.isAsyncReq(request.getData())) {
            InvokerAsyncTrace.clientAsyncRecv(response.getException(), request.getData());
        }

        if (callback != null) {
            onResponse();
        }
    }

    @Override
    public V get() throws ExecutionException {
        return get(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public V get(long timeout, TimeUnit timeUnit) throws ExecutionException {
        Exception exception = null;
        if (!isDone()) {
            try {
                finished.await(timeout, timeUnit);
            } catch (InterruptedException e) {
                exception = e;
            }
            if (!isDone() && exception == null) {
                exception = new TimeoutException("GetRequest timeout, timeout:" + timeout);
            }
        }
        if (exception != null) {
            error = exception;
        }
        return getValue();
    }

    @SuppressWarnings("unchecked")
    private V getValue() throws ExecutionException {
        if (error != null) {
            throw new ExecutionException(error);
        } else {
            return value;
        }
    }

    @Override
    public boolean isDone() {
        return finished.getCount() == 0;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public void setCallback(ResponseCallback<V> callback) {
        this.callback = callback;
    }

    @Override
    public void cancel() {
        error = new CancellationException("Request future has been canceled.");
        finished.countDown();
    }

    @Override
    public void setCause(Throwable cause) {
        error = cause;
        finished.countDown();
    }

    public Channel getChannel() {
        return channel;
    }

    public Request getRequest() {
        return request;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    private void richResponse(Request request) {
        int responseSize = CommonUtil.objectToInt(response.getAttachment(Constants.RESPONSE_SIZE), -1);
        request.putAttachment(Constants.RESPONSE_SIZE, responseSize);
    }

    private void wrapException() {
        Throwable exception = response.getException();
        if (exception instanceof RemoteException || exception instanceof TimeoutException) {
            this.error = exception;
            return;
        }
        if (exception != null) {
            String remoteIpPort = NetUtil.toIpPort(request.getRemoteAddress());
            this.error = new RemoteException("Remote invoke failed, interface=" + request.getServiceName() + "|method=" +
                    request.getData().getMethod().getName() + "|provider=" + remoteIpPort, exception);
        }
    }

    private void onResponse() {
        if (error != null) {
            if (executor == null) {
                callback.onError(error);
            } else {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(error);
                    }
                });
            }
        } else if (value != null) {
            if (executor == null) {
                callback.onComplete(value);
            } else {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onComplete(value);
                    }
                });
            }
        }
    }
}
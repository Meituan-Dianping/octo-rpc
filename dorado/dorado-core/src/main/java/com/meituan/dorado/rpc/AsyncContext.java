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
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.transport.meta.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AsyncContext {

    private static final Logger logger = LoggerFactory.getLogger(AsyncContext.class);

    private static final ThreadLocal<AsyncContext> LOCAL = new ThreadLocal<AsyncContext>() {
        @Override
        protected AsyncContext initialValue() {
            return new AsyncContext();
        }
    };

    private final Map<String, Object> attachments = new HashMap<>();
    private ResponseFuture<?> future;

    public <T> ResponseFuture<T> asyncCall(Callable<T> callable) {
        try {
            try {
                setAttachment(Constants.ASYNC, Boolean.TRUE);
                final T o = callable.call();
                if (o != null) {
                    logger.warn("Do async call but actual action is sync");
                    return new MockFuture(o);
                }
            } catch (Exception e) {
                throw new RpcException(e);
            } finally {
                removeAttachment(Constants.ASYNC);
            }
        } catch (final RpcException e) {
            return new MockFuture(e);
        }
        return ((ResponseFuture<T>) getContext().getFuture());
    }

    public static boolean isAsyncReq(RpcInvocation data) {
        return data != null && Boolean.TRUE.equals(data.getAttachment(Constants.ASYNC));
    }

    public static AsyncContext getContext() {
        return LOCAL.get();
    }

    public static void removeContext() {
        LOCAL.remove();
    }

    public <T> ResponseFuture<T> getFuture() {
        return (ResponseFuture<T>) future;
    }

    public void setFuture(ResponseFuture<?> future) {
        this.future = future;
    }

    public Object getAttachment(String key) {
        return attachments.get(key);
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public AsyncContext setAttachment(String key, Object value) {
        if (value == null) {
            attachments.remove(key);
        } else {
            attachments.put(key, value);
        }
        return this;
    }

    public AsyncContext removeAttachment(String key) {
        attachments.remove(key);
        return this;
    }

    public static class MockFuture<V> implements ResponseFuture {

        private V value = null;
        private ResponseCallback<V> callback;

        public MockFuture(V value) {
            this.value = value;
        }

        @Override
        public Response getResponse() {
            return null;
        }

        @Override
        public void setCallback(ResponseCallback callback) {
            this.callback = callback;
        }

        @Override
        public int getTimeout() {
            return 0;
        }

        @Override
        public void received(Response response) {
        }

        @Override
        public void cancel() {
            // do nothing
        }

        @Override
        public void setCause(Throwable cause) {

        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public V get() throws ExecutionException {
            if (value instanceof RpcException) {
                throw new ExecutionException(((RpcException) value).getCause());
            }
            return value;
        }

        @Override
        public V get(long timeout, TimeUnit unit)
                throws ExecutionException {
            return get();
        }
    }
}

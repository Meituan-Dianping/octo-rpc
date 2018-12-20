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

import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.transport.meta.Response;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public interface ResponseFuture<V> {

    Response getResponse();

    void setCallback(ResponseCallback<V> callback);

    int getTimeout();

    void received(Response response);

    V get() throws InterruptedException, ExecutionException;

    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    boolean isDone();

    void cancel();

    void setCause(Throwable cause);
}
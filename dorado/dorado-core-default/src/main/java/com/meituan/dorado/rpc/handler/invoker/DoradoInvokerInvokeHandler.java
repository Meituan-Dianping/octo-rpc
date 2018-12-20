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
package com.meituan.dorado.rpc.handler.invoker;

import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.DefaultResponse;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.transport.meta.Response;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;

public class DoradoInvokerInvokeHandler extends AbstractInvokerInvokeHandler {

    @Override
    public Response buildResponse(Request request) {
        return new DefaultResponse((DefaultRequest) request);
    }

    @Override
    public Response handle(Request request) throws Throwable {
        try {
            return super.handle(request);
        } catch (Throwable e) {
            if (e.getCause() != null && e.getCause() instanceof TException && e.getCause() instanceof TBase) {
                throw e.getCause();
            }
            throw e;
        }
    }
}

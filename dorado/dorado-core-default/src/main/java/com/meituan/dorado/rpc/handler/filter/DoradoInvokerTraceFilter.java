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
package com.meituan.dorado.rpc.handler.filter;

import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.common.util.VersionUtil;
import com.meituan.dorado.rpc.handler.invoker.AbstractInvokerTraceFilter;
import com.meituan.dorado.trace.meta.TraceParam;
import com.meituan.dorado.trace.meta.TraceTimeline;
import com.meituan.dorado.trace.meta.TransportTraceInfo;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.util.ClazzUtil;

public class DoradoInvokerTraceFilter extends AbstractInvokerTraceFilter {

    @Override
    protected TraceParam genTraceParam(Request request, TraceTimeline timeline) {
        DefaultRequest req = (DefaultRequest) request;
        TraceParam param = new TraceParam();
        String serviceName = ClazzUtil.getClazzSimpleName(req.getServiceInterface());
        param.setSpanName(req.getRemoteAppkey() + ":" + serviceName + "." + req.getData().getMethod().getName());
        param.setLocalAppkey(req.getAppkey());
        param.setLocalIp(req.getClientIp());
        param.setRemoteAppkey(req.getRemoteAppkey());
        param.setRemoteIp(NetUtil.toIP(request.getRemoteAddress()));
        param.setRemotePort(NetUtil.toPort(request.getRemoteAddress()));
        param.setVersion(VersionUtil.getDoradoVersion());
        param.setProtocol(req.getProtocol());
        param.setTimeout(req.getTimeout());

        if (timeline.isEnable()) {
            param.setStartTimestamp(timeline.getTimestamp(TraceTimeline.INVOKE_START_TS));
        } else {
            param.setStartTimestamp(req.getStartTimestamp());
        }
        param.setTraceTimeline(timeline);

        TransportTraceInfo transportTraceInfo = new TransportTraceInfo(req.getAppkey());
        req.setTransportTraceInfo(transportTraceInfo);

        return param;
    }
}

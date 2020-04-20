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

import com.meituan.dorado.bootstrap.provider.ServicePublisher;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.util.CommonUtil;
import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.common.util.VersionUtil;
import com.meituan.dorado.rpc.handler.provider.AbstractProviderTraceFilter;
import com.meituan.dorado.trace.meta.TraceParam;
import com.meituan.dorado.trace.meta.TraceTimeline;
import com.meituan.dorado.trace.meta.TransportTraceInfo;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.Request;
import com.meituan.dorado.util.ClazzUtil;

import java.util.Map;

public class DoradoProviderTraceFilter extends AbstractProviderTraceFilter {

    @Override
    protected TraceParam genTraceParam(Request request, TraceTimeline timeline) {
        DefaultRequest req = (DefaultRequest) request;
        Map<String, Object> attachments = req.getAttachments();
        TraceParam param = new TraceParam();
        String serviceName = ClazzUtil.getClazzSimpleName(req.getServiceInterface());
        param.setSpanName(serviceName + "." + req.getData().getMethod().getName());
        param.setLocalAppkey(ServicePublisher.getAppkey());
        param.setLocalIp(CommonUtil.objectToStr(attachments.get(Constants.LOCAL_IP), Constants.UNKNOWN));
        param.setLocalPort(CommonUtil.objectToInt(attachments.get(Constants.LOCAL_PORT), 0));
        param.setVersion(VersionUtil.getDoradoVersion());
        param.setRemoteIp(NetUtil.toIP(request.getRemoteAddress()));
        param.setProtocol(req.getProtocol());

        if (timeline.isEnable()) {
            param.setStartTimestamp(timeline.getTimestamp(TraceTimeline.DECODE_START_TS));
        } else {
            param.setStartTimestamp(req.getStartTimestamp());
        }
        param.setTraceTimeline(timeline);

        TransportTraceInfo transportTraceInfo = req.getTransportTraceInfo();
        if (transportTraceInfo != null) {
            param.setTraceId(transportTraceInfo.getTraceId());
            param.setSpanId(transportTraceInfo.getSpanId());
            param.setRemoteAppkey(transportTraceInfo.getClientAppkey());
            param.setDebug(transportTraceInfo.isDebug());
        }
        return param;
    }
}

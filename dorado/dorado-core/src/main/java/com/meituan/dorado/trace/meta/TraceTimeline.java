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
package com.meituan.dorado.trace.meta;

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.util.CommonUtil;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.transport.meta.Request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TraceTimeline {

    enum InvokerCostPhase {
        NodeSelect("nodeSelect"),
        FilterFirstStage("filterFirstStage"),
        FilterToCodec("filterToCodec"),
        Encode("encode"),
        EncodeBody("encodeBody"),
        WaitResponse("waitResponse"),
        Decode("decode"),
        DecodeBody("decodeBody"),

        CodecToFilter("codecToFilter"),
        FilterSecondStage("filterSecondStage"),
        AsyncGetResult("asyncGetResult");

        private String name;

        InvokerCostPhase(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    enum ProviderCostPhase {
        Decode("decode"),
        DecodeBody("decodeBody"),
        CodecToFilter("codecToFilter"),
        FilterFirstStage("filterFirstStage"),
        BizCall("bizCall"),
        FilterSecondStage("filterSecondStage"),
        Encode("encode"),
        EncodeBody("encodeBody");

        private String name;

        ProviderCostPhase(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private boolean enable;
    private ConcurrentMap<String, Long> timestampMap;
    private static final String PHASE_SEPARATOR = "| ";
    private static final String COST_UNIT = "ms";

    public TraceTimeline() {
        this.enable = false;
    }

    public TraceTimeline(boolean enable) {
        this.enable = enable;
    }

    // 调用端
    public static final String INVOKE_START_TS = "invokeStartTs";
    public static final String ASYNC_INVOKE_END_TS = "asyncInvokeEndTs";

    // 服务端
    public static final String BIZ_CALL_START_TS = "bizCallStartTs";
    public static final String BIZ_CALL_END_TS = "bizCallEndTs";

    // 过滤器节点记录
    public static final String FILTER_START_TS = "filterStartTs";
    public static final String FILTER_END_TS = "filterEndTs";
    // 服务端有BIZ_CALL不需要该记录
    public static final String FILTER_FIRST_STAGE_END_TS = "filterFirstStageEndTs";
    public static final String FILTER_SECOND_STAGE_START_TS = "filterSecondStageStartTs";


    // 编码阶段时间戳记录
    public static final String ENCODE_START_TS = "encodeStartTs";
    public static final String ENCODE_BODY_END_TS = "encodeBodyEndTs";
    public static final String ENCODE_END_TS = "encodeEndTs";

    // 解码阶段时间戳记录
    public static final String DECODE_START_TS = "decodeStartTs";
    public static final String DECODE_BODY_START_TS = "decodeBodyStartTs";
    public static final String DECODE_END_TS = "decodeEndTs";

    public static TraceTimeline newRecord(boolean enable, String timePoint) {
        TraceTimeline timeline = new TraceTimeline(enable).record(timePoint);
        return timeline;
    }

    public static TraceTimeline record(String timePoint, RpcInvocation invocation) {
        if (invocation == null) {
            return new TraceTimeline(false);
        }
        TraceTimeline timeline = CommonUtil.objectToClazzObj(invocation.getAttachment(Constants.TRACE_TIMELINE),
                TraceTimeline.class);
        if (timeline != null) {
            if (!Boolean.parseBoolean((String) invocation.getAttachment(Constants.TRACE_REPORT_FINISHED))) {
                timeline.record(timePoint);
            }
        }
        return timeline;
    }

    public static TraceTimeline record(String timePoint, Request request) {
        if (request == null) {
            return new TraceTimeline(false);
        } else {
            return record(timePoint, request.getData());
        }
    }

    public static void copyRecord(TraceTimeline tmpTimeline, RpcInvocation invocation) {
        if (invocation == null) {
            return;
        }
        TraceTimeline timeline = CommonUtil.objectToClazzObj(invocation.getAttachment(Constants.TRACE_TIMELINE),
                TraceTimeline.class);
        if (timeline != null && timeline.isEnable()) {
            for (Map.Entry<String, Long> entry : tmpTimeline.getTimestampMap().entrySet()) {
                timeline.timestampMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }

    public TraceTimeline record(String timePoint) {
        if (!isEnable()) {
            return this;
        }
        if (timestampMap == null) {
            timestampMap = new ConcurrentHashMap<>();
        }
        timestampMap.putIfAbsent(timePoint, System.currentTimeMillis());
        return this;
    }

    public static boolean isEnable(RpcInvocation invocation) {
        return CommonUtil.objectToClazzObj(invocation.getAttachment(Constants.TRACE_TIMELINE), TraceTimeline.class, true).isEnable();
    }

    public boolean isEnable() {
        return enable;
    }

    public long getTimestamp(String timePoint) {
        if (timestampMap == null || !isEnable()) {
            return -1;
        }

        Long timestamp = timestampMap.get(timePoint);
        if (timestamp == null) {
            timestamp = -1L;
        }
        return timestamp;
    }

    public long getCost(String endPoint, String startPoint) {
        long end = getTimestamp(endPoint);
        long start = getTimestamp(startPoint);
        if (end == -1 || start == -1) {
            return -1;
        }
        return end - start;
    }

    public String getInvokerPhaseCostInfo(InvokerCostPhase phase, boolean withPhaseName) {
        if (!isEnable()) {
            return "timeLineTrace is not enabled";
        }
        StringBuilder phaseCost = new StringBuilder();
        String postfix = COST_UNIT;
        if (withPhaseName) {
            phaseCost.append(phase.name).append(":");
            postfix += PHASE_SEPARATOR;
        }
        switch (phase) {
            case NodeSelect:
                phaseCost.append(getCost(FILTER_START_TS, INVOKE_START_TS)).append(postfix);
                break;
            case FilterFirstStage:
                phaseCost.append(getCost(FILTER_FIRST_STAGE_END_TS, FILTER_START_TS)).append(postfix);
                break;
            case FilterToCodec:
                phaseCost.append(getCost(ENCODE_START_TS, FILTER_FIRST_STAGE_END_TS)).append(postfix);
                break;
            case Encode:
                phaseCost.append(getCost(ENCODE_END_TS, ENCODE_START_TS)).append(postfix);
                break;
            case EncodeBody:
                phaseCost.append(getCost(ENCODE_BODY_END_TS, ENCODE_START_TS)).append(postfix);
                break;
            case WaitResponse:
                phaseCost.append(getCost(DECODE_START_TS, ENCODE_END_TS)).append(postfix);
                break;
            case Decode:
                phaseCost.append(getCost(DECODE_END_TS, DECODE_START_TS)).append(postfix);
                break;
            case DecodeBody:
                phaseCost.append(getCost(DECODE_END_TS, DECODE_BODY_START_TS)).append(postfix);
                break;
            case CodecToFilter:
                // 仅在同步有
                phaseCost.append(getCost(FILTER_SECOND_STAGE_START_TS, DECODE_END_TS)).append(postfix);
                break;
            case FilterSecondStage:
                phaseCost.append(getCost(FILTER_END_TS, FILTER_SECOND_STAGE_START_TS)).append(postfix);
                break;
            case AsyncGetResult:
                // 仅在异步有
                phaseCost.append(getCost(ASYNC_INVOKE_END_TS, DECODE_END_TS)).append(postfix);
                break;
            default:
                phaseCost = phaseCost.append("Unknown; ");
        }
        return phaseCost.toString();
    }

    public String getProviderPhaseCostInfo(ProviderCostPhase phase, boolean withPhaseName) {
        if (!isEnable()) {
            return "timeLineTrace is not enabled";
        }
        StringBuilder phaseCost = new StringBuilder();
        String postfix = COST_UNIT;
        if (withPhaseName) {
            phaseCost.append(phase.name).append(":");
            postfix += PHASE_SEPARATOR;
        }
        switch (phase) {
            case Decode:
                phaseCost.append(getCost(DECODE_END_TS, DECODE_START_TS)).append(postfix);
                break;
            case DecodeBody:
                phaseCost.append(getCost(DECODE_END_TS, DECODE_BODY_START_TS)).append(postfix);
                break;
            case CodecToFilter:
                phaseCost.append(getCost(FILTER_START_TS, DECODE_END_TS)).append(postfix);
                break;
            case FilterFirstStage:
                phaseCost.append(getCost(BIZ_CALL_START_TS, FILTER_START_TS)).append(postfix);
                break;
            case BizCall:
                phaseCost.append(getCost(BIZ_CALL_END_TS, BIZ_CALL_START_TS)).append(postfix);
                break;
            case FilterSecondStage:
                phaseCost.append(getCost(ENCODE_START_TS, BIZ_CALL_END_TS)).append(postfix);
                break;
            case Encode:
                phaseCost.append(getCost(ENCODE_END_TS, ENCODE_START_TS)).append(postfix);
                break;
            case EncodeBody:
                phaseCost.append(getCost(ENCODE_BODY_END_TS, ENCODE_START_TS)).append(postfix);
                break;
            default:
                phaseCost = phaseCost.append("Unknown; ");
        }
        return phaseCost.toString();
    }

    public String genInvokerAllPhaseCost() {
        if (!isEnable()) {
            return "timeLineTrace is not enabled";
        }
        StringBuilder phaseCost = new StringBuilder();
        phaseCost.append(getInvokerPhaseCostInfo(InvokerCostPhase.NodeSelect, true))
                .append(getInvokerPhaseCostInfo(InvokerCostPhase.FilterFirstStage, true))
                .append(getInvokerPhaseCostInfo(InvokerCostPhase.FilterToCodec, true))
                .append(getInvokerPhaseCostInfo(InvokerCostPhase.EncodeBody, true))
                .append(getInvokerPhaseCostInfo(InvokerCostPhase.Encode, true))
                .append(getInvokerPhaseCostInfo(InvokerCostPhase.WaitResponse, true))
                .append(getInvokerPhaseCostInfo(InvokerCostPhase.DecodeBody, true))
                .append(getInvokerPhaseCostInfo(InvokerCostPhase.Decode, true));

        if (getTimestamp(ASYNC_INVOKE_END_TS) > 0) {
            phaseCost.append(getInvokerPhaseCostInfo(InvokerCostPhase.FilterSecondStage, true));
            phaseCost.append(getInvokerPhaseCostInfo(InvokerCostPhase.AsyncGetResult, true));
        } else {
            phaseCost.append(getInvokerPhaseCostInfo(InvokerCostPhase.CodecToFilter, true));
            phaseCost.append(getInvokerPhaseCostInfo(InvokerCostPhase.FilterSecondStage, true));
        }
        return phaseCost.toString();
    }

    public Map<String, String> genInvokerPhaseCostPair() {
        if (!isEnable()) {
            return Collections.emptyMap();
        }
        Map<String, String> phaseCostPair = new LinkedHashMap<>();
        phaseCostPair.put(InvokerCostPhase.NodeSelect.name, getInvokerPhaseCostInfo(InvokerCostPhase.NodeSelect, false));
        phaseCostPair.put(InvokerCostPhase.FilterFirstStage.name, getInvokerPhaseCostInfo(InvokerCostPhase.FilterFirstStage, false));
        phaseCostPair.put(InvokerCostPhase.FilterToCodec.name, getInvokerPhaseCostInfo(InvokerCostPhase.FilterToCodec, false));
        phaseCostPair.put(InvokerCostPhase.EncodeBody.name, getInvokerPhaseCostInfo(InvokerCostPhase.EncodeBody, false));
        phaseCostPair.put(InvokerCostPhase.Encode.name, getInvokerPhaseCostInfo(InvokerCostPhase.Encode, false));
        phaseCostPair.put(InvokerCostPhase.WaitResponse.name, getInvokerPhaseCostInfo(InvokerCostPhase.WaitResponse, false));
        phaseCostPair.put(InvokerCostPhase.DecodeBody.name, getInvokerPhaseCostInfo(InvokerCostPhase.DecodeBody, false));
        phaseCostPair.put(InvokerCostPhase.Decode.name, getInvokerPhaseCostInfo(InvokerCostPhase.Decode, false));

        if (getTimestamp(ASYNC_INVOKE_END_TS) > 0) {
            phaseCostPair.put(InvokerCostPhase.FilterSecondStage.name, getInvokerPhaseCostInfo(InvokerCostPhase.FilterSecondStage, false));
            phaseCostPair.put(InvokerCostPhase.AsyncGetResult.name, getInvokerPhaseCostInfo(InvokerCostPhase.AsyncGetResult, false));
        } else {
            phaseCostPair.put(InvokerCostPhase.CodecToFilter.name, getInvokerPhaseCostInfo(InvokerCostPhase.CodecToFilter, false));
            phaseCostPair.put(InvokerCostPhase.FilterSecondStage.name, getInvokerPhaseCostInfo(InvokerCostPhase.FilterSecondStage, false));
        }
        return phaseCostPair;
    }

    public String genProviderAllPhaseCost() {
        if (!isEnable()) {
            return "timeLineTrace is not enabled";
        }
        StringBuilder phaseCost = new StringBuilder();
        phaseCost.append(getProviderPhaseCostInfo(ProviderCostPhase.Decode, true))
                .append(getProviderPhaseCostInfo(ProviderCostPhase.DecodeBody, true))
                .append(getProviderPhaseCostInfo(ProviderCostPhase.CodecToFilter, true))
                .append(getProviderPhaseCostInfo(ProviderCostPhase.FilterFirstStage, true))
                .append(getProviderPhaseCostInfo(ProviderCostPhase.BizCall, true))
                .append(getProviderPhaseCostInfo(ProviderCostPhase.FilterSecondStage, true))
                .append(getProviderPhaseCostInfo(ProviderCostPhase.Encode, true))
                .append(getProviderPhaseCostInfo(ProviderCostPhase.EncodeBody, true));

        return phaseCost.toString();
    }

    public Map<String, String> genProviderPhaseCostPair() {
        if (!isEnable()) {
            return Collections.emptyMap();
        }
        Map<String, String> phaseCostPair = new LinkedHashMap<>();
        phaseCostPair.put(ProviderCostPhase.Decode.name, getProviderPhaseCostInfo(ProviderCostPhase.Decode, false));
        phaseCostPair.put(ProviderCostPhase.DecodeBody.name, getProviderPhaseCostInfo(ProviderCostPhase.DecodeBody, false));
        phaseCostPair.put(ProviderCostPhase.CodecToFilter.name, getProviderPhaseCostInfo(ProviderCostPhase.CodecToFilter, false));
        phaseCostPair.put(ProviderCostPhase.FilterFirstStage.name, getProviderPhaseCostInfo(ProviderCostPhase.FilterFirstStage, false));
        phaseCostPair.put(ProviderCostPhase.BizCall.name, getProviderPhaseCostInfo(ProviderCostPhase.BizCall, false));
        phaseCostPair.put(ProviderCostPhase.FilterSecondStage.name, getProviderPhaseCostInfo(ProviderCostPhase.FilterSecondStage, false));
        phaseCostPair.put(ProviderCostPhase.Encode.name, getProviderPhaseCostInfo(ProviderCostPhase.Encode, false));
        phaseCostPair.put(ProviderCostPhase.EncodeBody.name, getProviderPhaseCostInfo(ProviderCostPhase.EncodeBody, false));
        return phaseCostPair;
    }

    public ConcurrentMap<String, Long> getTimestampMap() {
        if (timestampMap == null) {
            return new ConcurrentHashMap();
        }
        return timestampMap;
    }
}

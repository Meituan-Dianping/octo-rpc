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

package com.meituan.dorado.trace.cat;

import com.dianping.cat.Cat;
import com.dianping.cat.message.ForkableTransaction;
import com.dianping.cat.message.ForkedTransaction;
import com.dianping.cat.message.Transaction;
import com.meituan.dorado.common.util.SizeUtil;
import com.meituan.dorado.rpc.AsyncContext;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.trace.AbstractInvokeTrace;
import com.meituan.dorado.trace.meta.TraceParam;
import com.meituan.dorado.trace.meta.TraceTimeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatInvokeTrace extends AbstractInvokeTrace {

    private static final Logger logger = LoggerFactory.getLogger(CatInvokeTrace.class);

    private static final String TRANSACTION = "transaction";
    private static final String ASYNC_FORKABLE_TRANSACTION = "ForkableTransaction";
    private static final String DORADO_CALL = "OctoCall";
    private static final String DORADO_SERVICE = "OctoService";
    private static final String TRACE_ID = "traceId";

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void init(String appkey) {
        Cat.initializeByDomain(appkey);
    }

    @Override
    public void clientSend(TraceParam traceParam, RpcInvocation invocation) {
        if (AsyncContext.isAsyncReq(invocation)) {
            // 获取当前的transaction，如果为空，则不需要将子线程的消息树嵌入主线程
            Transaction parentTransaction = Cat.getManager().getPeekTransaction();
            ForkableTransaction forkableTransaction = null;
            if (parentTransaction != null) {
                forkableTransaction = parentTransaction.forFork();
            }
            traceParam.putAttachment(ASYNC_FORKABLE_TRANSACTION, forkableTransaction);
        } else {
            Transaction transaction = Cat.newTransaction(DORADO_CALL, traceParam.getSpanName());
            traceParam.putAttachment(TRANSACTION, transaction);
        }
    }

    @Override
    public void serverRecv(TraceParam traceParam, RpcInvocation invocation) {
        if (TraceTimeline.isEnable(invocation)) {
            // do nothing, 在serveSend中通过newTransactionWithDuration上报耗时
        } else {
            Transaction transaction = Cat.newTransaction(DORADO_SERVICE, traceParam.getSpanName());
            traceParam.putAttachment(TRANSACTION, transaction);
        }
    }

    @Override
    protected void serverSendInFilter(TraceParam traceParam, RpcInvocation invocation) {
        Transaction transaction = (Transaction) traceParam.getAttachment(TRANSACTION);
        if (transaction == null) {
            logger.warn("ServerSide: Request {} won't do cat report, cause no start transaction info.", traceParam.getSpanName());
            return;
        }
        try {
            serverLogEvent(traceParam, invocation);
            transaction.addData(TRACE_ID, traceParam.getTraceId());

            if (traceParam.getThrowable() == null) {
                transaction.setStatus(Transaction.SUCCESS);
            } else {
                Cat.logErrorWithCategory(DORADO_SERVICE + "." + traceParam.getRemoteAppkey(), traceParam.getThrowable());
                transaction.setStatus(traceParam.getThrowable());
            }
        } catch (Exception e) {
            Cat.logErrorWithCategory(DORADO_SERVICE + "." + traceParam.getRemoteAppkey(), e);
            transaction.setStatus(e);
        } finally {
            transaction.complete();
        }
    }

    @Override
    protected void serverSendInCodec(TraceParam traceParam, RpcInvocation invocation) {
        TraceTimeline timeline = traceParam.getTraceTimeline();
        Transaction transaction = Cat.newTransactionWithDuration(DORADO_SERVICE, traceParam.getSpanName(),
                System.currentTimeMillis() - traceParam.getStartTimestamp());
        try {
            serverLogEvent(traceParam, invocation);
            transaction.addData(TRACE_ID, traceParam.getTraceId());
            transaction.addData(CatEventType.SERVICE_PHASECOST.type, timeline.genProviderAllPhaseCost());

            if (traceParam.getThrowable() != null) {
                Cat.logErrorWithCategory(DORADO_SERVICE + "." + traceParam.getRemoteAppkey(), traceParam.getThrowable());
                transaction.setStatus(traceParam.getThrowable());
            } else {
                transaction.setStatus(Transaction.SUCCESS);
            }
        } catch (Exception e) {
            Cat.logErrorWithCategory(DORADO_SERVICE + "." + traceParam.getRemoteAppkey(), e);
            transaction.setStatus(e);
        } finally {
            transaction.complete();
        }
    }

    @Override
    protected void syncClientRecv(TraceParam traceParam, RpcInvocation invocation) {
        Transaction transaction = (Transaction) traceParam.getAttachment(TRANSACTION);
        if (transaction == null) {
            logger.warn("ClientSide: Request {} won't do cat report, cause no start transaction info.", traceParam.getSpanName());
            return;
        }
        try {
            clientLogEvent(traceParam, invocation);
            if (traceParam.getThrowable() != null) {
                Cat.logErrorWithCategory(DORADO_CALL + "." + traceParam.getRemoteAppkey(), traceParam.getThrowable());
                transaction.setStatus(traceParam.getThrowable());
            } else {
                transaction.setStatus(Transaction.SUCCESS);
            }

            transaction.addData(TRACE_ID, traceParam.getTraceId());
            TraceTimeline timeline = traceParam.getTraceTimeline();
            if (timeline.isEnable()) {
                transaction.addData(CatEventType.INVOKER_PHASECOST.type, timeline.genInvokerAllPhaseCost());
            }
        } catch (Exception e) {
            Cat.logErrorWithCategory(DORADO_CALL + "." + traceParam.getRemoteAppkey(), e);
            transaction.setStatus(e);
        } finally {
            transaction.complete();
        }
    }

    @Override
    protected void asyncClientRecv(TraceParam traceParam, RpcInvocation invocation) {
        ForkableTransaction forkableTransaction = (ForkableTransaction) traceParam.getAttachment(ASYNC_FORKABLE_TRANSACTION);

        ForkedTransaction forkedTransaction = null;
        Transaction transaction = null;
        try {
            if (forkableTransaction != null) {
                forkedTransaction = forkableTransaction.doFork();
            }
            transaction = Cat.newTransactionWithDuration(DORADO_CALL, traceParam.getSpanName(),
                    System.currentTimeMillis() - traceParam.getStartTimestamp());
            clientLogEvent(traceParam, invocation);
            if (traceParam.getThrowable() != null) {
                Cat.logErrorWithCategory(DORADO_CALL + "." + traceParam.getRemoteAppkey(), traceParam.getThrowable());
                transaction.setStatus(traceParam.getThrowable());
            } else {
                transaction.setStatus(Transaction.SUCCESS);
            }

            transaction.addData(TRACE_ID, traceParam.getTraceId());
            TraceTimeline timeline = traceParam.getTraceTimeline();
            if (timeline.isEnable()) {
                transaction.addData(CatEventType.INVOKER_PHASECOST.type, timeline.genInvokerAllPhaseCost());
            }
        } catch (Exception e) {
            if (transaction != null) {
                Cat.logErrorWithCategory(DORADO_CALL + "." + traceParam.getRemoteAppkey(), e);
                transaction.setStatus(e);
            }
        } finally {
            if (transaction != null) {
                transaction.complete();
            }
            if (forkedTransaction != null) {
                forkedTransaction.close();
            }
        }
    }

    public enum CatEventType {
        SERVICE_CLIENT_APPKEY("OctoService.appkey"),
        SERVICE_CLIENT_IP("OctoService.clientIp"),
        SERVICE_PROTOCOL_TYPE("OctoService.protocolType"),
        SERVICE_REQUEST_SIZE("OctoService.requestSize"),
        SERVICE_RESPONSE_SIZE("OctoService.responseSize"),
        SERVICE_PHASECOST("OctoService.phaseCost"),

        INVOKER_SERVER_IP("OctoCall.serverIp"),
        INVOKER_TIMEOUT("OctoCall.timeout"),
        INVOKER_CALL_TYPE("OctoCall.callType"),
        INVOKER_PROTOCOL_TYPE("OctoCall.protocolType"),
        INVOKER_SERVER_APPKEY("OctoCall.appkey"),
        INVOKER_REQUEST_SIZE("OctoCall.requestSize"),
        INVOKER_RESPONSE_SIZE("OctoCall.responseSize"),
        INVOKER_PHASECOST("OctoCall.phaseCost");

        private String type;

        CatEventType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    private void clientLogEvent(TraceParam traceParam, RpcInvocation invocation) {
        Cat.logEvent(CatEventType.INVOKER_SERVER_APPKEY.getType(), traceParam.getRemoteAppkey());
        Cat.logEvent(CatEventType.INVOKER_SERVER_IP.getType(), traceParam.getRemoteIp());
        Cat.logEvent(CatEventType.INVOKER_TIMEOUT.getType(), String.valueOf(traceParam.getTimeout()));
        Cat.logEvent(CatEventType.INVOKER_CALL_TYPE.getType(), AsyncContext.isAsyncReq(invocation) ? "Async" : "Sync");
        Cat.logEvent(CatEventType.INVOKER_PROTOCOL_TYPE.getType(), traceParam.getProtocol());
        Cat.logEvent(CatEventType.INVOKER_REQUEST_SIZE.getType(), SizeUtil.getLogSize(traceParam.getRequestSize()));
        Cat.logEvent(CatEventType.INVOKER_RESPONSE_SIZE.getType(), SizeUtil.getLogSize(traceParam.getResponseSize()));
    }

    private void serverLogEvent(TraceParam traceParam, RpcInvocation invocation) {
        Cat.logEvent(CatEventType.SERVICE_CLIENT_APPKEY.getType(), traceParam.getRemoteAppkey());
        Cat.logEvent(CatEventType.SERVICE_CLIENT_IP.getType(), traceParam.getRemoteIp());
        Cat.logEvent(CatEventType.SERVICE_PROTOCOL_TYPE.getType(), traceParam.getProtocol());
        Cat.logEvent(CatEventType.SERVICE_REQUEST_SIZE.getType(), SizeUtil.getLogSize(traceParam.getRequestSize()));
        Cat.logEvent(CatEventType.SERVICE_RESPONSE_SIZE.getType(), SizeUtil.getLogSize(traceParam.getResponseSize()));
    }
}

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
package com.meituan.dorado.codec.octo;

import com.meituan.dorado.bootstrap.provider.ProviderInfoRepository;
import com.meituan.dorado.bootstrap.provider.ServicePublisher;
import com.meituan.dorado.codec.octo.meta.*;
import com.meituan.dorado.codec.octo.meta.old.RequestHeader;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.*;
import com.meituan.dorado.trace.AbstractInvokeTrace;
import com.meituan.dorado.trace.meta.TraceTimeline;
import com.meituan.dorado.trace.meta.TransportTraceInfo;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.DefaultResponse;
import org.apache.commons.lang3.StringUtils;

import static com.meituan.dorado.codec.octo.meta.StatusCode.RpcException;
import static com.meituan.dorado.codec.octo.meta.StatusCode.Success;

public class MetaUtil {

    public static Header convertRequestToHeader(DefaultRequest request) {
        Header header = new Header();

        if (request.getMessageType() == Constants.MESSAGE_TYPE_SERVICE) {
            header.setMessageType((byte) MessageType.Normal.getValue());
        } else if (request.getMessageType() == Constants.MESSAGE_TYPE_HEART) {
            header.setMessageType((byte) MessageType.NormalHeartbeat.getValue());
        } else {
            throw new ProtocolException("Serialize unknown messageType.");
        }

        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setSequenceId(request.getSeq());
        requestInfo.setTimeout(request.getTimeout());
        requestInfo.setServiceName(request.getServiceName());
        requestInfo.setCallType((byte) request.getCallType());

        header.setRequestInfo(requestInfo);

        TraceInfo traceInfo = new TraceInfo();
        traceInfo.setClientAppkey(request.getAppkey());
        traceInfo.setClientIp(request.getClientIp());
        TransportTraceInfo transportTraceInfo = request.getTransportTraceInfo();
        if (transportTraceInfo != null) {
            traceInfo.setTraceId(transportTraceInfo.getTraceId());
            traceInfo.setSpanId(transportTraceInfo.getSpanId());
            traceInfo.setDebug(transportTraceInfo.isDebug());
        }
        header.setTraceInfo(traceInfo);

        header.setGlobalContext(request.getGlobalContexts());
        header.setLocalContext(request.getLocalContexts());
        return header;
    }

    public static DefaultRequest convertHeaderToRequest(Header header) {

        RequestInfo requestInfo = header.getRequestInfo();
        if (requestInfo == null) {
            throw new ProtocolException("Deserialize request: lack RequestInfo.");
        }
        DefaultRequest request = new DefaultRequest(requestInfo.getSequenceId());
        request.setOctoProtocol(true);
        request.setTimeout(requestInfo.getTimeout());
        request.setCallType(requestInfo.getCallType() & 0xff);
        request.setServiceName(requestInfo.getServiceName());
        request.setMessageType(header.getMessageType() & 0xff);

        if (header.getTraceInfo() != null) {
            TraceInfo traceInfo = header.getTraceInfo();
            request.setRemoteAppkey(traceInfo.clientAppkey);
            request.setClientIp(traceInfo.clientIp);
            TransportTraceInfo transportTraceInfo = new TransportTraceInfo(traceInfo.getClientAppkey());
            transportTraceInfo.setSpanId(traceInfo.getSpanId());
            transportTraceInfo.setTraceId(traceInfo.getTraceId());
            transportTraceInfo.setDebug(traceInfo.isDebug());
            request.setTransportTraceInfo(transportTraceInfo);
        }

        request.setGlobalContext(header.getGlobalContext());
        request.putLocalContexts(header.getLocalContext());

        return request;
    }

    public static DefaultResponse convertHeaderToResponse(Header header) {
        ResponseInfo responseInfo = header.getResponseInfo();
        if (responseInfo == null) {
            throw new ProtocolException("Deserialize request: lack ResponseInfo.");
        }
        DefaultResponse response = new DefaultResponse(responseInfo.getSequenceId());
        response.setOctoProtocol(true);
        response.setStatusCode(responseInfo.getStatus());

        if (header.getMessageType() == MessageType.NormalHeartbeat.getValue()) {
            response.setMessageType(Constants.MESSAGE_TYPE_HEART);
        } else if (header.getMessageType() == MessageType.Normal.getValue()) {
            response.setMessageType(Constants.MESSAGE_TYPE_SERVICE);
        } else {
            throw new ProtocolException("Deserialize unknown messageType.");
        }

        response.setGlobalContexts(header.getGlobalContext());
        response.putLocalContexts(header.getLocalContext());
        return response;
    }

    public static Header convertResponseToHeader(DefaultResponse response) {
        Header header = new Header();

        int messageType = response.getMessageType();

        if (messageType == MessageType.Normal.getValue()) {
            header.setMessageType((byte) MessageType.Normal.getValue());
        } else if (messageType == MessageType.NormalHeartbeat.getValue()) {
            header.setMessageType((byte) MessageType.NormalHeartbeat.getValue());
        } else if (messageType == MessageType.ScannerHeartbeat.getValue()) {
            header.setMessageType((byte) MessageType.ScannerHeartbeat.getValue());
            HeartbeatInfo heartbeatInfo = genScannerHeartbeatInfo(response);
            header.setHeartbeatInfo(heartbeatInfo);
        } else {
            throw new ProtocolException("Deserialize unknown messageType.");
        }

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.setSequenceId(response.getSeq());
        responseInfo.setStatus((byte) StatusCode.Success.getValue());

        Throwable exception = response.getException();
        if (exception != null) {
            if (exception instanceof ProtocolException) {
                responseInfo.setStatus((byte) StatusCode.ProtocolException.getValue());
            } else if (exception instanceof DegradeException) {
                responseInfo.setStatus((byte) StatusCode.DegradeException.getValue());
            } else if (exception instanceof SecurityException) {
                responseInfo.setStatus((byte) StatusCode.SecurityException.getValue());
            } else if (exception instanceof TransportException) {
                responseInfo.setStatus((byte) StatusCode.TransportException.getValue());
            } else if (exception instanceof ServiceException) {
                responseInfo.setStatus((byte) StatusCode.ServiceException.getValue());
            } else if (exception instanceof ApplicationException) {
                responseInfo.setStatus((byte) StatusCode.ApplicationException.getValue());
            } else {
                responseInfo.setStatus((byte) RpcException.getValue());
            }
            responseInfo.setMessage(exception.getMessage());
        }
        header.setResponseInfo(responseInfo);

        header.setGlobalContext(response.getGlobalContexts());
        header.setLocalContext(response.getLocalContexts());
        return header;
    }

    public static RequestHeader convertRequestToOldProtocolHeader(DefaultRequest request) {
        RequestHeader requestHeader = new RequestHeader();
        requestHeader.setClientAppkey(request.getAppkey());
        requestHeader.setClientIp(request.getClientIp());
        TransportTraceInfo transportTraceInfo = request.getTransportTraceInfo();
        if (transportTraceInfo != null) {
            requestHeader.setTraceId(transportTraceInfo.getTraceId());
            requestHeader.setSpanId(transportTraceInfo.getSpanId());
            requestHeader.setDebug(transportTraceInfo.isDebug());
        }
        requestHeader.setGlobalContext(request.getGlobalContexts());
        requestHeader.setLocalContext(request.getLocalContexts());
        return requestHeader;
    }

    public static DefaultRequest convertOldProtocolHeaderToRequest(RequestHeader header, DefaultRequest request) {
        request.setRemoteAppkey(header.getClientAppkey());
        request.setClientIp(header.getClientIp());
        TransportTraceInfo transportTraceInfo = new TransportTraceInfo(header.getClientAppkey());
        transportTraceInfo.setSpanId(header.getSpanId());
        transportTraceInfo.setTraceId(header.getTraceId());
        transportTraceInfo.setDebug(header.isDebug());
        request.setTransportTraceInfo(transportTraceInfo);
        return request;
    }

    private static HeartbeatInfo genScannerHeartbeatInfo(DefaultResponse response) {
        HeartbeatInfo heartbeatInfo = new HeartbeatInfo();
        heartbeatInfo.setAppkey(ServicePublisher.getAppkey());
        heartbeatInfo.setSendTime(System.currentTimeMillis());
        heartbeatInfo.setStatus(ProviderInfoRepository.getProviderStatus(response.getPort()).getCode());

        return heartbeatInfo;
    }

    public static void recordTraceInfoInDecode(byte[] msgBuff, Object message) {
        if (message == null) {
            return;
        }
        if (message instanceof DefaultRequest) {
            DefaultRequest request = (DefaultRequest) message;
            if (request.getMessageType() != MessageType.Normal.getValue()) {
                return;
            }
            request.putAttachment(Constants.REQUEST_SIZE, msgBuff.length);
            TraceTimeline.record(TraceTimeline.DECODE_END_TS, request);
        } else if (message instanceof DefaultResponse) {
            DefaultResponse response = (DefaultResponse) message;
            if (response.getMessageType() != MessageType.Normal.getValue()) {
                return;
            }
            response.putAttachment(Constants.RESPONSE_SIZE, msgBuff.length);
            TraceTimeline.record(TraceTimeline.DECODE_END_TS, response.getRequest());
        }
    }

    public static void recordTraceInfoInEncode(byte[] msgBuff, Object message) {
        if (message == null) {
            return;
        }
        if (message instanceof DefaultRequest) {
            DefaultRequest request = (DefaultRequest) message;
            if (request.getMessageType() != MessageType.Normal.getValue()) {
                return;
            }
            request.putAttachment(Constants.REQUEST_SIZE, msgBuff.length);
            TraceTimeline.record(TraceTimeline.ENCODE_END_TS, request);
        } else if (message instanceof DefaultResponse) {
            DefaultResponse response = (DefaultResponse) message;
            if (response.getMessageType() != MessageType.Normal.getValue()) {
                return;
            }
            response.putAttachment(Constants.RESPONSE_SIZE, msgBuff.length);
            if (response.getRequest() == null) {
                return;
            }
            TraceTimeline.record(TraceTimeline.ENCODE_END_TS, response.getRequest());
            AbstractInvokeTrace.reportServerTraceInfoIfNeeded(response.getRequest(), response);
        }
    }

    public static void wrapException(Exception exception, DefaultResponse response) {
        if (exception == null) {
            return;
        }
        String expMessage = exception.getMessage();
        if (StringUtils.isBlank(expMessage)) {
            expMessage = expMessage.getClass().getName();
        }
        if (StatusCode.findByValue(response.getStatusCode() & 0xff) == Success) {
            if (expMessage.contains(ProtocolException.class.getName())) {
                response.setStatusCode((byte) StatusCode.ProtocolException.getValue());
            } else if (expMessage.contains(DegradeException.class.getName())) {
                response.setStatusCode((byte) StatusCode.DegradeException.getValue());
            } else if (expMessage.contains(SecurityException.class.getName())) {
                response.setStatusCode((byte) StatusCode.SecurityException.getValue());
            } else if (expMessage.contains(TransportException.class.getName())) {
                response.setStatusCode((byte) StatusCode.TransportException.getValue());
            } else if (expMessage.contains(ServiceException.class.getName())) {
                response.setStatusCode((byte) StatusCode.ServiceException.getValue());
            } else if (expMessage.contains(ApplicationException.class.getName())) {
                response.setStatusCode((byte) StatusCode.ApplicationException.getValue());
            } else {
                response.setStatusCode((byte) RpcException.getValue());
            }
        }
        switch (StatusCode.findByValue(response.getStatusCode() & 0xff)) {
            case ApplicationException:
                response.setException(new ApplicationException("Probably be business exception, cause by: " + expMessage));
                break;
            case RpcException:
                response.setException(new RpcException(expMessage));
                break;
            case RuntimeException:
                response.setException(new ApplicationException(expMessage));
                break;
            case TransportException:
                response.setException(new TransportException(expMessage));
                break;
            case ProtocolException:
                response.setException(new ProtocolException(expMessage));
                break;
            case DegradeException:
                response.setException(new DegradeException(expMessage));
                break;
            case SecurityException:
                response.setException(new SecurityException(expMessage));
                break;
            case RemoteException:
                response.setException(new RemoteException("Remote exception: " + expMessage));
                break;
            default:
                response.setException(new RpcException(expMessage));
        }
    }
}

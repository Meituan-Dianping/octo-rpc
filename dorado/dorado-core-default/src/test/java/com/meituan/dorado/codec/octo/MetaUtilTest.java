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


import com.meituan.dorado.codec.octo.meta.Header;
import com.meituan.dorado.codec.octo.meta.MessageType;
import com.meituan.dorado.codec.octo.meta.ResponseInfo;
import com.meituan.dorado.codec.octo.meta.old.RequestHeader;
import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.test.thrift.api.Echo;
import com.meituan.dorado.trace.meta.TransportTraceInfo;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.DefaultResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetaUtilTest {

    private DefaultResponse response;
    private DefaultRequest request;
    private Header header;
    private ResponseInfo responseInfo;
    private TransportTraceInfo traceInfo;
    private RequestHeader requestHeader;

    @Before
    public void setUp() throws Exception {
        response = mock(DefaultResponse.class);
        header = mock(Header.class);
        responseInfo = mock(ResponseInfo.class);
        request = mock(DefaultRequest.class);
        traceInfo = mock(TransportTraceInfo.class);
        requestHeader = mock(RequestHeader.class);

        when(responseInfo.getSequenceId()).thenReturn(1L);
        when(responseInfo.getStatus()).thenReturn((byte)1);

        when(response.getSeq()).thenReturn(1L);

        when(traceInfo.getTraceId()).thenReturn("123");
        when(traceInfo.getSpanId()).thenReturn("0.1");
        when(traceInfo.isDebug()).thenReturn(false);

        when(request.getAppkey()).thenReturn("com.sankuai.mock");
        when(request.getClientIp()).thenReturn("127.0.0.1");
        when(request.getTransportTraceInfo()).thenReturn(traceInfo);

        when(requestHeader.getClientAppkey()).thenReturn("com.sankuai.mock2");
        when(requestHeader.getClientIp()).thenReturn("127.0.0.2");
        when(requestHeader.getSpanId()).thenReturn("0.1");
    }

    @Test
    public void testResponseToHeader() {
        when(response.getMessageType()).thenReturn(MessageType.Normal.getValue());
        Header header = MetaUtil.convertResponseToHeader(response);
        Assert.assertEquals(header.getMessageType(), MessageType.Normal.getValue());

        when(response.getMessageType()).thenReturn(3);
        try {
            MetaUtil.convertResponseToHeader(response);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }

        when(response.getMessageType()).thenReturn(MessageType.ScannerHeartbeat.getValue());
        header = MetaUtil.convertResponseToHeader(response);
        Assert.assertEquals(header.getMessageType(), MessageType.ScannerHeartbeat.getValue());

        when(response.getMessageType()).thenReturn(MessageType.NormalHeartbeat.getValue());
        header = MetaUtil.convertResponseToHeader(response);
        Assert.assertEquals(header.getMessageType(), MessageType.NormalHeartbeat.getValue());

        when(response.getException()).thenReturn(new ProtocolException("Mock ProtocolException."));
        header = MetaUtil.convertResponseToHeader(response);
        Assert.assertEquals(header.getResponseInfo().sequenceId, 1L);
        Assert.assertEquals(header.getResponseInfo().getMessage(), "Mock ProtocolException.");
    }

    @Test
    public void testHeaderToResponse() {
        try {
            MetaUtil.convertHeaderToResponse(header);
        } catch(Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }

        when(header.getResponseInfo()).thenReturn(responseInfo);
        when(header.getMessageType()).thenReturn((byte) MessageType.Normal.getValue());
        when(header.getMessageType()).thenReturn((byte) 3);
        try {
            MetaUtil.convertHeaderToResponse(header);
        } catch(Exception e) {
            Assert.assertTrue(e instanceof TimeoutException);
        }
    }

    @Test
    public void testRequestToOldProtocolHeader() {
        RequestHeader header = MetaUtil.convertRequestToOldProtocolHeader(request);
        Assert.assertEquals(header.getClientAppkey(), "com.sankuai.mock");
        Assert.assertEquals(header.getTraceId(), "123");
    }

    @Test
    public void testOldProtocolHeaderToRequest() {
        DefaultRequest request = new DefaultRequest();
        request.setServiceInterface(Echo.Iface.class);
        MetaUtil.convertOldProtocolHeaderToRequest(requestHeader, request);
        Assert.assertEquals(request.getRemoteAppkey(), "com.sankuai.mock2");
        Assert.assertEquals(request.getClientIp(), "127.0.0.2");
        Assert.assertEquals(request.getTransportTraceInfo().getSpanId(), "0.1");
        Assert.assertEquals(request.getServiceName(), null);
    }
}

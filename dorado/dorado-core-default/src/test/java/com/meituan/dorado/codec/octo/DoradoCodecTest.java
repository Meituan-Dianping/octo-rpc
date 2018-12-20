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


import com.meituan.dorado.Echo;
import com.meituan.dorado.HelloService;
import com.meituan.dorado.bootstrap.invoker.ServiceInvocationRepository;
import com.meituan.dorado.bootstrap.provider.ServicePublisher;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.common.exception.RpcException;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.common.exception.TransportException;
import com.meituan.dorado.rpc.DefaultFuture;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.serialize.thrift.ThriftMessageSerializer;
import com.meituan.dorado.trace.meta.TransportTraceInfo;
import com.meituan.dorado.transport.Channel;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.DefaultResponse;
import com.meituan.dorado.util.CompressUtil;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(OctoCodec.class)
public class DoradoCodecTest {

    private DefaultRequest request;
    private RpcInvocation invocation;
    private DefaultFuture future;

    private DefaultResponse response;
    private TransportTraceInfo traceInfo;

    private Channel channel;
    private DoradoCodec codec;

    @Before
    public void setUp() throws Exception {
        codec = new DoradoCodec();

        request = mock(DefaultRequest.class);
        response = mock(DefaultResponse.class);
        invocation = mock(RpcInvocation.class);
        channel = mock(Channel.class);
        future = mock(DefaultFuture.class);
        traceInfo = mock(TransportTraceInfo.class);

        when(traceInfo.getTraceId()).thenReturn("123");
        when(traceInfo.getSpanId()).thenReturn("0.1");
        when(traceInfo.isDebug()).thenReturn(false);

        when(channel.getLocalAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9001));
        when(channel.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9001));

        when(invocation.getMethod()).thenReturn(Echo.Iface.class.getMethod("echo", String.class));
        when(invocation.getServiceInterface()).thenAnswer(new Answer<Class<?>>() {
            @Override
            public Class<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Echo.Iface.class;
            }
        });
        when(invocation.getArguments()).thenReturn(new Object[]{"hello"});

        when(request.getMessageType()).thenReturn(Constants.MESSAGE_TYPE_SERVICE);
        when(request.getSerialize()).thenReturn((byte) 1);
        when(request.getServiceInterface()).thenAnswer(new Answer<Class<?>>() {
            @Override
            public Class<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Echo.Iface.class;
            }
        });
        when(request.getSeq()).thenReturn(1L);
        when(request.getTimeout()).thenReturn(100);
        when(request.getServiceName()).thenReturn("Echo.Iface");
        when(request.getCallType()).thenReturn(1);
        when(request.getAppkey()).thenReturn("com.meituan.octo.dorado.client");
        when(request.getClientIp()).thenReturn("127.0.0.1");
        when(request.getDoChecksum()).thenReturn(true);
        when(request.getCompressType()).thenReturn(CompressUtil.CompressType.SNAPPY);
        when(request.getVersion()).thenReturn((byte) 1);
        when(request.getData()).thenReturn(invocation);
        when(request.getTransportTraceInfo()).thenReturn(traceInfo);

        when(response.getMessageType()).thenReturn(Constants.MESSAGE_TYPE_SERVICE);
        when(response.getSerialize()).thenReturn((byte) 1);
        when(response.getServiceInterface()).thenAnswer(new Answer<Class<?>>() {
            @Override
            public Class<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Echo.Iface.class;
            }
        });
        when(response.getThriftMsgInfo()).thenReturn(new ThriftMessageSerializer.ThriftMessageInfo(
                "echo", 1));
        when(response.getDoChecksum()).thenReturn(false);
        when(response.getCompressType()).thenReturn(CompressUtil.CompressType.SNAPPY);
        when(response.getVersion()).thenReturn((byte) 1);
        when(response.getResult()).thenReturn(new RpcResult("echo"));
        when(response.getSeq()).thenReturn(1L);

        ServicePublisher.getServiceInterfaceMap().put("Echo.Iface", Echo.Iface.class);
        ServiceInvocationRepository.putRequestAndFuture(request, future);
    }

    @Test
    public void testOctoRequest() throws Exception {
        when(request.isOctoProtocol()).thenReturn(true);
        try {
            byte[] content = codec.encode(channel, request, Collections.<String, Object>emptyMap());
            Object object = codec.decode(channel, content, Collections.<String, Object>emptyMap());
            Assert.assertTrue(object instanceof DefaultRequest);
            Assert.assertTrue(((DefaultRequest) object).isOctoProtocol());
        } catch (Exception e) {
            Assert.fail();
        }

        when(request.getCompressType()).thenReturn(CompressUtil.CompressType.GZIP);
        try {
            byte[] content = codec.encode(channel, request, Collections.<String, Object>emptyMap());
            Object object = codec.decode(channel, content, Collections.<String, Object>emptyMap());
            Assert.assertTrue(object instanceof DefaultRequest);
        } catch (Exception e) {
            Assert.fail();
        }

        when(request.getServiceName()).thenReturn("Nothing.Iface");
        try {
            byte[] content = codec.encode(channel, request, Collections.<String, Object>emptyMap());
            codec.decode(channel, content, Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }

        when(request.getServiceName()).thenReturn("Echo.Iface");
        when(request.getServiceInterface()).thenAnswer(new Answer<Class<?>>() {
            @Override
            public Class<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return HelloService.Iface.class;
            }
        });
        try {
            codec.encode(channel, request, Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }


        when(request.getServiceInterface()).thenAnswer(new Answer<Class<?>>() {
            @Override
            public Class<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Echo.Iface.class;
            }
        });
        whenNew(TSerializer.class).withNoArguments().thenThrow(new TException());
        try {
            codec.encode(channel, request, Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }
    }

    @Test
    public void testOctoExceptionResponse() {
        when(response.isOctoProtocol()).thenReturn(true);
        when(response.getException()).thenReturn(new TException());

        try {
            byte[] content = codec.encode(channel, response, Collections.<String, Object>emptyMap());
            Object object = codec.decode(channel, content, Collections.<String, Object>emptyMap());
            Assert.assertTrue(object instanceof DefaultResponse);
            Assert.assertTrue(((DefaultResponse)object).getException() instanceof RpcException);
        } catch (Exception e) {
            Assert.fail();
        }

        when(response.getException()).thenReturn(new TransportException("mock transportException."));
        try {
            byte[] content = codec.encode(channel, response, Collections.<String, Object>emptyMap());
            Object object = codec.decode(channel, content, Collections.<String, Object>emptyMap());
            Assert.assertTrue(object instanceof DefaultResponse);
            Assert.assertTrue(((DefaultResponse)object).getException() instanceof TransportException);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testOctoResponse() throws Exception {
        when(response.isOctoProtocol()).thenReturn(true);
        try {
            byte[] content = codec.encode(channel, response, Collections.<String, Object>emptyMap());
            Object object = codec.decode(channel, content, Collections.<String, Object>emptyMap());
            Assert.assertTrue(object instanceof DefaultResponse);
            Assert.assertEquals(((DefaultResponse)object).getResult().getReturnVal(), "echo");
        } catch (Exception e) {
            Assert.fail();
        }

        when(response.getCompressType()).thenReturn(CompressUtil.CompressType.GZIP);
        try {
            byte[] content = codec.encode(channel, response, Collections.<String, Object>emptyMap());
            Object object = codec.decode(channel, content, Collections.<String, Object>emptyMap());
            Assert.assertTrue(object instanceof DefaultResponse);
        } catch (Exception e) {
            Assert.fail();
        }

        ServiceInvocationRepository.removeAndGetFuture(request.getSeq());
        try {
            byte[] content = codec.encode(channel, response, Collections.<String, Object>emptyMap());
            codec.decode(channel, content, Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TimeoutException);
        }

        ServiceInvocationRepository.putRequestAndFuture(request, future);
        when(response.getServiceInterface()).thenAnswer(new Answer<Class<?>>() {
            @Override
            public Class<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return HelloService.Iface.class;
            }
        });
        try {
            codec.encode(channel, response, Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }


        when(response.getServiceInterface()).thenAnswer(new Answer<Class<?>>() {
            @Override
            public Class<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Echo.Iface.class;
            }
        });
        whenNew(TDeserializer.class).withNoArguments().thenThrow(new TException());
        try {
            byte[] content = codec.encode(channel, response, Collections.<String, Object>emptyMap());
            codec.decode(channel, content, Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }

        try {
            codec.decode(channel, new byte[4], Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }
    }

    @Test
    public void testErrorObject() {
        try {
            codec.encode(channel, new Object(), Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
            Assert.assertTrue(e.getMessage().contains("Encode object type is invalid."));
        }
    }

    @Test
    public void testHeartbeatRequest() {
        when(request.getMessageType()).thenReturn(Constants.MESSAGE_TYPE_HEART);
        when(request.isOctoProtocol()).thenReturn(true);
        try {
            byte[] content = codec.encode(channel, request, Collections.<String, Object>emptyMap());
            Object object = codec.decode(channel, content, Collections.<String, Object>emptyMap());
            Assert.assertTrue(object instanceof DefaultRequest);
            Assert.assertTrue(((DefaultRequest) object).isHeartbeat());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testThriftRequest() {
        when(request.isOctoProtocol()).thenReturn(false);
        Map<String, Object> attachments = new HashMap<>();
        attachments.put(Constants.SERVICE_IFACE, Echo.Iface.class);
        try {
            byte[] content = codec.encode(channel, request, Collections.<String, Object>emptyMap());
            Object object = codec.decode(channel, content, attachments);
            Assert.assertTrue(object instanceof DefaultRequest);
            Assert.assertTrue(!((DefaultRequest) object).isOctoProtocol());
        } catch (Exception e) {
            Assert.fail();
        }

        attachments.clear();
        try {
            byte[] content = codec.encode(channel, request, Collections.<String, Object>emptyMap());
            codec.decode(channel, content, attachments);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }
    }

    @Test
    public void testThriftResponse() {
        when(response.isOctoProtocol()).thenReturn(false);
        Map<String, Object> attachments = new HashMap<>();
        attachments.put(Constants.SERVICE_IFACE, Echo.Iface.class);
        try {
            byte[] content = codec.encode(channel, response, Collections.<String, Object>emptyMap());
            Object object = codec.decode(channel, content, attachments);
            Assert.assertTrue(object instanceof DefaultResponse);
            Assert.assertEquals(((DefaultResponse)object).getResult().getReturnVal(), "echo");
        } catch (Exception e) {
            Assert.fail();
        }
    }

}

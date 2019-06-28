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
package com.meituan.dorado.transport.http.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.rpc.handler.http.DefaultHttpResponse;
import com.meituan.dorado.transport.http.HttpSender;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyHttpSender implements HttpSender {
    private static final Logger logger = LoggerFactory.getLogger(NettyHttpSender.class);

    private HttpRequest request;
    private Channel channel;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public NettyHttpSender(Channel channel, HttpRequest request) {
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void send(DefaultHttpResponse httpResponse) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(httpResponse.getContent()));
        setHeaders(response, httpResponse.getContentType());
        channel.writeAndFlush(response);
    }

    @Override
    public void sendObjectJson(Object object) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(object);
            DefaultHttpResponse response = new DefaultHttpResponse(bytes, Constants.CONTENT_TYPE_JSON);
            send(response);
        } catch (JsonProcessingException e) {
            logger.error("Object to json failed", e);
            sendErrorResponse("Object to json failed: " + e.getMessage());
        }
    }

    @Override
    public void sendErrorResponse(String errorMsg) {
        try {
            String returnMessage = genReturnMessage(false, errorMsg);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(returnMessage.getBytes("UTF-8")));
            setHeaders(response, Constants.CONTENT_TYPE_JSON);
            channel.writeAndFlush(response);
        } catch (Exception e) {
            logger.error("Send error response failed", e);
        }
    }

    private void setHeaders(FullHttpResponse response, String contentType) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
        }
    }

    private String genReturnMessage(boolean isSuccess, String result) throws JsonProcessingException {
        ReturnMessage returnMessage = new ReturnMessage(isSuccess, result);
        return objectMapper.writeValueAsString(returnMessage);
    }
}

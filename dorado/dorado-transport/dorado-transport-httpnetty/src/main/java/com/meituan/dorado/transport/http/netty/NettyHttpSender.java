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

import com.meituan.dorado.common.Constants;
import com.meituan.dorado.rpc.handler.http.DefaultHttpResponse;
import com.meituan.dorado.transport.http.HttpSender;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyHttpSender implements HttpSender {

    private HttpRequest request;
    private Channel channel;

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
    public void sendErrorResponse(String errorMsg) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(errorMsg.getBytes()));
        setHeaders(response, Constants.CONTENT_TYPE_JSON);
        channel.writeAndFlush(response);
    }

    private void setHeaders(FullHttpResponse response, String contentType) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
        }
    }
}

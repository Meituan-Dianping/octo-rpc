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

import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.rpc.handler.http.DefaultHttpResponse;
import com.meituan.dorado.rpc.handler.http.HttpHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttpServerHandler.class);

    private HttpRequest request;
    private HttpHandler httpHandler;
    private NettyHttpServer server;
    private byte[] frontBuff;

    public NettyHttpServerHandler(HttpHandler httpHandler, NettyHttpServer server) {
        this.httpHandler = httpHandler;
        this.server = server;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            request = (HttpRequest) msg;
        }
        if (msg instanceof HttpContent) {
            if ("/favicon.ico".equals(request.uri())) {
                return;
            }
            if (msg instanceof LastHttpContent) {
                HttpContent content = (HttpContent) msg;
                ByteBuf buf = content.content();
                byte[] buffer = new byte[buf.readableBytes()];
                buf.readBytes(buffer);
                buf.release();

                byte[] fullBuff;
                if (frontBuff != null) {
                    fullBuff = new byte[frontBuff.length + buffer.length];
                    System.arraycopy(frontBuff, 0, fullBuff, 0, frontBuff.length);
                    System.arraycopy(buffer, 0, fullBuff, frontBuff.length, buffer.length);
                    frontBuff = null;
                } else {
                    fullBuff = buffer;
                }

                NettyHttpSender httpSender = new NettyHttpSender(ctx.channel(), request);
                try {
                    httpHandler.handle(httpSender, request.uri(), fullBuff);
                } catch (Throwable e) {
                    logger.warn("Handle http request({}) exception,", request.uri(), e);
                    String errorMsg = e.getMessage();
                    errorMsg = errorMsg == null ? "" : errorMsg;
                    DefaultHttpResponse httpResponse = new DefaultHttpResponse();
                    httpResponse.generateFailContent(e.getClass().getName() + ":" + errorMsg);
                    httpSender.send(httpResponse);
                }
            } else {
                HttpContent content = (HttpContent) msg;
                ByteBuf buf = content.content();
                byte[] buffer = new byte[buf.readableBytes()];
                buf.readBytes(buffer);
                buf.release();

                if (frontBuff == null) {
                    frontBuff = buffer;
                } else {
                    byte[] newBuff = new byte[frontBuff.length + buffer.length];
                    System.arraycopy(frontBuff, 0, newBuff, 0, frontBuff.length);
                    System.arraycopy(buffer, 0, newBuff, frontBuff.length, buffer.length);
                    frontBuff = newBuff;
                }
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        server.getChannels().put(NetUtil.toIpPort((InetSocketAddress) ctx.channel().remoteAddress()), ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        server.getChannels().remove(NetUtil.toIpPort((InetSocketAddress) ctx.channel().remoteAddress()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Netty http request fail", cause);
        ctx.close();
    }
}

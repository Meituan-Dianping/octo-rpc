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
package com.meituan.dorado.transport.netty;

import com.meituan.dorado.codec.Codec;
import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.common.extension.ExtensionLoader;
import com.meituan.dorado.transport.LengthDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NettyCodec extends ByteToMessageCodec {

    private static final Logger logger = LoggerFactory.getLogger(NettyCodec.class);

    private static LengthDecoder lengthDecoder;

    private final Codec codec;
    private final Map<String, Object> attachments;

    static {
        try {
            lengthDecoder = ExtensionLoader.getExtension(LengthDecoder.class);
        } catch (Throwable e) {
            logger.error("No {} implement", LengthDecoder.class, e);
        }

    }

    public NettyCodec(Codec codec, Map<String, Object> attachments) {
        if (lengthDecoder == null) {
            throw new ProtocolException("No " + LengthDecoder.class + " implement, cannot do codec.");
        }
        if (attachments == null) {
            this.attachments = Collections.emptyMap();
        } else {
            this.attachments = attachments;
        }
        this.codec = codec;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        NettyChannel nettyChannel = ChannelManager.getOrAddChannel(ctx.channel());
        out.writeBytes(codec.encode(nettyChannel, msg, attachments));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List out) throws Exception {
        int totalLength = lengthDecoder.decodeLength(in.nioBuffer());
        int readableBytes = in.readableBytes();
        if (totalLength < 0) {
            logger.debug("Not getting enough bytes to get totalLength.");
            return;
        }
        if (readableBytes < totalLength) {
            logger.debug("Not getting enough bytes, need {} bytes but got {} bytes", totalLength,
                    readableBytes);
            return;
        }

        NettyChannel nettyChannel = ChannelManager.getOrAddChannel(ctx.channel());
        byte[] buffer = new byte[totalLength];
        in.readBytes(buffer);

        out.add(codec.decode(nettyChannel, buffer, attachments));
    }
}

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

import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.serialize.DoradoSerializerFactory;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.DefaultResponse;

public class DoradoCodec extends OctoCodec {

    @Override
    protected RpcInvocation decodeReqBody(byte serialize, byte[] buff, DefaultRequest request) {
        if (serialize == DoradoSerializerFactory.SerializeType.THRIFT_CODEC.getCode()) {
            return super.decodeReqBody(serialize, buff, request);
        }
        throw new ProtocolException(this.getClass().getSimpleName() + " just support thrift codec now!");
    }

    @Override
    protected RpcResult decodeRspBody(byte serialize, byte[] buff, DefaultResponse response) {
        if (serialize == DoradoSerializerFactory.SerializeType.THRIFT_CODEC.getCode()) {
            return super.decodeRspBody(serialize, buff, response);
        }
        throw new ProtocolException(this.getClass().getSimpleName() + " just support thrift codec now!");
    }

    @Override
    protected byte[] encodeReqBody(byte serialize, DefaultRequest request) {
        if (serialize == DoradoSerializerFactory.SerializeType.THRIFT_CODEC.getCode()) {
            return super.encodeReqBody(serialize, request);
        }
        throw new ProtocolException(this.getClass().getSimpleName() + " just support thrift codec now!");
    }

    @Override
    protected byte[] encodeRspBody(byte serialize, DefaultResponse response) {
        if (serialize == DoradoSerializerFactory.SerializeType.THRIFT_CODEC.getCode()) {
            return super.encodeRspBody(serialize, response);
        }
        throw new ProtocolException(this.getClass().getSimpleName() + " just support thrift codec now!");
    }
}

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
package com.meituan.dorado.serialize;

import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.serialize.protostuff.ProtostuffSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DoradoSerializerFactory {

    private static final Logger logger = LoggerFactory.getLogger(DoradoSerializerFactory.class);

    private final static ConcurrentMap<Byte, Serializer> serializers = new ConcurrentHashMap<Byte, Serializer>();

    public static Serializer getSerializer(byte serializerCode) {
        if (SerializeType.THRIFT_CODEC.getCode() == serializerCode) {
            return null;
        }
        Serializer serializer = serializers.get(serializerCode);
        if (serializer == null) {
            serializer = registerSerializer(serializerCode);
        }
        return serializer;
    }

    private static Serializer registerSerializer(byte serializerCode) {
        Serializer serializer;
        if (SerializeType.PROTOSTUFF.getCode() == serializerCode) {
            serializer = new ProtostuffSerializer();
        } else {
            throw new ProtocolException("Get Serializer failed: no corresponding Serializer of code=" + serializerCode);
        }
        serializers.putIfAbsent(serializerCode, serializer);
        return serializer;
    }

    public enum SerializeType {
        THRIFT_CODEC((byte) 1, "thriftCodec"),
        THRIFT((byte) 2, "thrift"),
        PROTOSTUFF((byte) 3, "protostuff");

        private byte code;
        private String serialize;

        SerializeType(byte code, String serialize) {
            this.code = code;
            this.serialize = serialize;
        }

        public byte getCode() {
            return code;
        }

        public String getSerialize() {
            return serialize;
        }

        public static byte getSerializeCode(String serialize) {
            if (THRIFT_CODEC.getSerialize().equalsIgnoreCase(serialize)) {
                return THRIFT_CODEC.code;
            } else if (THRIFT.getSerialize().equalsIgnoreCase(serialize)) {
                // 后续支持
                return THRIFT.code;
            } else if (PROTOSTUFF.getSerialize().equalsIgnoreCase(serialize)) {
                // 后续支持
                return PROTOSTUFF.code;
            } else {
                logger.error("Get Serializer code failed, serializeName={}, will use default {}", serialize, THRIFT_CODEC.serialize);
                return THRIFT_CODEC.code;
            }
        }
    }

}

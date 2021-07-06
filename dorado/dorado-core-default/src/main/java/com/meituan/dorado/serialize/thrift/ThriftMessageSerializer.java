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
package com.meituan.dorado.serialize.thrift;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TTransport;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class ThriftMessageSerializer {

    protected static final org.apache.thrift.protocol.TField MTRACE_FIELD_DESC = new org.apache.thrift.protocol.TField(
            "mtrace", org.apache.thrift.protocol.TType.STRUCT, (short) 32767);

    protected static final int INITIAL_BYTE_ARRAY_SIZE = 1024;

    protected static final ConcurrentMap<String, Method> cachedMethod = new ConcurrentHashMap<>();
    protected static final ConcurrentMap<String, Class<?>> cachedClass = new ConcurrentHashMap<>();

    protected abstract byte[] serialize(Object obj) throws Exception;

    protected abstract Object deserialize4OctoThrift(byte[] buff, Object obj) throws Exception;

    protected abstract Object deserialize4Thrift(byte[] buff, Class<?> iface, Map<String, Object> attachments) throws Exception;

    protected boolean hasOldRequestHeader(TProtocol protocol) {
        TTransport trans = protocol.getTransport();
        if (trans.getBytesRemainingInBuffer() >= 3) {
            byte type = trans.getBuffer()[trans.getBufferPosition()];
            if (type == org.apache.thrift.protocol.TType.STRUCT) {
                short id = (short) (((trans.getBuffer()[trans.getBufferPosition() + 1] & 0xff) << 8) | ((trans.getBuffer()[trans.getBufferPosition() + 2] & 0xff)));
                if (id == MTRACE_FIELD_DESC.id) {
                    return true;
                }
            }
        }
        return false;
    }

    protected byte[] getActualBytes(TMemoryBuffer memoryBuffer) {
        byte[] actualBytes = new byte[memoryBuffer.length()];
        System.arraycopy(memoryBuffer.getArray(), 0, actualBytes, 0, memoryBuffer.length());
        return actualBytes;
    }

    public static class ThriftMessageInfo {
        String methodName;
        int seqId;
        boolean oldProtocol;

        public ThriftMessageInfo(String methodName, int seqId) {
            this.methodName = methodName;
            this.seqId = seqId;
        }

        public boolean isOldProtocol() {
            return oldProtocol;
        }

        public void setOldProtocol(boolean oldProtocol) {
            this.oldProtocol = oldProtocol;
        }
    }
}

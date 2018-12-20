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
package com.meituan.dorado.transport;

import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.util.BytesUtil;

import java.nio.ByteBuffer;

public class DoradoLengthDecoder implements LengthDecoder {

    private static final byte[] MAGIC = new byte[] { (byte) 0xab, (byte) 0xba };
    private static final int LEAST_BYTE_SIZE = 4;

    @Override
    public int decodeLength(ByteBuffer in) {
        if (in.remaining() < LEAST_BYTE_SIZE) {
            return -1;
        }

        byte[] first4Bytes = new byte[4];
        in.get(first4Bytes);

        if (first4Bytes[0] == MAGIC[0] && first4Bytes[1] == MAGIC[1]) {
            if (in.remaining() < LEAST_BYTE_SIZE) {
                return -1;
            } else {
                return in.getInt() + 8;
            }
        } else {
            int originalThriftSize = BytesUtil.bytes2int(first4Bytes, 0);
            if (originalThriftSize > 0) {
                return originalThriftSize + 4;
            } else {
                throw new ProtocolException("Receiving data not octo protocol or original thrift protocol!");
            }
        }
    }
}

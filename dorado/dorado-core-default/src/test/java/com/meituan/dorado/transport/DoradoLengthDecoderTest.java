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
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class DoradoLengthDecoderTest {

    @Test
    public void testOctoDecodeLength() {
        byte[] byteArray = {(byte) 0xab, (byte) 0xba, (byte) 0x0, (byte) 0x0,
                (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x12};
        ByteBuffer in = ByteBuffer.wrap(byteArray);

        DoradoLengthDecoder decoder = new DoradoLengthDecoder();
        int length = decoder.decodeLength(in);
        Assert.assertTrue(length == 26);

        // byte array less than the least length
        byteArray = new byte[]{(byte) 0xab, (byte) 0xba, (byte) 0x0, (byte) 0x0,
                (byte) 0x0, (byte) 0x0};
        in = ByteBuffer.wrap(byteArray);
        length = decoder.decodeLength(in);
        Assert.assertTrue(length == -1);
    }

    @Test
    public void testEmptyDecodeLength() {
        byte[] byteArray = new byte[0];
        ByteBuffer in = ByteBuffer.wrap(byteArray);

        DoradoLengthDecoder decoder = new DoradoLengthDecoder();
        int length = decoder.decodeLength(in);
        Assert.assertTrue(length == -1);
    }

    @Test
    public void testThriftDecodeLength() {
        DoradoLengthDecoder decoder = new DoradoLengthDecoder();

        byte[] byteArray = {(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x12};
        ByteBuffer in = ByteBuffer.wrap(byteArray);
        int length = decoder.decodeLength(in);
        Assert.assertTrue(length == 22);

        // byte array length equals 0
        byteArray = new byte[]{(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x00};
        in = ByteBuffer.wrap(byteArray);
        try {
            decoder.decodeLength(in);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ProtocolException);
        }
    }
}

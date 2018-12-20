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
package com.meituan.dorado.util;

import org.junit.Assert;
import org.junit.Test;

public class BytesUtilTest {

    @Test
    public void testLong2bytes() {
        byte[] data = new byte[8];
        BytesUtil.long2bytes(12L, data, 0);
        long value = BytesUtil.bytes2long(data, 0);
        Assert.assertTrue(value == 12L);
    }

    @Test
    public void testInt2bytes() {
        byte[] data = new byte[4];
        BytesUtil.int2bytes(12, data, 0);
        int value = BytesUtil.bytes2int(data, 0);
        Assert.assertTrue(value == 12);
    }

    @Test
    public void testShort2bytes() {
        byte[] data = new byte[2];
        BytesUtil.short2bytes((short) 12, data, 0);
        short value = BytesUtil.bytes2short(data, 0);
        Assert.assertTrue(value == (short) 12);
    }

    @Test
    public void testBytesEquals() {
        String message = "message";
        Assert.assertTrue(BytesUtil.bytesEquals(message.getBytes(), message.getBytes()));

        String message2 = "message2";
        Assert.assertTrue(!BytesUtil.bytesEquals(message.getBytes(), message2.getBytes()));

        String message3 = "massage";
        Assert.assertTrue(!BytesUtil.bytesEquals(message.getBytes(), message3.getBytes()));

        Assert.assertTrue(BytesUtil.bytesEquals(null,null));
        Assert.assertTrue(!BytesUtil.bytesEquals(null,message2.getBytes()));
    }

}

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


import com.meituan.dorado.util.CompressUtil.CompressType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CompressUtilTest {

    @Test
    public void testCompressGZip() throws IOException {
        String message = "test dorado compress gzip";
        byte[] compressData = CompressUtil.compressGZip(message.getBytes());
        byte[] data = CompressUtil.uncompressGZip(compressData);
        Assert.assertEquals("test dorado compress gzip", new String(data));

        // null data
        compressData = CompressUtil.compressGZip(null);
        data = CompressUtil.uncompressGZip(compressData);
        Assert.assertNull(data);
    }

    @Test
    public void testCompressSnappy() throws IOException {
        String message = "test dorado compress snappy";
        byte[] compressData = CompressUtil.compressSnappy(message.getBytes());
        byte[] data = CompressUtil.uncompressSnappy(compressData);
        Assert.assertEquals("test dorado compress snappy", new String(data));

        // null data
        compressData = CompressUtil.compressSnappy(null);
        try {
            CompressUtil.uncompressSnappy(compressData);
        } catch(Exception e) {
            Assert.assertTrue(e instanceof IOException);
        }
    }

    @Test
    public void testCompress() throws IOException {
        String message = "message";

        CompressType type = CompressType.NO;
        byte[] compressData = type.compress(message.getBytes());
        byte[] data = type.uncompress(compressData);
        Assert.assertEquals("message", new String(data));

        type = CompressType.SNAPPY;
        compressData = type.compress(message.getBytes());
        data = type.uncompress(compressData);
        Assert.assertEquals("message", new String(data));

        type = CompressType.GZIP;
        compressData = type.compress(message.getBytes());
        data = type.uncompress(compressData);
        Assert.assertEquals("message", new String(data));
    }
}

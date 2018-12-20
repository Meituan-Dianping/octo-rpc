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

import org.xerial.snappy.Snappy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressUtil {

    public static byte[] compressGZip(byte[] data) throws IOException {
        if (data == null) {
            return null;
        }
        byte[] bytes;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = null;
        try {
            gzip = new GZIPOutputStream(bos);
            gzip.write(data);
            gzip.finish();
            bytes = bos.toByteArray();
        } finally {
            if (gzip != null) {
                gzip.close();
            }
            bos.close();
        }
        return bytes;
    }

    /***
     * 解压GZip
     *
     * @param data
     * @return
     */
    public static byte[] uncompressGZip(byte[] data) throws IOException {
        if (data == null) {
            return null;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes;
        GZIPInputStream gzip = null;
        try {
            gzip = new GZIPInputStream(bis);
            byte[] buf = new byte[1024];
            int num = -1;
            while ((num = gzip.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, num);
            }
            bytes = baos.toByteArray();
            baos.flush();
        } finally {
            baos.close();
            if (gzip != null) {
                gzip.close();
            }
            bis.close();
        }
        return bytes;
    }

    /**
     * Snappy压缩
     * @param data
     * @return
     * @throws IOException
     */
    public static byte[] compressSnappy(byte[] data) throws IOException {
        if (data == null) {
            return new byte[0];
        }
        return Snappy.compress(data);
    }

    /**
     * Snappy解压
     * @param data
     * @return
     * @throws IOException
     */
    public static byte[] uncompressSnappy(byte[] data) throws IOException {
        if (data == null) {
            return new byte[0];
        }
        return Snappy.uncompress(data);
    }

    public enum CompressType {
        NO, SNAPPY, GZIP;

        public byte[] compress(byte[] data) throws IOException {
            if (this.equals(SNAPPY)) {
                return compressSnappy(data);
            } else if (this.equals(GZIP)) {
                return compressGZip(data);
            } else {
                return data;
            }
        }

        public byte[] uncompress(byte[] data) throws IOException {
            if (this.equals(SNAPPY)) {
                return uncompressSnappy(data);
            } else if (this.equals(GZIP)) {
                return uncompressGZip(data);
            } else {
                return data;
            }
        }
    }
}

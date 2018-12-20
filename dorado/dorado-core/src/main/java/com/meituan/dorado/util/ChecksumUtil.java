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


import java.util.zip.Adler32;

public class ChecksumUtil {

    public static byte[] genAdler32ChecksumBytes(byte[] bytes) {
        Adler32 a32 = new Adler32();
        a32.update(bytes, 0, bytes.length);
        int sum = (int) a32.getValue();
        byte[] checksum = new byte[4];
        checksum[0] = (byte) (sum >> 24);
        checksum[1] = (byte) (sum >> 16);
        checksum[2] = (byte) (sum >> 8);
        checksum[3] = (byte) (sum);
        return checksum;
    }
}

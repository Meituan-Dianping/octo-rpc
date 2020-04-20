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
package com.meituan.dorado.mock;


import com.meituan.dorado.codec.Codec;
import com.meituan.dorado.transport.Channel;

import java.io.IOException;
import java.util.Map;

public class MockCodec implements Codec {

    @Override
    public byte[] encode(Channel channel, Object message, Map<String, Object> attachments) {
        return new byte[0];
    }

    @Override
    public Object decode(Channel channel, byte[] buffer, Map<String, Object> attachments) {
        return null;
    }
}

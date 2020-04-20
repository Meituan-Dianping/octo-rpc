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
package com.meituan.dorado.codec;

import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.common.extension.SPI;
import com.meituan.dorado.transport.Channel;

import java.util.Map;

@SPI
public interface Codec {

    byte[] encode(Channel channel, Object message, Map<String, Object> attachments) throws ProtocolException;

    Object decode(Channel channel, byte[] buffer, Map<String, Object> attachments) throws ProtocolException;
}

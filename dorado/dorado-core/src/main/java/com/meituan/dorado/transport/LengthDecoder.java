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

import com.meituan.dorado.common.extension.SPI;

import java.nio.ByteBuffer;

@SPI
public interface LengthDecoder {

    /**
     * 从数据源中解析本次会话所需的数据size
     *
     * @param in 数据源
     * @return 所需数据长度，负值表示不合法的情况
     */
    int decodeLength(ByteBuffer in);
}

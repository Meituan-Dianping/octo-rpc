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
package com.meituan.dorado.transport.netty;

import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.config.service.ProviderConfig;
import com.meituan.dorado.config.service.ReferenceConfig;

public class NettyTest {

    protected static final String IP = "127.0.0.1";
    protected static final int PORT = 9999;

    protected static ProviderConfig genProviderConfig() {
        ProviderConfig cfg = new ProviderConfig();
        cfg.setPort(PORT);
        return cfg;
    }

    protected ClientConfig genClientConfig() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setTimeout(1000);
        referenceConfig.setConnTimeout(1000);
        ClientConfig cfg = new ClientConfig(referenceConfig);
        cfg.setAddress(IP, PORT);
        return cfg;
    }
}

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


import com.meituan.dorado.MockUtil;
import com.meituan.dorado.mock.MockClient;
import com.meituan.dorado.mock.MockCodec;
import com.meituan.dorado.mock.MockServer;
import org.junit.Assert;
import org.junit.Test;

public class TransportTest {

    @Test
    public void test() {
        MockServer server = MockUtil.getServer();
        Assert.assertTrue(!server.isClosed());
        Assert.assertTrue(server.getCodec() instanceof MockCodec);

        server.close();
        Assert.assertTrue(server.isClosed());

        MockClient client = MockUtil.getClient();
        client.connect();
        Assert.assertTrue(!client.isClosed());
        Assert.assertTrue(client.isConnected());

        try {
            client.request(MockUtil.getRequest());
        } catch(Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            client.request(new Object());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        client.disconnect();
        client.close();
        Assert.assertTrue(client.isClosed());
    }
}

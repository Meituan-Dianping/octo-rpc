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
package com.meituan.dorado.testclient;

import com.meituan.dorado.test.thrift.api.Echo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
public class ClientTest {
    private static final Logger logger = LoggerFactory.getLogger(ClientTest.class);

    @Resource(name = "client1")
    private Echo.Iface client1;
    @Resource(name = "client2")
    private Echo.Iface client2;
    @Resource(name = "client3")
    private Echo.Iface client3;

    @PostConstruct
    public void startClientTest() {
        while (true) {
            try {
                logger.info(client1.echo("Client1 use thrift"));
            } catch (Exception e) {
                logger.error("Client1 use thrift failed.", e);
            }

            try {
                logger.info(client2.echo("Client2 use thrift"));
            } catch (Exception e) {
                logger.error("Client2 use thrift failed.", e);
            }

            try {
                logger.info(client3.echo("Client3 use thrift"));
            } catch (Exception e) {
                logger.error("Client3 use thrift failed.", e);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

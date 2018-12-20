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

package com.meituan.dorado.testserver;

import com.meituan.dorado.bootstrap.ServiceBootstrap;
import com.meituan.dorado.test.echo.Echo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ThriftClient {
    private static final Logger logger = LoggerFactory.getLogger(ThriftClient.class);

    public static void main(String[] args) {
        ClassPathXmlApplicationContext beanFactory = new ClassPathXmlApplicationContext("echozk/thrift-client.xml");
//        ClassPathXmlApplicationContext beanFactory = new ClassPathXmlApplicationContext("echomns/thrift-client.xml");

        for (int i = 0; i < 200; i++) {
            try {
                Echo.Iface client1 = (Echo.Iface) beanFactory.getBean("client1");
                System.out.println(client1.echo("Client1 use thrift"));
            } catch (Exception e) {
                logger.info("Client1 use thrift failed.", e);
            }

            Echo.Iface client2 = (Echo.Iface) beanFactory.getBean("client2");
            try {
                System.out.println(client2.echo("Client2 use thrift"));
            } catch (Exception e) {
                logger.info("Client2 use thrift failed.", e);
            }


            Echo.Iface client3 = (Echo.Iface) beanFactory.getBean("client3");
            try {
                System.out.println(client3.echo("Client3 use thrift"));
            } catch (Exception e) {
                logger.info("Client3 use thrift failed.", e);
            }
        }

        beanFactory.destroy();
        ServiceBootstrap.clearGlobalResource();
        System.exit(0);
    }
}

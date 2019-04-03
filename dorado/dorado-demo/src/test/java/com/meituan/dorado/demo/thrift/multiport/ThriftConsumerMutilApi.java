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
package com.meituan.dorado.demo.thrift.multiport;

import com.meituan.dorado.bootstrap.ServiceBootstrap;
import com.meituan.dorado.test.thrift.api.Echo;
import com.meituan.dorado.test.thrift.api.HelloService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ThriftConsumerMutilApi {
    public static void main(String[] args) {
        try {
            ClassPathXmlApplicationContext beanFactory = new ClassPathXmlApplicationContext("thrift/multiport/thrift-consumer-multiapi.xml");

            HelloService.Iface userservice = (HelloService.Iface) beanFactory.getBean("helloService");
            System.out.println(userservice.sayHello("Emma"));

            Echo.Iface echo = (Echo.Iface) beanFactory.getBean("echoService");
            System.out.println(echo.echo("Hello world"));

            beanFactory.destroy();
            ServiceBootstrap.clearGlobalResource();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

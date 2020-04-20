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
package com.meituan.dorado.demo.thrift.zkregistry;

import com.meituan.dorado.test.thrift.api.HelloService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 注意此方式需要在配置thrift/zkregistry/thrift-consumer.xml中配置你的zk服务地址
 */
public class ThriftConsumer {

    public static void main(String[] args) {
        try {
            ClassPathXmlApplicationContext beanFactory = new ClassPathXmlApplicationContext("thrift/zkregistry/thrift-consumer.xml");

            HelloService.Iface userService = (HelloService.Iface) beanFactory.getBean("helloService");
            System.out.println(userService.sayHello("Emma"));

            beanFactory.destroy();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

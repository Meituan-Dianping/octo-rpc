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
package com.meituan.dorado.demo.thrift.zkmock;

import com.meituan.dorado.demo.ConsoleCommandProcessor;
import com.meituan.dorado.demo.QuitOperation;
import org.apache.curator.test.TestingServer;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

public class ThriftProvider {

    protected static TestingServer zkServer;

    public static void initZkServer() throws Exception {
        int zkServerPort = 2200;
        zkServer = new TestingServer(zkServerPort, true);
    }

    public static void main(String[] args) throws Exception {
        initZkServer();
        ClassPathXmlApplicationContext beanFactory = new ClassPathXmlApplicationContext("thrift/zkmock/thrift-provider.xml");
        ConsoleCommandProcessor.processCommandsWithQuitOperation(beanFactory, new QuitOperation() {
            @Override
            public void prepareQuit() {
                try {
                    closeZkServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void closeZkServer() throws IOException {
        zkServer.stop();
    }
}

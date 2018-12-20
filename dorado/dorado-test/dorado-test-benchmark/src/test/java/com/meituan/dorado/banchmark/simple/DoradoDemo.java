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
package com.meituan.dorado.banchmark.simple;

import com.meituan.dorado.banchmark.simple.api.Echo;
import org.apache.thrift.TException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.Random;

public class DoradoDemo {
    private static int num = 100;

    public static void main(String[] args) throws TException, InterruptedException, IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("context.xml");
        final Echo.Iface client = context.getBean("clientProxy", Echo.Iface.class);

        Thread[] threads = new Thread[num];

        for (int i = 0; i < num; i++) {
            final int finalI = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String result = null;
                    Random random = new Random();
                    for (int j = 0; j < num; j++) {
                        try {
                            result = client.echo("hello" + finalI + "," + j);
                            Thread.sleep(random.nextInt(100));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        assert ("echo: hello" + finalI + "," + j).equals(result);
                    }
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        context.destroy();
        System.exit(0);
    }
}

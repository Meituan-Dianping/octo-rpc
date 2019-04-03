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
package com.meituan.dorado.server;

import com.meituan.dorado.test.thrift.api.Echo;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class EchoImpl implements Echo.Iface {

    public static final Logger logger = LoggerFactory.getLogger(EchoImpl.class);
    private static volatile AtomicLong counter = new AtomicLong(0);
    private static long timeWindow = 10;

    private static ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    static {
        service.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    long tps = counter.get() / timeWindow;
                    counter.set(0);
                    logger.info("TPS:" + tps);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, timeWindow, TimeUnit.SECONDS);
    }

    @Override
    public String echo(String messge) throws TException {
        counter.addAndGet(1);
        return messge;
    }
}

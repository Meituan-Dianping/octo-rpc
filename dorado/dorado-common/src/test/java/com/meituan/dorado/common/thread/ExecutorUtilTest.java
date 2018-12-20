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
package com.meituan.dorado.common.thread;


import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorUtilTest {

    @Test
    public void testExecutor() {
        List<ExecutorService> executors = new ArrayList<>();
        executors.add(Executors.newFixedThreadPool(1));
        executors.add(Executors.newFixedThreadPool(1));

        ExecutorUtil.shutdownExecutors(executors, 0);
        for (ExecutorService executorService : executors) {
            Assert.assertTrue(executorService.isShutdown());
        }
    }
}

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

import java.util.List;
import java.util.concurrent.*;

public class ExecutorUtil {

    private static final long DEFAULT_THREAD_KEEP_ALIVE_TIME = 30L;

    public static void shutdownExecutors(List<ExecutorService> executors, int timeoutMillis) {
        if (executors == null) {
            return;
        }
        for (ExecutorService executor : executors) {
            shutdownExecutor(executor, timeoutMillis);
        }
    }

    public static void shutdownExecutor(ExecutorService executor, int timeoutMillis) {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
        }
    }

    public static ThreadPoolExecutor getThreadPool(int corePoolSize, int maximumPoolSize, int workQueueSize,
                                                   BlockingQueue<Runnable> workQueue, DefaultThreadFactory threadFactory) {
        if (workQueue != null) {
            return new ThreadPoolExecutor(corePoolSize,
                    maximumPoolSize,
                    DEFAULT_THREAD_KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    workQueue,
                    threadFactory);
        } else if (workQueueSize > 0) {
            return new ThreadPoolExecutor(corePoolSize,
                    maximumPoolSize,
                    DEFAULT_THREAD_KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(workQueueSize),
                    threadFactory);
        } else {
            return new ThreadPoolExecutor(corePoolSize,
                    maximumPoolSize,
                    DEFAULT_THREAD_KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    threadFactory);
        }
    }
}

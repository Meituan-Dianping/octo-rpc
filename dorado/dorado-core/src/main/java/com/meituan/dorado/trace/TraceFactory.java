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
package com.meituan.dorado.trace;

import com.meituan.dorado.common.extension.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class TraceFactory {

    private static final Logger logger = LoggerFactory.getLogger(TraceFactory.class);

    private static volatile boolean initialized = false;
    private static List<InvokeTrace> invokeTraceList = Collections.emptyList();

    public synchronized static List<InvokeTrace> getInvokeTrace() {
        return invokeTraceList;
    }

    public synchronized static void initInvokeTrace(String appkey) {
        if (!initialized || invokeTraceList.isEmpty()) {
            invokeTraceList = ExtensionLoader.getExtensionList(InvokeTrace.class);
            if (invokeTraceList.isEmpty()) {
                logger.warn("No impl of InvokeTrace, will not do performance trace.");
            }
            for (InvokeTrace invokeTrace : invokeTraceList) {
                invokeTrace.init(appkey);
            }
            initialized = true;
        }
    }
}

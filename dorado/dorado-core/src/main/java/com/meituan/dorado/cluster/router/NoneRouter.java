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
package com.meituan.dorado.cluster.router;

import com.meituan.dorado.cluster.Router;
import com.meituan.dorado.rpc.handler.invoker.Invoker;

import java.util.List;

public class NoneRouter implements Router {

    private static final String NAME = "none";

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers) {
        return invokers;
    }

    @Override
    public String getName() {
        return NAME;
    }
}

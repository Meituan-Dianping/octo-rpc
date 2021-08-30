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
package com.meituan.dorado.rpc;

import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;

import java.util.List;

@ThriftService
public interface GenericService {

    @ThriftMethod
    String $invoke(String methodName, List<String> parameterTypes, List<String> parameters) throws Exception;

    String $invoke(String methodName, List<String> parameterTypes, Object[] parameters) throws Exception;
}

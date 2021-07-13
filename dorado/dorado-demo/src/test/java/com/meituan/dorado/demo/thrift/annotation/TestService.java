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
package com.meituan.dorado.demo.thrift.annotation;

import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import org.apache.thrift.TException;

@ThriftService
public interface TestService {

    @ThriftMethod
    public String testNull() throws TException;

    @ThriftMethod
    public String testException() throws TException;

    @ThriftMethod
    public String testMock(String str) throws TException;

    @ThriftMethod
    public void testTimeout() throws TException;

    @ThriftMethod
    public TestResponse testStruct(TestRequest testRequest) throws
            TException;

    @ThriftMethod
    public int testBaseTypeException() throws TException;

    @ThriftMethod(value = "methodAlias")
    public String testAlias(String str) throws TException;
}
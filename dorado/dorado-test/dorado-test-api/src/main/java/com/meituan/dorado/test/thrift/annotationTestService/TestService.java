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
package com.meituan.dorado.test.thrift.annotationTestService;

import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import org.apache.thrift.TException;

@ThriftService
public interface TestService {

    @ThriftMethod
    public String testNull() throws TException;

    @ThriftMethod(exception = {@ThriftException(type = MyException.class, id = 1),
            @ThriftException(type = InternalErrorException.class, id = 2)})
    public String testException() throws MyException, InternalErrorException, TException;

    @ThriftMethod
    public String testMock(String str) throws TException;

    @ThriftMethod
    public void testTimeout() throws TException;

    @ThriftMethod
    public TestResponse testStruct(TestRequest testRequest) throws
            TException;

    @ThriftMethod(exception = {@ThriftException(type = MyException.class, id = 1)})
    public int testBaseTypeException() throws MyException, TException;
}
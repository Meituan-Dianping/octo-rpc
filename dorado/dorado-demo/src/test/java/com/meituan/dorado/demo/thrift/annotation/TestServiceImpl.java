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

import org.apache.thrift.TException;

public class TestServiceImpl implements TestService {

    public String testNull() throws TException {
        System.out.println("testNull!");
        return "null";
    }

    public String testException() throws TException {
        System.out.println("testException!");
        throw new NullPointerException("My Error");
    }

    public String testMock(String str) throws TException {
        System.out.println("testMock!");
        return str;
    }

    public void testTimeout() throws TException {
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public TestResponse testStruct(TestRequest testRequest) throws TException {
        TestResponse testResponse = new TestResponse();
        testResponse.setUserid(testRequest.getUserid());
        testResponse.setMessage("haha" + testRequest.getMessage());
        testResponse.setSeqid(testRequest.getSeqid());
        return testResponse;
    }

    public int testBaseTypeException() throws TException {
        System.out.println("testBaseTypeException");
        throw new TException("My Error");
    }

    public String testAlias(String str) throws TException {
        System.out.println("testAlias!");
        return str;
    }
}
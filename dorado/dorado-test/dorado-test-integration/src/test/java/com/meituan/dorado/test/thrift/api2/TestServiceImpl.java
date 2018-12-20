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
package com.meituan.dorado.test.thrift.api2;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.transport.TTransportException;

public class TestServiceImpl implements TestService.Iface {

    public String testNull() throws TException {
        String str1 = null;
        str1.length();
        return "";
    }

    public String testException() throws MyException, TException {
        System.out.println("testException!");
        throw new MyException("error");
    }

    @Override
    public int testBaseTypeException() throws MyException, TException {
        System.out.println("testBaseTypeException!");
        throw new MyException("error");
    }

    public String testMock(String str) throws TException {
        System.out.println("testMock!");
        return str;
    }

    public long testLong(long n) throws TException {
        return n;
    }

    public void testProtocolMisMatch() throws TException {
        throw new TProtocolException("testProtocolMisMatch");
    }

    public void testTransportException() throws TException {
        throw new TTransportException("testTransportException");
    }

    public void testTimeout() throws TException {
        try {
            Thread.sleep(1000000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public String testReturnNull() throws TException {
        return null;
    }
}

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
package com.meituan.dorado.test.thrift.apisuite;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.transport.TTransportException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSuiteImpl implements TestSuite.Iface {

    @Override
    public void testVoid() throws TException {
    }

    @Override
    public String testString(String str) throws TException {
        return str;
    }

    @Override
    public long testLong(long n) throws TException {
        return n;
    }

    @Override
    public Message testMessage(Message message) throws TException {
        return message;
    }

    @Override
    public SubMessage testSubMessage(SubMessage message) throws TException {
        return message;
    }

    @Override
    public Map<Message, SubMessage> testMessageMap(Map<Message, SubMessage> messages) throws TException {
        return messages;
    }

    @Override
    public List<Message> testMessageList(List<Message> messages) throws TException {
        return messages;
    }

    @Override
    public SubMessage testStrMessage(String str, SubMessage message) throws TException {
        return message;
    }

    @Override
    public Map<String, String> multiParam(byte param1, int param2, long param3, double param4) throws TException {
        Map<String, String> result = new HashMap<String, String>();
        result.put(String.valueOf(param1), String.valueOf(param1));
        result.put(String.valueOf(param2), String.valueOf(param2));
        result.put(String.valueOf(param3), String.valueOf(param3));
        result.put(String.valueOf(param4), String.valueOf(param4));
        return result;
    }

    @Override
    public void testMockProtocolException() throws TException {
        throw new TProtocolException("test");
    }

    @Override
    public void testTimeout() throws TException {
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String testReturnNull() throws TException {
        return null;
    }

    @Override
    public String testNPE() throws TException {
        String str = null;
        str.length();
        return "";
    }

    @Override
    public String testIDLException() throws ExceptionA, TException {
        throw new ExceptionA("exceptionA happen");
    }

    @Override
    public String testMultiIDLException() throws ExceptionA, ExceptionB, TException {
        throw new ExceptionB("exceptionB happen");
    }

    @Override
    public int testBaseTypeReturnException() throws ExceptionA, TException {
        throw new ExceptionA("exceptionA happen");
    }
}

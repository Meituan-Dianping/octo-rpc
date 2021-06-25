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
package com.meituan.dorado.test.thrift.annotationTwitter;

import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import org.apache.thrift.TException;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ThriftService
public interface Twitter {

    @ThriftMethod
    public boolean testBool(boolean b) throws TException;

    @ThriftMethod
    public byte testByte(byte b) throws TException;

    @ThriftMethod
    public short testI16(short i) throws TException;

    @ThriftMethod
    public int testI32(int i) throws TException;

    @ThriftMethod
    public long testI64(long i) throws TException;

    @ThriftMethod
    public double testDouble(double d) throws TException;

    @ThriftMethod
    public ByteBuffer testBinary(ByteBuffer b) throws TException;

    @ThriftMethod
    public String testString(String s) throws TException;

    @ThriftMethod
    public List<String> testList(List<String> l) throws TException;

    @ThriftMethod
    public Set<String> testSet(Set<String> s) throws TException;

    @ThriftMethod
    public Map<String,String> testMap(Map<String, String> m) throws TException;

    @ThriftMethod
    public void testVoid() throws TException;

    @ThriftMethod
    public String testReturnNull() throws TException;

    @ThriftMethod
    public TweetSearchResult testStruct(String query) throws TException;

    @ThriftMethod(exception = {@ThriftException(type = TwitterUnavailable.class, id = 1)})
    public String testException(Tweet tweet) throws TwitterUnavailable, TException;

}
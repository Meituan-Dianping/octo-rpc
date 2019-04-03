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
package com.meituan.dorado.test.thrift.apitwitter;

import org.apache.thrift.TException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TwitterImpl implements Twitter.Iface {


    @Override
    public boolean testBool(boolean b) throws TException {
        return b;
    }

    @Override
    public byte testByte(byte b) throws TException {
        return b;
    }

    @Override
    public short testI16(short i) throws TException {
        return i;
    }

    @Override
    public int testI32(int i) throws TException {
        return i;
    }

    @Override
    public long testI64(long i) throws TException {
        return i;
    }

    @Override
    public double testDouble(double d) throws TException {
        return d;
    }

    @Override
    public ByteBuffer testBinary(ByteBuffer b) throws TException {
        return b;
    }

    @Override
    public String testString(String s) throws TException {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return s;
    }

    @Override
    public List<String> testList(List<String> l) throws TException {
        return l;
    }

    @Override
    public Set<String> testSet(Set<String> s) throws TException {
        return s;
    }

    @Override
    public Map<String, String> testMap(Map<String, String> m) throws TException {
        return m;
    }

    @Override
    public void testVoid() throws TException {

    }

    @Override
    public String testReturnNull() throws TException {
        return null;
    }

    @Override
    public TweetSearchResult testStruct(String query) throws TException {
        List<Tweet> tweets = new ArrayList<Tweet>();
        tweets.add(new Tweet(1, "1", "1"));
        tweets.add(new Tweet(2, "2", "2"));
        tweets.add(new Tweet(3, "3", "3"));
        TweetSearchResult result = new TweetSearchResult(tweets);
        return result;
    }

    @Override
    public String testException(Tweet tweet) throws TwitterUnavailable, TException {
        throw new TwitterUnavailable("mock twitter exception.");
    }
}

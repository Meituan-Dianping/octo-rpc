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

package com.meituan.dorado.test.thrift.normal;

import com.meituan.dorado.test.thrift.apitwitter.Tweet;
import com.meituan.dorado.test.thrift.apitwitter.TweetSearchResult;
import com.meituan.dorado.test.thrift.apitwitter.Twitter;
import com.meituan.dorado.test.thrift.apitwitter.TwitterUnavailable;
import com.meituan.dorado.test.thrift.filter.ClientQpsLimitFilter;
import com.meituan.dorado.test.thrift.filter.ServerQpsLimitFilter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.nio.ByteBuffer;
import java.util.*;

public class TwitterTest {
    private static ClassPathXmlApplicationContext clientBean;
    private static ClassPathXmlApplicationContext serverBean;
    private static Twitter.Iface oriThriftClient;
    private static Twitter.Iface octoProtocolClient;

    @BeforeClass
    public static void init() {
        serverBean = new ClassPathXmlApplicationContext("thrift/normal/twitter/thrift-provider.xml");
        clientBean = new ClassPathXmlApplicationContext("thrift/normal/twitter/thrift-consumer.xml");
        oriThriftClient = (Twitter.Iface) clientBean.getBean("oriThriftClient");
        octoProtocolClient = (Twitter.Iface) clientBean.getBean("octoProtocolClient");

        ClientQpsLimitFilter.disable();
        ServerQpsLimitFilter.disable();
    }

    @AfterClass
    public static void stop() {
        clientBean.destroy();
        serverBean.destroy();
    }

    @Test
    public void baseTypeTest() {
        try {
            boolean b = true;
            boolean result = oriThriftClient.testBool(b);
            Assert.assertEquals(b, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            byte b = 10;
            byte result = octoProtocolClient.testByte(b);
            Assert.assertEquals(b, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            short s = 100;
            short result = octoProtocolClient.testI16(s);
            Assert.assertEquals(s, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            int i = 1234;
            int result = oriThriftClient.testI32(i);
            Assert.assertEquals(i, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            long l = 123456;
            long result1 = octoProtocolClient.testI64(l);
            Assert.assertEquals(l, result1);
            long result2 = oriThriftClient.testI64(l);
            Assert.assertEquals(l, result2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            double d = 123456.789;
            double result1 = octoProtocolClient.testDouble(d);
            Assert.assertEquals(0, Double.compare(d, result1));
            double result2 = oriThriftClient.testDouble(d);
            Assert.assertEquals(0, Double.compare(d, result2));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            ByteBuffer b = ByteBuffer.wrap("test".getBytes());
            ByteBuffer result1 = octoProtocolClient.testBinary(b);
            Assert.assertEquals(b, result1);
            ByteBuffer result2 = oriThriftClient.testBinary(b);
            Assert.assertEquals(b, result2);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        String str = "test123456";
        StringBuilder stringBuilder = new StringBuilder(str);
        for (int i = 0; i < 100; i++) {
            stringBuilder.append(str);
            if (stringBuilder.length() > 10240)
                break;
        }
        try {
            for (int j = 0; j < 20; j++) {
                String result = octoProtocolClient.testString(stringBuilder.toString());
                Assert.assertEquals(stringBuilder.toString(), result);
            }
            for (int j = 0; j < 20; j++) {
                String result = oriThriftClient.testString(stringBuilder.toString());
                Assert.assertEquals(stringBuilder.toString(), result);
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void containersTest() {
        try {
            List<String> list = new ArrayList<String>();
            list.add("a");
            list.add("b");
            list.add("c");
            List<String> result1 = oriThriftClient.testList(list);
            Assert.assertEquals(list, (result1));
            List<String> result2 = octoProtocolClient.testList(list);
            Assert.assertEquals(list, (result2));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            Set<String> set = new HashSet<String>();
            set.add("a");
            set.add("b");
            set.add("c");
            Set<String> result1 = oriThriftClient.testSet(set);
            Assert.assertEquals(set, result1);
            Set<String> result2 = octoProtocolClient.testSet(set);
            Assert.assertEquals(set, result2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put("1", "a");
            map.put("2", "b");
            map.put("3", "c");
            Map<String, String> result1 = octoProtocolClient.testMap(map);
            Assert.assertEquals(map, result1);
            Map<String, String> result2 = oriThriftClient.testMap(map);
            Assert.assertEquals(map, result2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void otherTest() {
        try {
            octoProtocolClient.testVoid();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            List<Tweet> tweets = new ArrayList<Tweet>();
            tweets.add(new Tweet(1, "1", "1"));
            tweets.add(new Tweet(2, "2", "2"));
            tweets.add(new Tweet(3, "3", "3"));
            TweetSearchResult tweetSearchResult = new TweetSearchResult(tweets);

            TweetSearchResult result1 = oriThriftClient.testStruct("test");
            Assert.assertEquals(tweetSearchResult.getTweets(), result1.getTweets());
            TweetSearchResult result2 = octoProtocolClient.testStruct("test");
            Assert.assertEquals(tweetSearchResult.getTweets(), result2.getTweets());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            oriThriftClient.testException(new Tweet(1, "1", "1"));
        } catch (Exception e) {
            Assert.assertEquals(TwitterUnavailable.class, e.getClass());
        }
    }
}

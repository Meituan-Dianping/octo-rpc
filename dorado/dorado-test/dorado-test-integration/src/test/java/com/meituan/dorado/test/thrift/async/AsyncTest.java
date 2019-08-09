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
package com.meituan.dorado.test.thrift.async;

import com.meituan.dorado.rpc.AsyncContext;
import com.meituan.dorado.rpc.ResponseCallback;
import com.meituan.dorado.rpc.ResponseFuture;
import com.sankuai.mtthrift.testSuite.idlTest.Tweet;
import com.sankuai.mtthrift.testSuite.idlTest.TweetSearchResult;
import com.sankuai.mtthrift.testSuite.idlTest.Twitter;
import com.sankuai.mtthrift.testSuite.idlTest.TwitterUnavailable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AsyncTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;

    private static Twitter.Iface nettyClient;

    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/twitterAsync/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/twitterAsync/thrift-consumer.xml");

        nettyClient = (Twitter.Iface) clientBeanFactory.getBean("nettyClientProxy");
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    @Test
    public void testFuture() {
        try {
            ResponseFuture<String> future = AsyncContext.getContext().asyncCall(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return nettyClient.testString("Emma async");
                }
            });
            Assert.assertEquals("Emma async", future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            final ByteBuffer b = ByteBuffer.wrap("test".getBytes());
            ResponseFuture<ByteBuffer> future = AsyncContext.getContext().asyncCall(new Callable<ByteBuffer>() {
                @Override
                public ByteBuffer call() throws Exception {
                    return nettyClient.testBinary(b);
                }
            });
            Assert.assertEquals(b, future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            ResponseFuture<Short> future = AsyncContext.getContext().asyncCall(new Callable<Short>() {
                @Override
                public Short call() throws Exception {
                    return nettyClient.testI16((short) 1);
                }
            });
            Assert.assertTrue((short)1 == future.get());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            ResponseFuture<Double> future = AsyncContext.getContext().asyncCall(new Callable<Double>() {
                @Override
                public Double call() throws Exception {
                    return nettyClient.testDouble(2.0);
                }
            });
            double result = future.get();
            Assert.assertTrue(Double.compare(2.0, result) == 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            ResponseFuture<Boolean> future = AsyncContext.getContext().asyncCall(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return nettyClient.testBool(false);
                }
            });
            Assert.assertEquals(false, future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            ResponseFuture<Long> future = AsyncContext.getContext().asyncCall(new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    return nettyClient.testI64(2L);
                }
            });
            Assert.assertTrue(2L == future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            ResponseFuture<Integer> future = AsyncContext.getContext().asyncCall(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return nettyClient.testI32(2);
                }
            });
            Assert.assertTrue(2 == future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            final byte b = 10;
            ResponseFuture<Byte> future = AsyncContext.getContext().asyncCall(new Callable<Byte>() {
                @Override
                public Byte call() throws Exception {
                    return nettyClient.testByte(b);
                }
            });
            Assert.assertTrue(b == future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCallback() throws InterruptedException {
        //3. 异步回调
        ResponseFuture<String> future2 = AsyncContext.getContext().asyncCall(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return nettyClient.testString("Emma async callback");
            }
        });
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        future2.setCallback(new ResponseCallback<String>() {
            @Override
            public void onComplete(String result) {
                if ("Emma async callback".equals(result)) {
                    countDownLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        });
        boolean result = countDownLatch.await(5000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(result);


        ResponseFuture<Integer> future1 = AsyncContext.getContext().asyncCall(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return nettyClient.testI32(2);
            }
        });
        final CountDownLatch countDownLatch1 = new CountDownLatch(1);
        future1.setCallback(new ResponseCallback<Integer>() {
            @Override
            public void onComplete(Integer result) {
                if (2 == result) {
                    countDownLatch1.countDown();
                }
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        });
        result = countDownLatch1.await(5000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(result);
    }

    @Test
    public void callbackAsyncOtherTest() throws InterruptedException {
        final int timeout = 5000;
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            ResponseFuture<Void> future2 = AsyncContext.getContext().asyncCall(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    nettyClient.testVoid();
                    return null;
                }
            });
            future2.setCallback(new ResponseCallback<Void>() {
                @Override
                public void onComplete(Void result) {
                    countDownLatch.countDown();
                }
                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        boolean result = countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        Assert.assertEquals(true, result);


        final CountDownLatch countDownLatch1 = new CountDownLatch(1);
        try {
            ResponseFuture<String> future2 = AsyncContext.getContext().asyncCall(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return nettyClient.testReturnNull();
                }
            });
            future2.setCallback(new ResponseCallback<String>() {
                @Override
                public void onComplete(String result) {
                    if (result == null) {
                        countDownLatch1.countDown();
                    }
                }
                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        result = countDownLatch1.await(timeout, TimeUnit.MILLISECONDS);
        Assert.assertEquals(true, result);


        final CountDownLatch countDownLatch2 = new CountDownLatch(1);
        List<Tweet> tweets = new ArrayList<Tweet>();
        tweets.add(new Tweet(1, "1", "1"));
        tweets.add(new Tweet(2, "2", "2"));
        tweets.add(new Tweet(3, "3", "3"));
        final TweetSearchResult tweetSearchResult = new TweetSearchResult(tweets);
        try {
            ResponseFuture<TweetSearchResult> future2 = AsyncContext.getContext().asyncCall(new Callable<TweetSearchResult>() {
                @Override
                public TweetSearchResult call() throws Exception {
                    return nettyClient.testStruct("test");
                }
            });
            future2.setCallback(new ResponseCallback<TweetSearchResult>() {
                @Override
                public void onComplete(TweetSearchResult result) {
                    if (tweetSearchResult.equals(result)) {
                        countDownLatch2.countDown();
                    }
                }
                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        result = countDownLatch2.await(timeout, TimeUnit.MILLISECONDS);
        Assert.assertEquals(true, result);


        final CountDownLatch countDownLatch3 = new CountDownLatch(1);
        try {
            ResponseFuture<String> future2 = AsyncContext.getContext().asyncCall(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return nettyClient.testException(new Tweet(1, "1", "1"));
                }
            });
            future2.setCallback(new ResponseCallback<String>() {
                @Override
                public void onComplete(String result) {

                }
                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    if (e.getCause() instanceof TwitterUnavailable) {
                        countDownLatch3.countDown();
                    }
                }
            });
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        result = countDownLatch3.await(timeout, TimeUnit.MILLISECONDS);
        Assert.assertEquals(true, result);
    }

}
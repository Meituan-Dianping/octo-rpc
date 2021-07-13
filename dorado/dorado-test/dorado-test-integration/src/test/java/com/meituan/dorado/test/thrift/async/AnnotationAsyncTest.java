package com.meituan.dorado.test.thrift.async;

import com.meituan.dorado.rpc.AsyncContext;
import com.meituan.dorado.rpc.ResponseCallback;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.test.thrift.annotationTestService.TestService;
import com.meituan.dorado.test.thrift.annotationTwitter.Tweet;
import com.meituan.dorado.test.thrift.annotationTwitter.TweetSearchResult;
import com.meituan.dorado.test.thrift.annotationTwitter.Twitter;
import com.meituan.dorado.test.thrift.annotationTwitter.TwitterUnavailable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AnnotationAsyncTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static Twitter nettyClient;
    private static TestService testService;

    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/annotationAsync/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/annotationAsync/thrift-consumer.xml");

        nettyClient = (Twitter) clientBeanFactory.getBean("nettyClientProxy");
        testService = (TestService) clientBeanFactory.getBean("nettyClientProxy2");
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    /**
     * 测试 nettyIO 异步 使用注解时参数、返回值为基本类型的rpc调用
     */
    @Test
    public void nettyBaseTypeTest() {
        try {
            final boolean b = true;
            ResponseFuture<Boolean> future = AsyncContext.getContext().asyncCall(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return nettyClient.testBool(b);
                }
            });
            Assert.assertEquals(b, future.get());
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
            Assert.assertEquals(b, (byte) future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            final short s = 100;
            ResponseFuture<Short> future = AsyncContext.getContext().asyncCall(new Callable<Short>() {
                @Override
                public Short call() throws Exception {
                    return nettyClient.testI16(s);
                }
            });
            Assert.assertEquals(s, (short) future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            final int i = 1234;
            ResponseFuture<Integer> future = AsyncContext.getContext().asyncCall(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return nettyClient.testI32(i);
                }
            });
            Assert.assertEquals(i, (int) future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            final long l = 123456;
            ResponseFuture<Long> future = AsyncContext.getContext().asyncCall(new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    return nettyClient.testI64(l);
                }
            });
            Assert.assertEquals(l, (long) future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            final double d = 123456.789;
            ResponseFuture<Double> future = AsyncContext.getContext().asyncCall(new Callable<Double>() {
                @Override
                public Double call() throws Exception {
                    return nettyClient.testDouble(d);
                }
            });
            Assert.assertTrue(Double.compare(d, future.get()) == 0);
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
            final String s = "test";
            ResponseFuture<String> future = AsyncContext.getContext().asyncCall(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return nettyClient.testString(s);
                }
            });
            Assert.assertEquals(s, future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            final String s = "mock";
            ResponseFuture<String> future = AsyncContext.getContext().asyncCall(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return testService.testMock(s);
                }
            });
            Assert.assertEquals(s, future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * 测试 nettyIO 异步 使用注解时参数、返回值为容器类型的rpc调用
     */
    @Test
    public void nettyContainersTest(){
        try {
            final List<String> l = new ArrayList<String>(Arrays.asList(new String[] {"a", "b", "c"}));
            ResponseFuture<List<String>> future = AsyncContext.getContext().asyncCall(new Callable<List<String>>() {
                @Override
                public List<String> call() throws Exception {
                    return nettyClient.testList(l);
                }
            });
            Assert.assertEquals(l, future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            final Set<String> s = new HashSet<String>(Arrays.asList(new String[] {"a", "b", "c"}));
            ResponseFuture<Set<String>> future = AsyncContext.getContext().asyncCall(new Callable<Set<String>>() {
                @Override
                public Set<String> call() throws Exception {
                    return nettyClient.testSet(s);
                }
            });
            Assert.assertEquals(s, future.get());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            final Map<String, String> m = new HashMap<String, String>();
            m.put("1", "a");
            m.put("2", "b");
            m.put("3", "c");
            ResponseFuture<Map<String, String>> future = AsyncContext.getContext().asyncCall(new Callable<Map<String, String>>() {
                @Override
                public Map<String, String> call() throws Exception {
                    return nettyClient.testMap(m);
                }
            });
            Assert.assertEquals(m, future.get());
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
        List<Tweet> tweets = new ArrayList<>();
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
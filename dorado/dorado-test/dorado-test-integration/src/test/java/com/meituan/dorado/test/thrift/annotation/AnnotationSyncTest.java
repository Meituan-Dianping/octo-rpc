package com.meituan.dorado.test.thrift.annotation;

import com.meituan.dorado.test.thrift.annotationTestService.TestService;
import com.meituan.dorado.test.thrift.annotationTwitter.Tweet;
import com.meituan.dorado.test.thrift.annotationTwitter.TweetSearchResult;
import com.meituan.dorado.test.thrift.annotationTwitter.Twitter;
import com.meituan.dorado.test.thrift.annotationTwitter.TwitterUnavailable;
import org.apache.thrift.TException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.nio.ByteBuffer;
import java.util.*;

public class AnnotationSyncTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;
    private static Twitter nettyClient;
    private static TestService testService;

    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/annotationSync/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/annotationSync/thrift-consumer.xml");

        nettyClient = (Twitter) clientBeanFactory.getBean("nettyClientProxy");
        testService = (TestService) clientBeanFactory.getBean("nettyClientProxy2");
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    /**
     * 测试 nettyIO 同步 使用注解时参数、返回值为基本类型的rpc调用
     */
    @Test
    public void nettyBaseTypeTest() {

        try {
            boolean b = true;
            boolean result = nettyClient.testBool(b);
            Assert.assertEquals(b, result);
        } catch (TException e) {
            Assert.fail(e.getMessage());
        }

        try {
            byte b = 10;
            byte result = nettyClient.testByte(b);
            Assert.assertEquals(b, result);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }

        try {
            short s = 100;
            short result = nettyClient.testI16(s);
            Assert.assertEquals(s, result);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }

        try {
            int i = 1234;
            int result = nettyClient.testI32(i);
            Assert.assertEquals(i, result);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }

        try {
            long l = 123456;
            long result = nettyClient.testI64(l);
            Assert.assertEquals(l, result);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }

        try {
            double d = 123456.789;
            double result = nettyClient.testDouble(d);
            Assert.assertTrue(Double.compare(d, result) == 0);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }

        try {
            ByteBuffer b = ByteBuffer.wrap("test".getBytes());
            ByteBuffer result = nettyClient.testBinary(b);
            Assert.assertEquals(b, result);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }

        try {
            String s = "test";
            String result = nettyClient.testString(s);
            Assert.assertEquals(s, result);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }

        try {
            String s = "mock";
            String result = testService.testMock(s);
            Assert.assertEquals(s, result);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }
    }

    /**
     * 测试 nettyIO 同步 使用注解时参数、返回值为容器类型的rpc调用
     */
    @Test
    public void nettySyncContainersTest(){
        try {
            List<String> l = new ArrayList<String>(Arrays.asList(new String[] {"a", "b", "c"}));
            List<String> result = nettyClient.testList(l);
            Assert.assertEquals(l, result);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }

        try {
            Set<String> s = new HashSet<String>(Arrays.asList(new String[] {"a", "b", "c"}));
            Set<String> result = nettyClient.testSet(s);
            Assert.assertEquals(s, result);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }

        try {
            Map<String, String> m = new HashMap<String, String>();
            m.put("1", "a");
            m.put("2", "b");
            m.put("3", "c");
            Map<String, String> result = nettyClient.testMap(m);
            Assert.assertEquals(m, result);
        } catch (TException e){
            Assert.fail(e.getMessage());
        }
    }

    /**
     * 测试 nettyIO 同步 使用注解时返回值为Void、struct、返回Null、返回自定义异常时的rpc调用
     */
    @Test
    public void nettySyncOtherTest(){
        try {
            nettyClient.testVoid();
        } catch (Exception e){
            Assert.fail(e.getMessage());
        }

        try {
            String result = nettyClient.testReturnNull();
            Assert.assertEquals(null, result);
        } catch (Exception e){
            Assert.fail(e.getMessage());
        }

        try {
            List<Tweet> tweets = new ArrayList<Tweet>();
            tweets.add(new Tweet(1, "1", "1"));
            tweets.add(new Tweet(2, "2", "2"));
            tweets.add(new Tweet(3, "3", "3"));
            TweetSearchResult tweetSearchResult = new TweetSearchResult(tweets);
            TweetSearchResult result = nettyClient.testStruct("test");
            Assert.assertEquals(tweetSearchResult, result);
        } catch (Exception e){
            Assert.fail(e.getMessage());
        }

        /**
         * 当返回值为bool、int等基本类型时，thrift 0.8 不会抛出自定义异常
         *  todo
         */
        try {
            nettyClient.testException(new Tweet(1, "1", "1"));
            Assert.fail();
        } catch (TwitterUnavailable twitterUnavailable) {
            Assert.assertTrue(twitterUnavailable.getMessage().endsWith("exception"));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
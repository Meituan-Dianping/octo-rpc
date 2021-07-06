package com.meituan.dorado.test.thrift.annotation;

import com.meituan.dorado.common.exception.RemoteException;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.rpc.AsyncContext;
import com.meituan.dorado.rpc.ResponseFuture;
import com.meituan.dorado.serialize.thrift.annotation.AbstractThriftException;
import com.meituan.dorado.test.thrift.annotationTestService.TestRequest;
import com.meituan.dorado.test.thrift.annotationTestService.TestResponse;
import com.meituan.dorado.test.thrift.annotationTestService.TestService;
import org.apache.thrift.TApplicationException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class AnnotationExceptionTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;

    private static TestService nettyClient;

    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/annotationException/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/annotationException/thrift-consumer.xml");

        nettyClient = (TestService) clientBeanFactory.getBean("nettyClientProxy");
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    @Test
    public void testStruct() {
        try {
            TestRequest testRequest = new TestRequest();
            testRequest.setUserid(123);
            testRequest.setName("土豆");
            testRequest.setMessage("你是谁");
            testRequest.setSeqid(1);
            TestResponse testResponse = nettyClient.testStruct(testRequest);

            TestResponse expectResponse = new TestResponse();
            expectResponse.setUserid(testRequest.getUserid());
            expectResponse.setMessage("haha" + testRequest.getMessage());
            expectResponse.setSeqid(testRequest.getSeqid());
            Assert.assertEquals(expectResponse, testResponse);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * 测试 nettyIO 同步 使用注解时 服务端抛出NullPointerException的rpc调用
     */
    @Test
    public void nettySyncNullPointerExceptionTest() {
        try {
            nettyClient.testException();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RemoteException);
            Assert.assertTrue(e.getCause() instanceof TApplicationException);
            Assert.assertTrue(e.getCause().getMessage().contains("NullPointerException"));
        }
    }

    /**
     * 测试 nettyIO 同步 使用注解时 服务端抛出TBaseTypeException的rpc调用
     */
    @Test
    public void nettySyncTBaseTypeExceptionTest() {
        try {
            nettyClient.testBaseTypeException();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof AbstractThriftException);
        }
    }

    /**
     * 测试 nettyIO 同步 使用注解时 抛出TimeoutException的rpc调用
     */
    @Test
    public void nettySyncTimeoutExceptionTest() {
        try {
            nettyClient.testTimeout();
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof TimeoutException);
        }
    }

    /**
     * 测试 nettyIO 异步 使用注解时 服务端抛出NullPointerException的rpc调用
     */
    @Test
    public void nettyAsyncNullPointerExceptionTest() {
        try {
            ResponseFuture<String> future = AsyncContext.getContext().asyncCall(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return nettyClient.testException();
                }
            });
            future.get();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ExecutionException);
            Assert.assertTrue(e.getCause() instanceof RemoteException);
            Assert.assertTrue(e.getCause().getCause().getMessage().contains("NullPointerException"));
        }
    }

    /**
     * 测试 nettyIO 异步 使用注解时 服务端抛出TBaseTypeException的rpc调用
     */
    @Test
    public void nettyAsyncTBaseTypeExceptionTest() {
        try {
            ResponseFuture<Integer> future = AsyncContext.getContext().asyncCall(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return nettyClient.testBaseTypeException();
                }
            });
            future.get();
        } catch (Exception e) {
            Assert.assertTrue(e.getCause() instanceof RemoteException);
            Assert.assertTrue(e.getCause().getCause() instanceof AbstractThriftException);
        }
    }

    /**
     * 测试 nettyIO 异步 使用注解时 抛出TimeoutException的rpc调用
     */
    @Test
    public void nettyAsyncTimeoutExceptionTest() {
        try {
            nettyClient.testTimeout();
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof TimeoutException);
        }
    }
}
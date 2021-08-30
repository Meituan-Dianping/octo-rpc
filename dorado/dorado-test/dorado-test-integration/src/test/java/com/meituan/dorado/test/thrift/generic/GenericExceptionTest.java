package com.meituan.dorado.test.thrift.generic;

import com.meituan.dorado.rpc.GenericService;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Arrays;
import java.util.Collections;

public class GenericExceptionTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;

    private static GenericService client;

    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/genericException/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/genericException/thrift-consumer.xml");

        client = (GenericService) clientBeanFactory.getBean("clientProxy");
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    @Test
    public void testException() {
        try {
            client.$invoke("testA", Collections.EMPTY_LIST, new Object[]{});
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e.getCause() instanceof TApplicationException);
            Assert.assertTrue(e.getCause().getMessage().contains("genericParameterTypes is empty, methodName=testA is not valid")
                    && e.getCause().getMessage().contains("testA"));
        }
    }
}
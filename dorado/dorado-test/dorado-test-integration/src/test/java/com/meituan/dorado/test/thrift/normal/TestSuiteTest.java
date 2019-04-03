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

import com.meituan.dorado.common.exception.ApplicationException;
import com.meituan.dorado.common.exception.RemoteException;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.test.thrift.apisuite.*;
import com.meituan.dorado.test.thrift.filter.ClientQpsLimitFilter;
import com.meituan.dorado.test.thrift.filter.ServerQpsLimitFilter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSuiteTest {
    private static ClassPathXmlApplicationContext clientBean;
    private static ClassPathXmlApplicationContext serverBean;
    private static TestSuite.Iface oriThriftClient;
    private static TestSuite.Iface octoProtocolClient;

    @BeforeClass
    public static void init() {
        serverBean = new ClassPathXmlApplicationContext("thrift/normal/suite/thrift-provider.xml");
        clientBean = new ClassPathXmlApplicationContext("thrift/normal/suite/thrift-consumer.xml");
        oriThriftClient = (TestSuite.Iface) clientBean.getBean("oriThriftClient");
        octoProtocolClient = (TestSuite.Iface) clientBean.getBean("octoProtocolClient");

        ClientQpsLimitFilter.disable();
        ServerQpsLimitFilter.disable();
    }

    @AfterClass
    public static void stop() {
        clientBean.destroy();
        serverBean.destroy();
    }

    @Test
    public void testVoid() {
        try {
            oriThriftClient.testVoid();
            octoProtocolClient.testVoid();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testString() {
        try {
            Assert.assertEquals("Hello", oriThriftClient.testString("Hello"));
            Assert.assertEquals("Hello", octoProtocolClient.testString("Hello"));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testLong() {
        try {
            Assert.assertEquals(Long.MAX_VALUE, oriThriftClient.testLong(Long.MAX_VALUE));
            Assert.assertEquals(Long.MAX_VALUE, octoProtocolClient.testLong(Long.MAX_VALUE));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testMessage() {
        try {
            SubMessage subMessage = new SubMessage();
            subMessage.setId(1);
            subMessage.setValue("subMessage");

            Message message = new Message();
            message.setId(2);
            message.setValue("message");
            message.setSubMessages(Arrays.asList(subMessage));

            Assert.assertEquals(message, oriThriftClient.testMessage(message));
            Assert.assertEquals(message, octoProtocolClient.testMessage(message));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testSubMessage() {
        try {
            SubMessage subMessage = new SubMessage();
            subMessage.setId(1);
            subMessage.setValue("subMessage");

            Assert.assertEquals(subMessage, oriThriftClient.testSubMessage(subMessage));
            Assert.assertEquals(subMessage, octoProtocolClient.testSubMessage(subMessage));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testMessageMap() {
        try {
            SubMessage subMessage = new SubMessage();
            subMessage.setId(1);
            subMessage.setValue("subMessage");

            Message message = new Message();
            message.setId(2);
            message.setValue("message");
            message.setSubMessages(Arrays.asList(subMessage));

            Map<Message, SubMessage> messages = new HashMap<>();
            messages.put(message, subMessage);

            Assert.assertEquals(messages, oriThriftClient.testMessageMap(messages));
            Assert.assertEquals(messages, octoProtocolClient.testMessageMap(messages));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testMessageList() {
        try {
            SubMessage subMessage = new SubMessage();
            subMessage.setId(1);
            subMessage.setValue("subMessage");

            Message message = new Message();
            message.setId(2);
            message.setValue("message");
            message.setSubMessages(Arrays.asList(subMessage));

            List<Message> list = Arrays.asList(message, message);

            Assert.assertEquals(list, oriThriftClient.testMessageList(list));
            Assert.assertEquals(list, octoProtocolClient.testMessageList(list));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testStrMessage() {
        try {
            SubMessage subMessage = new SubMessage();
            subMessage.setId(1);
            subMessage.setValue("subMessage");

            Message message = new Message();
            message.setId(2);
            message.setValue("message");
            message.setSubMessages(Arrays.asList(subMessage));

            Assert.assertEquals(subMessage, oriThriftClient.testStrMessage("dorado", subMessage));
            Assert.assertEquals(subMessage, octoProtocolClient.testStrMessage("dorado", subMessage));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testMultiParam() {
        try {
            Map<String, String> result = new HashMap<>();
            result.put(String.valueOf((1)), String.valueOf(1));
            result.put(String.valueOf(2), String.valueOf(2));
            result.put(String.valueOf(3L), String.valueOf(3L));
            result.put(String.valueOf(4.0), String.valueOf(4.0));
            Assert.assertEquals(result, oriThriftClient.multiParam((byte) 1, 2, 3L, 4.0));
            Assert.assertEquals(result, octoProtocolClient.multiParam((byte) 1, 2, 3L, 4.0));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testProtocolMisMatch() {
        try {
            oriThriftClient.testMockProtocolException();
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals(RemoteException.class, e.getClass());
            Assert.assertEquals(ApplicationException.class, e.getCause().getClass());
        }
        try {
            octoProtocolClient.testMockProtocolException();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(RemoteException.class, e.getClass());
            Assert.assertEquals(ApplicationException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testTimeout() {
        try {
            oriThriftClient.testTimeout();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(TimeoutException.class, e.getClass());
        }
        try {
            octoProtocolClient.testTimeout();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(TimeoutException.class, e.getClass());
        }
    }

    @Test
    public void testReturnNull() {
        try {
            String ret1 = oriThriftClient.testReturnNull();
            Assert.assertEquals(null, ret1);
            String ret2 = octoProtocolClient.testReturnNull();
            Assert.assertEquals(null, ret2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNPE() {
        try {
            String ret1 = oriThriftClient.testNPE();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getCause().getMessage().contains("NullPointerException"));
        }
        try {
            String ret2 = octoProtocolClient.testNPE();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getCause().getMessage().contains("NullPointerException"));
        }
    }

    @Test
    public void testIDLException() {
        try {
            String ret1 = oriThriftClient.testIDLException();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(ExceptionA.class, e.getClass());
            Assert.assertEquals("exceptionA happen", e.getMessage());
        }
        try {
            String ret2 = octoProtocolClient.testIDLException();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(ExceptionA.class, e.getClass());
            Assert.assertEquals("exceptionA happen", e.getMessage());
        }
    }

    @Test
    public void testMultiIDLException() {
        try {
            String ret1 = oriThriftClient.testMultiIDLException();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(ExceptionB.class, e.getClass());
            Assert.assertEquals("exceptionB happen", e.getMessage());
        }
        try {
            String ret2 = octoProtocolClient.testMultiIDLException();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(ExceptionB.class, e.getClass());
            Assert.assertEquals("exceptionB happen", e.getMessage());
        }
    }

    /**
     * 当返回值为bool、int基本类型时，thrift 0.8 不会抛出自定义异常
     */
    @Test
    public void testBaseTypeReturnException() {
        try {
            int ret1 = oriThriftClient.testBaseTypeReturnException();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(ExceptionA.class, e.getClass());
            Assert.assertEquals("exceptionA happen", e.getMessage());
        }
        try {
            int ret2 = octoProtocolClient.testBaseTypeReturnException();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(ExceptionA.class, e.getClass());
            Assert.assertEquals("exceptionA happen", e.getMessage());
        }
    }
    @Test
    public void multiTypeTest() {
        for (int i = 0; i < 1000; i++) {
            try {
                oriThriftClient.testVoid();
                octoProtocolClient.testVoid();
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }

            try {
                Assert.assertEquals("Hello", oriThriftClient.testString("Hello"));
                Assert.assertEquals("Hello", octoProtocolClient.testString("Hello"));
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }

            try {
                Assert.assertEquals(Long.MAX_VALUE, oriThriftClient.testLong(Long.MAX_VALUE));
                Assert.assertEquals(Long.MAX_VALUE, octoProtocolClient.testLong(Long.MAX_VALUE));
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }

            try {
                SubMessage subMessage = new SubMessage();
                subMessage.setId(1);
                subMessage.setValue("subMessage");

                Message message = new Message();
                message.setId(2);
                message.setValue("message");
                message.setSubMessages(Arrays.asList(subMessage));

                Assert.assertEquals(message, oriThriftClient.testMessage(message));
                Assert.assertEquals(message, octoProtocolClient.testMessage(message));
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }

            try {
                SubMessage subMessage = new SubMessage();
                subMessage.setId(1);
                subMessage.setValue("subMessage");

                Message message = new Message();
                message.setId(2);
                message.setValue("message");
                message.setSubMessages(Arrays.asList(subMessage));

                List<Message> list = Arrays.asList(message, message);

                Assert.assertEquals(list, oriThriftClient.testMessageList(list));
                Assert.assertEquals(list, octoProtocolClient.testMessageList(list));
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }

            try {
                Map<String, String> result = new HashMap<>();
                result.put(String.valueOf((1)), String.valueOf(1));
                result.put(String.valueOf(2), String.valueOf(2));
                result.put(String.valueOf(3L), String.valueOf(3L));
                result.put(String.valueOf(4.0), String.valueOf(4.0));
                Assert.assertEquals(result, oriThriftClient.multiParam((byte) 1, 2, 3L, 4.0));
                Assert.assertEquals(result, octoProtocolClient.multiParam((byte) 1, 2, 3L, 4.0));
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }
    }
}

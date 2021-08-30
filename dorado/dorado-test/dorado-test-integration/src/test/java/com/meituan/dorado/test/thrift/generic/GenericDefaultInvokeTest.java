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
package com.meituan.dorado.test.thrift.generic;

import com.meituan.dorado.common.exception.RemoteException;
import com.meituan.dorado.common.util.JacksonUtils;
import com.meituan.dorado.rpc.GenericService;
import org.apache.thrift.TApplicationException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.*;

public class GenericDefaultInvokeTest {

    private static ClassPathXmlApplicationContext clientBeanFactory;
    private static ClassPathXmlApplicationContext serverBeanFactory;

    private static GenericService clientForAnno;
    private static GenericService clientForIDL;

    @BeforeClass
    public static void start() {
        serverBeanFactory = new ClassPathXmlApplicationContext("thrift/generic/thrift-provider.xml");
        clientBeanFactory = new ClassPathXmlApplicationContext("thrift/generic/thrift-consumer.xml");

        clientForAnno = (GenericService) clientBeanFactory.getBean("clientProxyForAnnotation");
        clientForIDL = (GenericService) clientBeanFactory.getBean("clientProxyForIDL");
    }

    @AfterClass
    public static void stop() {
        clientBeanFactory.destroy();
        serverBeanFactory.destroy();
    }

    @Test
    public void testEcho1() {
        List<String> paramTypes = new ArrayList<String>();

        try {
            String result = clientForIDL.$invoke("echo1", paramTypes, new Object[]{});
            System.out.println(result);
            Assert.assertEquals("null", result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEcho2() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.lang.String");
        String paramValues = "hello world";

        try {
            String result = clientForIDL.$invoke("echo2", paramTypes, new Object[]{paramValues});
            System.out.println(result);
            Assert.assertEquals("\"hello world-echo2\"", result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEcho3() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("com.sankuai.mtthrift.testSuite.api.generic.SubMessage");

        SubMessage subMessage = new SubMessage();
        subMessage.setId(1);
        subMessage.setValue("hello world");

        try {
            String expected = JacksonUtils.serializeUnchecked(subMessage);
            String result = clientForIDL.$invoke("echo3", paramTypes, new Object[]{subMessage});
            System.out.println(result);
            Assert.assertEquals(expected, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEcho4() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.util.List");
        List<SubMessage> subMessages = new ArrayList<>();
        SubMessage subMessage = new SubMessage();
        subMessage.setId(2);
        subMessage.setValue("hello world");
        subMessages.add(subMessage);

        try {
            List<String> paramValues = new ArrayList<>();
            String expected = JacksonUtils.serializeUnchecked(subMessages);
            paramValues.add(expected);
            String result = clientForIDL.$invoke("echo4", paramTypes, new Object[]{subMessages});
            System.out.println(result);
            Assert.assertEquals(expected, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testEcho5() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.util.Map");
        Map<SubMessage, SubMessage> maps = new HashMap<>();
        SubMessage key = new SubMessage();
        key.setId(1);
        key.setValue("hello world");
        maps.put(key, key);

        try {
            List<String> paramValues = new ArrayList<>();
            String expected = JacksonUtils.serializeUnchecked(maps);
            paramValues.add(expected);
            String result = clientForIDL.$invoke("echo5", paramTypes, new Object[]{maps});
            System.out.println(result);
            Assert.assertEquals(expected, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testEcho6() {
        List<String> paramTypes = new ArrayList<String>();
        paramTypes.add("com.sankuai.mtthrift.testSuite.api.generic.Message");
        Message message = new Message();
        message.setId(1);
        message.setValue("hello world");
        List<SubMessage> subMessages = new ArrayList<SubMessage>();
        SubMessage subMessage = new SubMessage();
        subMessage.setId(1);
        subMessage.setValue("hello world");
        subMessages.add(subMessage);
        message.setSubMessages(subMessages);

        try {
            List<String> paramValues = new ArrayList<>();
            String expected = JacksonUtils.serializeUnchecked(message);
            paramValues.add(expected);
            String result = clientForIDL.$invoke("echo6", paramTypes, new Object[]{message});
            System.out.println(result);
            Assert.assertEquals(expected, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testEcho7() {
        List<String> paramTypes = new ArrayList<String>();
        paramTypes.add("java.lang.String");
        paramTypes.add("com.sankuai.mtthrift.testSuite.api.generic.SubMessage");

        String message = "hello world";
        SubMessage subMessage = new SubMessage();
        subMessage.setId(1);
        subMessage.setValue("hello world");

        try {
            List<String> paramValues = new ArrayList<>();
            paramValues.add("\"hello world\"");
            String expected = JacksonUtils.serializeUnchecked(subMessage);
            paramValues.add(expected);
            String result = clientForIDL.$invoke("echo7", paramTypes, new Object[]{message, subMessage});
            System.out.println(result);
            Assert.assertEquals(expected, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testEcho8() {
        List<String> paramTypes = new ArrayList<>();
        try {
            String result = clientForIDL.$invoke("echo8", paramTypes, new Object[]{});
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof RemoteException);
            Assert.assertTrue(e.getCause() instanceof TApplicationException);
            Assert.assertTrue(e.getCause().getMessage().equals("GenericException(message:generic error)"));
        }
    }

    @Test
    public void testEcho9() {
        List<String> paramTypes = new ArrayList<String>();
        paramTypes.add("java.lang.Byte");
        paramTypes.add("java.lang.Integer");
        paramTypes.add("java.lang.Long");
        paramTypes.add("java.lang.Double");

        byte param = 2;
        int param2 = 10;
        long param3 = 100L;
        double param4 = 2.01;

        try {
            String result = clientForIDL.$invoke("echo9", paramTypes, new Object[]{param, param2, param3, param4});
            Assert.assertEquals("2", result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEcho10() {
        List<String> paramTypes = new ArrayList<String>();
        paramTypes.add("java.util.List");
        paramTypes.add("java.util.List");
        paramTypes.add("java.util.Set");
        paramTypes.add("java.util.Set");

        List<Long> param = new ArrayList<>();
        param.add(7136387L);
        List<Short> param2 = new ArrayList<>();
        param2.add((short) 4);
        Set<Byte> param3 = new HashSet<>();
        param3.add((byte) 12);
        Set<MessageType> param4 = new HashSet<>();
        param4.add(MessageType.DM);

        try {
            String result = clientForIDL.$invoke("echo10", paramTypes, new Object[]{param, param2, param3, param4});
            Assert.assertEquals("\"echo10\"", result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testList() {
        List<String> paramTypes = new ArrayList<String>();
        paramTypes.add("java.util.List");

        try {
            List<String> paramValues = new ArrayList<>();
            paramValues.add("first");
            paramValues.add("second");
            String expected = JacksonUtils.serializeUnchecked(paramValues);
            String result = clientForAnno.$invoke("testList", paramTypes, new Object[]{paramValues});
            System.out.println(result);
            Assert.assertEquals(expected, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testBool() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.lang.Boolean");
        boolean paramValues = false;

        try {
            String result = clientForAnno.$invoke("testBool", paramTypes, new Object[]{paramValues});
            System.out.println(result);
            Assert.assertEquals(paramValues, Boolean.valueOf(result));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testByte() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.lang.Byte");
        byte paramValues = 3;

        try {
            String result = clientForAnno.$invoke("testByte", paramTypes, new Object[]{paramValues});
            System.out.println(result);
            Assert.assertTrue(paramValues == Byte.valueOf(result));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testI16() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.lang.Short");
        short paramValues = 30;

        try {
            String result = clientForAnno.$invoke("testI16", paramTypes, new Object[]{paramValues});
            System.out.println(result);
            Assert.assertTrue(paramValues == Short.valueOf(result));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testI32() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.lang.Integer");
        int paramValues = 1293;

        try {
            String result = clientForAnno.$invoke("testI32", paramTypes, new Object[]{paramValues});
            System.out.println(result);
            Assert.assertTrue(paramValues == Integer.valueOf(result));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testI64() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.lang.Long");
        long paramValues = Integer.MAX_VALUE + 100L;

        try {
            String result = clientForAnno.$invoke("testI64", paramTypes, new Object[]{paramValues});
            System.out.println(result);
            Assert.assertTrue(paramValues == Long.valueOf(result));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDouble() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.lang.Double");
        double paramValues = 3.4;

        try {
            String result = clientForAnno.$invoke("testDouble", paramTypes, new Object[]{paramValues});
            System.out.println(result);
            Assert.assertTrue(Double.compare(paramValues, Double.valueOf(result)) == 0);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

//    @Test
//    public void testBinary() {
//        List<String> paramTypes = new ArrayList<>();
//        paramTypes.add("java.nio.ByteBuffer");
//        ByteBuffer paramValues = ByteBuffer.wrap("test".getBytes());
//
//        try {
//            String result = clientForAnno.$invoke("testBinary", paramTypes, new Object[]{paramValues});
//            System.out.println(result);
//            Assert.assertEquals(paramValues, ByteBuffer.wrap(result.getBytes()));
//        } catch (Exception e) {
//            Assert.fail(e.getMessage());
//        }
//    }

    @Test
    public void testSet() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.util.Set");

        try {
            Set<String> paramValues = new HashSet<>();
            paramValues.add("first");
            paramValues.add("second");
            String expected = JacksonUtils.serializeUnchecked(paramValues);
            String result = clientForAnno.$invoke("testSet", paramTypes, new Object[]{paramValues});
            System.out.println(result);
            Assert.assertEquals(expected, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testMap() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.util.Map");

        try {
            Map<String, String> paramValues = new HashMap<>();
            paramValues.put("first", "second");
            paramValues.put("second", "third");
            String expected = JacksonUtils.serializeUnchecked(paramValues);
            String result = clientForAnno.$invoke("testMap", paramTypes, new Object[]{paramValues});
            System.out.println(result);
            Assert.assertEquals(expected, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testReturnNull() {
        List<String> paramTypes = new ArrayList<String>();

        try {
            String result = clientForAnno.$invoke("testReturnNull", paramTypes, new Object[]{});
            System.out.println(result);
            Assert.assertEquals("null", result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testParamValuesSerialize() {
        List<String> paramTypes = new ArrayList<>();
        paramTypes.add("java.util.Map");
        List<String> serializeParamValues = new ArrayList<String>();

        try {
            Map<String, String> paramValues = new HashMap<>();
            paramValues.put("first", "second");
            paramValues.put("second", "third");
            String expected = JacksonUtils.serializeUnchecked(paramValues);
            serializeParamValues.add(expected);
            String result = clientForAnno.$invoke("testMap", paramTypes, serializeParamValues);
            System.out.println(result);
            Assert.assertEquals(expected, result);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }
}
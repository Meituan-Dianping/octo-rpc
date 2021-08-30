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

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenericImpl implements Generic.Iface {

    private static final Logger logger = LoggerFactory.getLogger(GenericImpl.class);

    @Override
    public void echo1() throws TException {
        logger.info("echo1");
    }

    @Override
    public String echo2(String message) throws TException {
        logger.info("echo2");
        return message + "-echo2";
    }

    @Override
    public SubMessage echo3(SubMessage message) throws TException {
        logger.info("echo3");
        return message;
    }

    @Override
    public List<SubMessage> echo4(List<SubMessage> messages) throws TException {
        return messages;
    }

    @Override
    public Map<SubMessage, SubMessage> echo5(Map<SubMessage, SubMessage> messages) throws TException {
        logger.info("echo5");
        return messages;
    }

    @Override
    public Message echo6(Message message) throws TException {
        logger.info("echo6");
        return message;
    }

    @Override
    public SubMessage echo7(String strMessage, SubMessage message) throws TException {
        logger.info("string message: {}", strMessage);
        return message;
    }

    @Override
    public void echo8() throws GenericException, TException {
        throw new GenericException("generic error");
    }

    @Override
    public byte echo9(byte param1, int param2, long param3, double param4) throws TException {
        logger.info(param1 + ":" + param2 + ":" + param3 + ":" + param4);
        return param1;
    }

    @Override
    public String echo10(List<Long> param1, List<Short> param2, Set<Byte> param3, Set<MessageType> param4) throws TException {
        logger.info(String.valueOf(param1.get(0)));
        logger.info(String.valueOf(param2.get(0)));
        logger.info(String.valueOf(param4.size()));
        logger.info(String.valueOf(param3.size()));
        return "echo10";
    }
}
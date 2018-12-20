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
package com.meituan.dorado.common.util;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class NetUtilTest {

    @Test
    public void testGetLocalHost() {
        try {
            NetUtil.getLocalHost();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetIpByHost() {
        try {
            NetUtil.getIpByHost("");
        } catch(Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testToIpPort() {
        try {
            String ipPort = NetUtil.toIpPort(new InetSocketAddress(2222));
            Assert.assertTrue("0.0.0.0:2222".equals(ipPort));
        } catch(Exception e) {
            Assert.fail();
        }

    }

    @Test
    public void testToAddress() {
        String address = "1.2.3.4:66";
        try {
            NetUtil.toAddress(address);
        } catch(Exception e) {
            Assert.fail();
        }


        String illegalAddress = ".5:33";
        try {
            NetUtil.toAddress(illegalAddress);
        } catch(Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testIsIpPortStr() {
        String str1 = "10.10:99";
        String str2 = "10:1221";
        String str3 = "10.10.10.1:100";
        String str4 = "10.10.:2000";
        String str5 = "10.10.10.1:100000";
        String str6 = "10.10.10.1:9999";
        String str7 = "10.10.10.1:30000";

        Assert.assertFalse(NetUtil.isIpPortStr(str1));
        Assert.assertFalse(NetUtil.isIpPortStr(str2));
        Assert.assertFalse(NetUtil.isIpPortStr(str3));
        Assert.assertFalse(NetUtil.isIpPortStr(str4));
        Assert.assertFalse(NetUtil.isIpPortStr(str5));
        Assert.assertTrue(NetUtil.isIpPortStr(str6));
        Assert.assertTrue(NetUtil.isIpPortStr(str7));
    }
}

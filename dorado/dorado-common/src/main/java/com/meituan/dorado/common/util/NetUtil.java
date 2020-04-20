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

import com.meituan.dorado.common.Constants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class NetUtil {

    private static final Logger logger = LoggerFactory.getLogger(NetUtil.class);

    public static final String LOCAL_HOST = "127.0.0.1";
    public static final String ANY_HOST = "0.0.0.0";
    public static final int MAX_PORT = 65535;

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    private static final Pattern PORT_PATTERN = Pattern.compile("[0-9]{4,5}");
    private static volatile InetAddress LOCAL_ADDRESS = null;
    private static final List<String> ignoreNiNames;

    static {
        ignoreNiNames = initIgnoreNiNames();
    }

    public static String getLocalHost() {
        InetAddress address = getLocalAddress();
        return address == null ? LOCAL_HOST : address.getHostAddress();
    }

    /**
     * 遍历本地网卡，返回第一个合理的IP。
     *
     * @return 本地网卡IP
     */
    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }
        InetAddress localAddress = getLocalIpV4Adress();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }

    public static InetAddress getLocalIpV4Adress() {
        InetAddress localAddress = null;
        Enumeration<NetworkInterface> networkInterface;
        try {
            networkInterface = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logger.error("Failed to get network interface information.", e);
            return localAddress;
        }
        Set<InetAddress> ips = new HashSet<InetAddress>();
        while (networkInterface.hasMoreElements()) {
            NetworkInterface ni = networkInterface.nextElement();
            // 忽略虚拟网卡的IP,docker 容器的IP
            String niName = (null != ni) ? ni.getName() : "";
            if (isIgnoreNI(niName)) {
                continue;
            }

            Enumeration<InetAddress> inetAddress = null;
            try {
                if (null != ni) {
                    inetAddress = ni.getInetAddresses();
                }
            } catch (Exception e) {
                logger.debug("Failed to get ip information.", e);
            }
            while (null != inetAddress && inetAddress.hasMoreElements()) {
                InetAddress ia = inetAddress.nextElement();
                if (ia instanceof Inet6Address) {
                    continue; // ignore ipv6
                }
                // 排除 回送地址
                if (isValidAddress(ia)) {
                    ips.add(ia);
                    if (!StringUtils.isBlank(ia.getHostAddress())) {
                        localAddress = ia;
                        return localAddress;
                    }
                }
            }
        }
        if (localAddress == null || StringUtils.isBlank(localAddress.getHostAddress())) {
            logger.error("Cannot get local ip.");
        }
        return localAddress;
    }

    /**
     * @param hostName
     * @return ip address or hostName if UnknownHostException
     */
    public static String getIpByHost(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException e) {
            return hostName;
        }
    }

    public static String toIpPort(InetSocketAddress address) {
        if (address == null) {
            return Constants.UNKNOWN;
        }
        return address.getAddress().getHostAddress() + Constants.COLON + address.getPort();
    }

    public static String toIP(InetSocketAddress address) {
        if (address == null || address.getAddress() == null) {
            return Constants.UNKNOWN;
        }
        return address.getAddress().getHostAddress();
    }

    public static int toPort(InetSocketAddress address) {
        if (address == null) {
            return -1;
        }
        return address.getPort();
    }

    public static String toHostPort(InetSocketAddress address) {
        if (address == null) {
            return Constants.UNKNOWN;
        }
        return address.getHostName() + Constants.COLON + address.getPort();
    }

    public static InetSocketAddress toAddress(String address) {
        int i = address.indexOf(':');
        String host;
        int port;
        if (i > -1) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
            port = 0;
        }
        return new InetSocketAddress(host, port);
    }

    public static int getAvailablePort(int defaultPort) {
        int port = defaultPort;
        while (port < MAX_PORT) {
            if (!isPortInUse(port)) {
                return port;
            } else {
                port++;
            }
        }
        while (port > 0) {
            if (!isPortInUse(port)) {
                return port;
            } else {
                port--;
            }
        }
        throw new IllegalStateException("No available port");
    }

    public static boolean isIpPortStr(String str) {
        if (StringUtils.isBlank(str) || !str.contains(Constants.COLON)) {
            return false;
        }
        String[] strs = str.split(Constants.COLON);
        if (strs.length != 2) {
            return false;
        }
        String ip = strs[0];
        if (!IP_PATTERN.matcher(ip).matches()) {
            return false;
        }
        String portStr = strs[1];
        if (!PORT_PATTERN.matcher(portStr).matches()) {
            return false;
        }
        return true;
    }

    public static boolean isPortInUse(int port) {
        try {
            bindPortCheck(ANY_HOST, port);
            bindPortCheck(InetAddress.getLocalHost().getHostAddress(), port);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static void bindPortCheck(String host, int port) throws IOException {
        ServerSocket s = new ServerSocket();
        s.bind(new InetSocketAddress(host, port));
        s.close();
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        return (name != null
                && !ANY_HOST.equals(name)
                && !LOCAL_HOST.equals(name)
                && IP_PATTERN.matcher(name).matches());
    }

    /**
     * ignore:
     * vnic(virtual network interface controller)
     * docker
     * vmnet （vmware）
     * vmbox, vbox (virtual box)
     */
    private static List<String> initIgnoreNiNames() {
        List<String> ret = new ArrayList<>();
        ret.add("vnic");
        ret.add("docker");
        ret.add("vmnet");
        ret.add("vmbox");
        ret.add("vbox");
        return ret;
    }

    private static boolean isIgnoreNI(String niName) {
        for (String item : ignoreNiNames) {
            if (StringUtils.containsIgnoreCase(niName, item)) {
                return true;
            }
        }
        return false;
    }

}

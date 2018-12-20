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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionUtil {

    private static final Logger logger = LoggerFactory.getLogger(VersionUtil.class);

    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String PROPERTIES_NAME = "dorado-application.properties";
    private static final String DORADO_VERSION_KEY = "dorado.version";
    private static final String DORADO_VERSION = "dorado-v" + getVersion(DORADO_VERSION_KEY);

    public static String getDoradoVersion() {
        return DORADO_VERSION;
    }

    private static String getVersion(String cfgName) {
        Properties props = new Properties();
        ClassLoader cl;
        InputStream is = null;

        cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            logger.debug("Trying to find [{}] using class loader {}.", PROPERTIES_NAME, cl);
            is = cl.getResourceAsStream(PROPERTIES_NAME);
        }

        if (is == null) {
            cl = VersionUtil.class.getClassLoader();
            if (cl != null) {
                logger.debug("Trying to find [{}] using class loader {}.", PROPERTIES_NAME, cl);
                is = cl.getResourceAsStream(PROPERTIES_NAME);
            }
        }

        if (is != null) {
            try {
                props.load(is);
            } catch (IOException ignored) {
                logger.debug("Load {} properties failed.", PROPERTIES_NAME, ignored);
            }
        }
        String version = props.getProperty(cfgName, DEFAULT_VERSION);
        return version;
    }
}

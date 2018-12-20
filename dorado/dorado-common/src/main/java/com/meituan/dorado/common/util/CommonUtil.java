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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CommonUtil {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static Map<String, String> parseUrlParams(String paramCfgs) {
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isBlank(paramCfgs)) {
            return params;
        }
        String[] cfgPairs = paramCfgs.split("&");
        for (String cfgPair : cfgPairs) {
            String[] keyVal = cfgPair.split("=");
            if (keyVal.length > 1) {
                params.put(keyVal[0], keyVal[1]);
            }
        }
        return params;
    }

    public static long strToLong(String numStr, long defaultNum) {
        long num;
        if (numStr == null) {
            return defaultNum;
        }
        try {
            num = Long.parseLong(numStr);
        } catch (NumberFormatException e) {
            num = defaultNum;
            logger.debug("String[{}] transform to long failed. Return defaultNum={}", numStr, defaultNum, e);
        }
        return num;
    }

    public static int strToInt(String numStr, int defaultNum) {
        int num;
        if (numStr == null) {
            return defaultNum;
        }
        try {
            num = Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            num = defaultNum;
            logger.debug("String[{}] transform to int failed. Return defaultNum={}", numStr, defaultNum, e);
        }
        return num;
    }

    public static int objectToInt(Object obj, int defaultNum) {
        int num;
        if (obj == null) {
            return defaultNum;
        }
        try {
            num = (int) obj;
        } catch (Exception e) {
            num = defaultNum;
            logger.debug("Object[{}] transform to int failed. Return defaultNum={}", obj.toString(), defaultNum, e);
        }
        return num;
    }

    public static long objectToLong(Object obj, long defaultNum) {
        long num;
        if (obj == null) {
            return defaultNum;
        }
        try {
            num = (long) obj;
        } catch (Exception e) {
            num = defaultNum;
            logger.debug("Object[{}] transform to long failed. Return defaultNum={}", obj.toString(), defaultNum, e);
        }
        return num;
    }

    public static boolean objectToBool(Object obj, boolean defaultVal) {
        boolean val;
        if (obj == null) {
            return defaultVal;
        }
        try {
            val = (boolean) obj;
        } catch (Exception e) {
            val = defaultVal;
            logger.debug("Object[{}] transform to bool failed. Return defaultVal={}", obj.toString(), defaultVal, e);
        }
        return val;
    }

    public static String objectToStr(Object obj, String defaultStr) {
        String str;
        if (obj == null) {
            return defaultStr;
        }
        try {
            str = (String) obj;
        } catch (Exception e) {
            str = defaultStr;
            logger.debug("Object[{}] transform to String failed. Return defaultVal={}", obj.toString(), defaultStr, e);
        }
        return str;
    }

    public static <T> T objectToClazzObj(Object obj, Class<T> clazz) {
        return objectToClazzObj(obj, clazz, false);
    }

    public static <T> T objectToClazzObj(Object obj, Class<T> clazz, boolean returnNewIfNull) {
        T clazzObj = null;
        try {
            clazzObj = (T) obj;
        } catch (Exception e) {
            try {
                if (returnNewIfNull) {
                    clazzObj = clazz.newInstance();
                    logger.debug("Object[{}] transform to {} failed. Build new instance return.", obj.toString(), clazz.getName(), e);
                }
            } catch (InstantiationException | IllegalAccessException e2) {
                clazzObj = null;
                logger.debug("Object[{}] transform to {} failed. Return null.", obj.toString(), clazz.getName(), e);
            }
        }
        return clazzObj;
    }

}

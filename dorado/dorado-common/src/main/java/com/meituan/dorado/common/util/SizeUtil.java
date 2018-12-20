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

public class SizeUtil {

    private static final Logger logger = LoggerFactory.getLogger(SizeUtil.class);
    private static final String SIZE_RANGE_CONFIG = "1,2,4,8,16,32,64,128,256,512,1024";
    private static final int[] SIZE_RANGE_ARRAY = initRangeArray(SIZE_RANGE_CONFIG);
    private static final long MIN_SIZE = 0;
    private static final String UNKNOWN_SIZE = "unknown";

    public static int[] initRangeArray(String rangeConfig) {
        String[] range = rangeConfig.split(",");
        int end = Integer.parseInt(range[range.length - 1]);
        int[] rangeArray = new int[end];
        int rangeIndex = 0;
        for (int i = 0; i < end; i++) {
            if (range.length > rangeIndex) {
                int value = Integer.parseInt(range[rangeIndex]);
                if (i >= value) {
                    rangeIndex++;
                }
                rangeArray[i] = value;
            }
        }
        return rangeArray;
    }

    public static String getLogSize(int size) {
        if (size > MIN_SIZE) {
            try {
                return getLogSize(size, SIZE_RANGE_ARRAY);
            } catch (Exception e) {
                logger.warn("Error while get size range.", e);
            }
        }
        return null;
    }

    public static String getLogSize(int size, int[] rangeArray) {
        if (size > 0 && rangeArray != null && rangeArray.length > 0) {
            String value = ">" + rangeArray[rangeArray.length - 1] + "k";
            int sizeK = (int) Math.ceil(size * 1d / 1024);
            if (rangeArray.length > sizeK) {
                value = "<" + rangeArray[sizeK] + "k";
            }
            return value;
        }
        return UNKNOWN_SIZE;
    }

}
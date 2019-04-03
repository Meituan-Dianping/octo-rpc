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

import java.util.Map;

public class URLUtil {

    private static final int DEFAULT_MAX_PARAMS = 1024;

    public static String getURIPath(String uri) {
        if (StringUtils.isBlank(uri)) {
            return "";
        }
        int pathEndPos = uri.indexOf('?');
        if (pathEndPos < 0) {
            return uri;
        } else {
            return uri.substring(0, pathEndPos);
        }
    }

    public static String getURIPathAndParameter(String uri, Map<String, String> kvParams) {
        String path = getURIPath(uri);

        if (uri.length() == path.length() || kvParams == null) {
            return path;
        }

        decodeParams(uri.substring(path.length() + 1), kvParams);
        return path;
    }

    private static void decodeParams(String str, Map<String, String> kvParams) {
        int paramNum = 0;
        String name = null;
        int pos = 0; // Beginning of the unprocessed region
        int i;       // End of the unprocessed region
        char c;  // Current character
        for (i = 0; i < str.length(); i++) {
            c = str.charAt(i);
            if (c == '=' && name == null) {
                if (pos != i) {
                    name = str.substring(pos, i);
                }
                pos = i + 1;
            } else if (c == '&') {
                if (name == null && pos != i) {
                    // 没有=时赋空值
                    if (!addParam(kvParams, str.substring(pos, i), "", paramNum++)) {
                        return;
                    }
                } else if (name != null) {
                    if (!addParam(kvParams, name, str.substring(pos, i), paramNum++)) {
                        return;
                    }
                    name = null;
                }
                pos = i + 1;
            }
        }

        if (pos != i) {  // Are there characters we haven't dealt with?
            if (name == null) {     // Yes and we haven't seen any `='.
                addParam(kvParams, str.substring(pos, i), "", paramNum++);
            } else {                // Yes and this must be the last value.
                addParam(kvParams, name, str.substring(pos, i), paramNum++);
            }
        } else if (name != null) {  // Have we seen a name without value?
            addParam(kvParams, name, "", paramNum++);
        }
    }

    private static boolean addParam(Map<String, String> params, String name, String value, int paramNum) {
        if (paramNum >= DEFAULT_MAX_PARAMS) {
            return false;
        }
        params.put(name, value);
        return true;
    }
}

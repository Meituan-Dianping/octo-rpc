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

import java.util.*;

public class URLUtil {

    private static final int DEFAULT_MAX_PARAMS = 1024;

    public static Map<String, List<String>> getURIParameter(String uri) {
        if (StringUtils.isBlank(uri)) {
            return Collections.emptyMap();
        }
        String path = getURIPath(uri);

        if (uri.length() == path.length()) {
            return Collections.emptyMap();
        }

        return decodeParams(uri.substring(path.length() + 1));
    }

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

    /**
     * from io.netty QueryStringDecoder
     * @param str
     * @return
     */
    private static Map<String, List<String>> decodeParams(String str) {
        Map<String, List<String>> params = new LinkedHashMap<String, List<String>>();
        int paramNum = 0;
        String name = null;
        int pos = 0; // Beginning of the unprocessed region
        int i;       // End of the unprocessed region
        char c;  // Current character
        for (i = 0; i < str.length(); i++) {
            c = str.charAt(i);
            if (c == '=' && name == null) {
                if (pos != i) {
                    name = decodeComponent(str.substring(pos, i));
                }
                pos = i + 1;
                // http://www.w3.org/TR/html401/appendix/notes.html#h-B.2.2
            } else if (c == '&' || c == ';') {
                if (name == null && pos != i) {
                    // We haven't seen an `=' so far but moved forward.
                    // Must be a param of the form '&a&' so add it with
                    // an empty value.
                    if (!addParam(params, decodeComponent(str.substring(pos, i)), "", paramNum++)) {
                        return params;
                    }
                } else if (name != null) {
                    if (!addParam(params, name, decodeComponent(str.substring(pos, i)), paramNum++)) {
                        return params;
                    }
                    name = null;
                }
                pos = i + 1;
            }
        }

        if (pos != i) {  // Are there characters we haven't dealt with?
            if (name == null) {     // Yes and we haven't seen any `='.
                addParam(params, decodeComponent(str.substring(pos, i)), "", paramNum++);
            } else {                // Yes and this must be the last value.
                addParam(params, name, decodeComponent(str.substring(pos, i)), paramNum++);
            }
        } else if (name != null) {  // Have we seen a name without value?
            addParam(params, name, "", paramNum++);
        }
        return params;
    }

    private static boolean addParam(Map<String, List<String>> params, String name, String value, int paramNum) {
        if (paramNum >= DEFAULT_MAX_PARAMS) {
            return false;
        }

        List<String> values = params.get(name);
        if (values == null) {
            values = new ArrayList<String>(1);  // Often there's only 1 value.
            params.put(name, values);
        }
        values.add(value);
        return true;
    }

    private static String decodeComponent(final String s) {
        if (s == null) {
            return "";
        }
        final int size = s.length();
        boolean modified = false;
        for (int i = 0; i < size; i++) {
            final char c = s.charAt(i);
            if (c == '%' || c == '+') {
                modified = true;
                break;
            }
        }
        if (!modified) {
            return s;
        }
        final byte[] buf = new byte[size];
        int pos = 0;  // position in `buf'.
        for (int i = 0; i < size; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    buf[pos++] = ' ';  // "+" -> " "
                    break;
                case '%':
                    if (i == size - 1) {
                        throw new IllegalArgumentException("unterminated escape"
                                + " sequence at end of string: " + s);
                    }
                    c = s.charAt(++i);
                    if (c == '%') {
                        buf[pos++] = '%';  // "%%" -> "%"
                        break;
                    }
                    if (i == size - 1) {
                        throw new IllegalArgumentException("partial escape"
                                + " sequence at end of string: " + s);
                    }
                    c = decodeHexNibble(c);
                    final char c2 = decodeHexNibble(s.charAt(++i));
                    if (c == Character.MAX_VALUE || c2 == Character.MAX_VALUE) {
                        throw new IllegalArgumentException(
                                "invalid escape sequence `%" + s.charAt(i - 1)
                                        + s.charAt(i) + "' at index " + (i - 2)
                                        + " of: " + s);
                    }
                    c = (char) (c * 16 + c2);
                    // Fall through.
                default:
                    buf[pos++] = (byte) c;
                    break;
            }
        }
        return new String(buf, 0, pos);
    }

    private static char decodeHexNibble(final char c) {
        if ('0' <= c && c <= '9') {
            return (char) (c - '0');
        } else if ('a' <= c && c <= 'f') {
            return (char) (c - 'a' + 10);
        } else if ('A' <= c && c <= 'F') {
            return (char) (c - 'A' + 10);
        } else {
            return Character.MAX_VALUE;
        }
    }
}

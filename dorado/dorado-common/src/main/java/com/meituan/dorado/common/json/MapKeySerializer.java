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
package com.meituan.dorado.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.meituan.dorado.common.util.JacksonUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;

public class MapKeySerializer extends com.fasterxml.jackson.databind.JsonSerializer {

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String keyString = JacksonUtils.serializeUnchecked(value);
        boolean isFinal = Modifier.isFinal(value.getClass().getModifiers());

        StringBuilder key = new StringBuilder();
        if (isFinal) {
            key.append("{\"").append(JacksonUtils.CLASS_KEY).append("\":\"").append(value.getClass().getName()).append("\",");

            if (value instanceof Integer || value instanceof Boolean
                    || value instanceof Byte || value instanceof Short
                    || value instanceof Long || value instanceof Float
                    || value instanceof Double || value instanceof String) {
                key.append(JacksonUtils.BASE_VALUE_KEY).append(":").append(keyString);
            } else if (value instanceof Enum) {
                key.append(JacksonUtils.BASE_VALUE_KEY).append(":").append(keyString);
            } else if (keyString.startsWith("{")) {
                key.append(keyString.substring(1, keyString.length() - 1));
            } else {
                key.append(keyString);
            }

            key.append("}");
            keyString = key.toString();
        }
        gen.writeFieldName(keyString);
    }
}
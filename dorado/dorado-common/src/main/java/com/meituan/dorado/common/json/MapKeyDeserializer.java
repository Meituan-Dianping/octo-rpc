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

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.meituan.dorado.common.util.JacksonUtils;

import java.io.IOException;

public class MapKeyDeserializer extends com.fasterxml.jackson.databind.KeyDeserializer {

    @Override
    public Object deserializeKey(String key, DeserializationContext context) throws IOException {
        JsonNode jsonNode = JacksonUtils.readNode(key);
        JsonNode classNode = jsonNode.get(JacksonUtils.CLASS_KEY);

        Class<?> clz = null;
        try {
            clz = Class.forName(classNode.asText());
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }

        JsonNode valueNode = jsonNode.get(JacksonUtils.BASE_VALUE_NODE_KEY);

        if (clz.equals(Integer.class)) {
            return Integer.parseInt(valueNode.asText());
        } else if (clz.equals(Boolean.class)) {
            return Boolean.parseBoolean(valueNode.asText());
        } else if (clz.equals(Byte.class)) {
            return Byte.parseByte(valueNode.asText());
        } else if (clz.equals(Short.class)) {
            return Short.parseShort(valueNode.asText());
        } else if (clz.equals(Long.class)) {
            return Long.parseLong(valueNode.asText());
        } else if (clz.equals(Float.class)) {
            return Float.parseFloat(valueNode.asText());
        } else if (clz.equals(Double.class)) {
            return Double.parseDouble(valueNode.asText());
        } else if (clz.equals(String.class)) {
            return valueNode.asText();
        } else if (clz.isEnum()) {
            return JacksonUtils.deserializeUnchecked('"' + valueNode.asText() + '"', clz);
        }

        return JacksonUtils.deserializeUnchecked(key, clz);
    }
}
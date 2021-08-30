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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.meituan.dorado.common.json.MapKeyDeserializer;
import com.meituan.dorado.common.json.MapKeyDeserializers;
import com.meituan.dorado.common.json.MapKeySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL;

public class JacksonUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonUtils.class);

    public static final String CLASS_KEY = "@class";
    public static final String BASE_VALUE_KEY = "\"value\"";
    public static final String BASE_VALUE_NODE_KEY = "value";
    private static final SimpleFilterProvider filterProvider = new SimpleFilterProvider();

    private static final ObjectMapper originalMapper = new ObjectMapper();

    private static final ObjectMapper customizedMapper = new ObjectMapper();

    private static final ObjectMapper simpleMapper = new ObjectMapper();

    static {
        try {
            SimpleModule module = new SimpleModule();
            module.setKeyDeserializers(new MapKeyDeserializers());
            module.addKeyDeserializer(Object.class, new MapKeyDeserializer());
            module.addKeySerializer(Object.class, new MapKeySerializer());
            customizedMapper.enableDefaultTypingAsProperty(NON_FINAL, CLASS_KEY);
            customizedMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            customizedMapper.registerModule(module);

            simpleMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            simpleMapper.setVisibility(simpleMapper.getSerializationConfig()
                    .getDefaultVisibilityChecker()
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
            SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter.serializeAllExcept("__isset_bit_vector", "optionals");
            filterProvider.addFilter("myFilter", filter);
            simpleMapper.setFilterProvider(filterProvider);

            originalMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        } catch (Exception e) {
            LOGGER.warn("JacksonUtil init failed", e);
        }
    }

    public static <T> T originalDeserializeUnchecked(String jsonString, JavaType type) throws IOException {
        return originalMapper.readValue(jsonString, type);
    }

    public static String originalSerializeUnchecked(Object result) throws IOException {
        return originalMapper.writeValueAsString(result);
    }

    public static <T> T originalDeserializeUnchecked(String jsonString, Class<T> clazz) throws IOException {
        return originalMapper.readValue(jsonString, clazz);
    }

    public static <T> T deserializeUnchecked(String jsonString, JavaType type) throws IOException {
        return customizedMapper.readValue(jsonString, type);
    }

    public static String serializeUnchecked(Object obj) throws IOException {
        return customizedMapper.writeValueAsString(obj);
    }

    public static <T> T deserializeUnchecked(String jsonString, Class<T> clazz) throws IOException {
        return customizedMapper.readValue(jsonString, clazz);
    }

    public static String simpleSerializeUnchecked(Object obj) throws JsonProcessingException {
        return simpleMapper.writeValueAsString(obj);
    }

    public static <T> T simpleDeserializeUnchecked(String jsonString, Class<T> clazz) throws IOException {
        return simpleMapper.readValue(jsonString, clazz);
    }

    public static JsonNode readNode(String jsonString) {
        try {
            return customizedMapper.readTree(jsonString);
        } catch (Throwable t) {
            LOGGER.error("JacksonUtils readNode error.", t);
        }
        return null;
    }

    public static TypeFactory typeFactory() {
        return customizedMapper.getTypeFactory();
    }

}
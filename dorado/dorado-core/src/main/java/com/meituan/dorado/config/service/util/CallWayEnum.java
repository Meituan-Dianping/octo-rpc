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
package com.meituan.dorado.config.service.util;

public enum CallWayEnum {

    SYNC((byte) 1, "sync"), CALLBACK((byte) 2, "callback"), FUTURE((byte) 3, "future"), ONEWAY((byte) 4, "oneway");

    private byte code;
    private String name;

    CallWayEnum(byte code, String name) {
        this.code = code;
        this.name = name;
    }

    public byte getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static CallWayEnum getCallWay(byte code) {
        switch (code) {
            case 1:
                return SYNC;
            case 2:
                return CALLBACK;
            case 3:
                return FUTURE;
            case 4:
                return ONEWAY;
            default:
                throw new IllegalArgumentException("Invalid callWay code: " + code);
        }
    }

    public static CallWayEnum getCallWay(String name) {
        if (SYNC.getName().equalsIgnoreCase(name)) {
            return SYNC;
        } else if (CALLBACK.getName().equalsIgnoreCase(name)) {
            return CALLBACK;
        } else if (FUTURE.getName().equalsIgnoreCase(name)) {
            return FUTURE;
        } else if (ONEWAY.getName().equalsIgnoreCase(name)) {
            return ONEWAY;
        } else {
            throw new IllegalArgumentException("Invalid callWay name: " + name);
        }
    }
}

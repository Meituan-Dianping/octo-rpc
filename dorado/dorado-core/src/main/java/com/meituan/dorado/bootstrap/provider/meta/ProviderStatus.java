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
package com.meituan.dorado.bootstrap.provider.meta;

public enum ProviderStatus {
    DEAD(0), STARTING(1), ALIVE(2), STOPPING(3), STOPPED(4);

    private int code;

    ProviderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static boolean isNotAliveStatus(int statusCode) {
        switch (statusCode) {
            case 0:
                return true;
            case 1:
                return true;
            case 3:
                return true;
            case 4:
                return true;
            default:
                return false;
        }
    }
}

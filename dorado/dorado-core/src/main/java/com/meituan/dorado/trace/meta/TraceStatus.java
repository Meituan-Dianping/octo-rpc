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
package com.meituan.dorado.trace.meta;

public enum TraceStatus {
    SUCCESS(0),
    EXCEPTION(1),
    TIMEOUT(2),
    DROP(3),
    HTTP_2XX(12),
    HTTP_3XX(13),
    HTTP_4XX(14),
    HTTP_5XX(15);

    private int value;

    private TraceStatus(int i) {
        this.value = i;
    }

    public int getValue() {
        return this.value;
    }
}

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

package com.meituan.dorado.registry.mns.util;

public class MnsUtil {

    public final static String EXTEND_SEPARTOR = ";";
    public final static int MNS_WEIGHT_ACTIVE_DEFAULT = 10;
    public final static int MNS_WEIGHT_INACTIVE_DEFAULT = 0;
    public final static double MNS_FWEIGHT_ACTIVE_DEFAULT = 10.d;
    public final static double MNS_FWEIGHT_INACTIVE_DEFAULT = 0.d;

    /**
     * 注册服务, uptCmd:
     * 0, 重置(代表后面的serviceName list就是该应用支持的全量接口)，
     * 1，增加(代表后面的serviceName list是该应用新增的接口)，
     * 2，减少(代表后面的serviceName list是该应用删除的接口)。
     */
    public final static int UPT_CMD_RESET = 0;
    public final static int UPT_CMD_ADD = 1;
    public final static int UPT_CMD_DEL = 2;

    public enum HeartBeatType {
        NotSupport((byte) 0), P2POnly((byte) 1), ScannerOnly((byte) 2), BothSupport((byte) 3);

        private byte value;

        HeartBeatType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }
}

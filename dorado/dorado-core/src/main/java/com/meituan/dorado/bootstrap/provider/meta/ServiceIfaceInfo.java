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

public class ServiceIfaceInfo {

    private String serviceName;

    private String ifaceName;

    private String implName;

    public ServiceIfaceInfo(String serviceName, String ifaceName, String implName) {
        this.serviceName = serviceName;
        this.ifaceName = ifaceName;
        this.implName = implName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getIfaceName() {
        return ifaceName;
    }

    public String getImplName() {
        return implName;
    }
}

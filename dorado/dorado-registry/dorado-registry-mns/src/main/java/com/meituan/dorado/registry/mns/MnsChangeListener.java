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

package com.meituan.dorado.registry.mns;

import com.meituan.dorado.registry.ProviderListener;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.registry.meta.SubscribeInfo;
import com.meituan.octo.mns.listener.IServiceListChangeListener;
import com.octo.naming.common.thrift.model.ProtocolRequest;
import com.octo.naming.common.thrift.model.SGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MnsChangeListener implements IServiceListChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(MnsChangeListener.class);

    private ProviderListener listener;
    private SubscribeInfo subscribeInfo;

    public MnsChangeListener(ProviderListener listener, SubscribeInfo subscribeInfo) {
        this.listener = listener;
        this.subscribeInfo = subscribeInfo;
    }

    @Override
    public void changed(ProtocolRequest req,
                        List<SGService> oldList,
                        List<SGService> newList,
                        List<SGService> addList,
                        List<SGService> deletedList,
                        List<SGService> modifiedList) {
        List<Provider> providerList = MnsDiscoveryService.genProviderList(newList, subscribeInfo);
        logger.info("Mns find node changed, {} valid nodes(total receive {})", providerList.size(), newList.size());
        listener.notify(providerList);
    }
}

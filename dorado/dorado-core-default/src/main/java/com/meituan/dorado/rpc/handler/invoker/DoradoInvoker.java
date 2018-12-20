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
package com.meituan.dorado.rpc.handler.invoker;

import com.meituan.dorado.codec.octo.meta.CallType;
import com.meituan.dorado.codec.octo.meta.MessageType;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.util.NetUtil;
import com.meituan.dorado.config.service.ClientConfig;
import com.meituan.dorado.config.service.util.CallWayEnum;
import com.meituan.dorado.registry.meta.Provider;
import com.meituan.dorado.serialize.DoradoSerializerFactory;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.util.CompressUtil;

public class DoradoInvoker<T> extends AbstractInvoker<T> {

    public DoradoInvoker(ClientConfig cfg, Provider provider) {
        super(cfg, provider);
    }

    @Override
    public DefaultRequest genRequest() {
        DefaultRequest request = new DefaultRequest();
        request.setAppkey(config.getAppkey());
        request.setRemoteAppkey(config.getRemoteAppkey());
        request.setServiceName(config.getServiceName());
        request.setServiceInterface(config.getServiceInterface());
        request.setTimeout(config.getTimeout());
        request.setMessageType(MessageType.Normal.getValue());
        request.setCallType(getCallTypeOfTransport(config.getCallWay()));
        request.setProtocol(config.getProtocol());
        request.setOctoProtocol(config.isRemoteOctoProtocol() ? true : provider.isOctoProtocol());
        if (Constants.ProtocolType.isThrift(config.getProtocol())) {
            request.setSerialize(DoradoSerializerFactory.SerializeType.THRIFT_CODEC.getCode());
        } else {
            request.setSerialize(DoradoSerializerFactory.SerializeType.getSerializeCode(config.getSerialize()));
        }
        request.setDoChecksum(false);
        request.setCompressType(CompressUtil.CompressType.NO);
        request.setClientIp(NetUtil.getLocalHost());
        request.setClient(client);

        return request;
    }

    private int getCallTypeOfTransport(String name) {
        CallWayEnum callWay = CallWayEnum.getCallWay(name);
        if (CallWayEnum.ONEWAY == callWay) {
            return CallType.NoReply.getValue();
        } else {
            return CallType.Reply.getValue();
        }
    }
}

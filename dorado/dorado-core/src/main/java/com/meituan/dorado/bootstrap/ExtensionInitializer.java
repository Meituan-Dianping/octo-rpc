package com.meituan.dorado.bootstrap;

import com.meituan.dorado.common.RpcRole;
import com.meituan.dorado.common.extension.SPI;
import com.meituan.dorado.config.service.AbstractConfig;

@SPI
public interface ExtensionInitializer {

    void init(AbstractConfig config, RpcRole rpcRole);

}

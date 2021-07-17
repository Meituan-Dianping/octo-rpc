package com.meituan.dorado.config.service;

import java.util.LinkedList;
import java.util.List;

public class ShutdownHook extends Thread {

    private static final List<Disposable> configs = new LinkedList<>();

    static {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    public static synchronized void register(Disposable config) {
        configs.add(config);
    }

    private ShutdownHook() {
        super("DoradoShutdownHook-Thread");
    }
    @Override
    public void run() {
        for (Disposable config : configs) {
            config.destroy();
        }
    }
}

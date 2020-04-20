
## Dorado 优雅关闭

Dorado的优雅关闭通过ShutDownHook方式实现，调用端和服务端通过添加hook进行资源的清理和关闭

```java
protected synchronized void addShutDownHook() {
    ShutdownHook.register(this);
}

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
```
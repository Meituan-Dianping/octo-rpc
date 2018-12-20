
## Dorado 优雅关闭

Dorado的优雅关闭通过ShutDownHook方式实现，调用端和服务端通过添加hook进行资源的清理和关闭

```java
protected synchronized void addShutDownHook() {
    if (hook == null) {
        hook = new ShutDownHook(this);
        Runtime.getRuntime().addShutdownHook(hook);
    }
}

class ShutDownHook extends Thread {
    private ReferenceConfig config;
    
    public ShutDownHook(ReferenceConfig config) {
        this.config = config;
    }
    
    @Override
    public void run() {
        hook = null;
        config.destroy(); 
    }
}
```
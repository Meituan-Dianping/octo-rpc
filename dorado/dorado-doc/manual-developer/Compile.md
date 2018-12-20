
# Dorado 源码编译说明

## 1.下载工程
```
  git clone https://github.com/Meituan-Dianping/octo-rpc.git octo-rpc
```

## 2.构建Jar包
环境要求：
- Java version >= 1.7    
- Maven version >= 3.0    

切换到dorado目录

```
cd octo-rpc/dorado
```

本地install，执行后在本地仓库~/.m2/repository/com/meituan/octo/dorado/（假如你的仓库位置是~/.m2/repository）下可以找到dorado的jar

```
# octo-rpc/dorado 目录下
mvn clean install -Dmaven.test.skip=true
```

或 直接打包，执行后在octo-rpc/dorado/dorado-build/target/ 下可以找到dorado的jar
```
# octo-rpc/dorado 目录下
mvn clean package -Dmaven.test.skip=true
```
**注意：dorado-registry-mns、dorado-trace-cat模块因需要mns和cat的依赖，若构建失败可忽略**

- 默认dorado包未包含需以上模块，若需要使用见看下面的 自定义Jar包

- dorado-registry-mns是OCTO-NS注册中心的集成模块，若使用请到[OCTO-NS](https://github.com/Meituan-Dianping/octo-ns/blob/master/mns-invoker/README.md)获取依赖mns-invoker依赖（需要有MNS注册服务）

- dorado-trace-cat是Cat应用监控的集成，若使用请到[Cat](https://github.com/dianping/cat)获取依赖（需要有Cat服务）


## 3.自定义Jar包

如果需要自定义生成jar包含的模块内容，可以修改dorado-build的pom文件，更新include包含的模块，同时在dorado-build的pom中**增加该模块的依赖**，具体可见dorado-build/pom.xml。

```xml
<build>
    <plugins>
        <plugin>
            <artifactId>maven-shade-plugin</artifactId>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <createSourcesJar>true</createSourcesJar>
                        <promoteTransitiveDependencies>false</promoteTransitiveDependencies>
                        <createDependencyReducedPom>true</createDependencyReducedPom>
                        <artifactSet>
                            <includes>
                                <include>com.meituan.octo:dorado-common</include>
                                <include>com.meituan.octo:dorado-core</include>
                                <include>com.meituan.octo:dorado-protocol-octo</include>
                                <include>com.meituan.octo:dorado-core-default</include>
                                <include>com.meituan.octo:dorado-transport-netty</include>
                                <include>com.meituan.octo:dorado-transport-httpnetty</include>
                                <include>com.meituan.octo:dorado-registry-zookeeper</include>
                                <include>com.meituan.octo:dorado-registry-mock</include>
                                <!-- 需要mns-invoker依赖 -->
                                <!--<include>com.meituan.octo:dorado-registry-mns</include>-->
                                <!-- 需要cat-client依赖 -->
                                <!--<include>com.meituan.octo:dorado-trace-cat</include>-->
                            </includes>
                        </artifactSet>
                        <transformers>
                            <transformer
                                    implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                        </transformers>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
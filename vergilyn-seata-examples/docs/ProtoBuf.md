# ProtoBuf

protobuf(Google Protocol Buffers)
> 在以不同语言编写并在不同平台上运行的应用程序之间交换数据时，Protobuf 编码可提高效率。

个人也没有去大致了解过，只是因为启动seata-server时报错才看到的。参考：
+ [github, protobuf]
- [深入 ProtoBuf - 简介](https://www.jianshu.com/p/a24c88c0526a)

启动seata-server时遇到的问题：
```
E:\Workspace Git\seata-fork\codec\seata-codec-protobuf\src\main\java\io\seata\codec\protobuf\convertor\BranchCommitRequestConvertor.java
Error:(19, 41) java: 程序包io.seata.codec.protobuf.generated不存在
```

## 1 protobuf 本地安装
下载地址：[github, protobuf-release]

<font color="red">特别：通过maven-plugin来编译proto文件，**可能**不需要这么安装protobuf。(ps. 搞懵逼了，i'm five~~)</font>

注意windows下载的是`protoc-3.11.3-win64.zip`，而不是`protobuf-java-3.11.3.zip`（这个需要自己编译）。

下载并解压后，将`bin`目录添加到`环境变量 - 系统变量 - path`。通过cmd验证是否安装成功：
```
PS C:\Users\Administrator> protoc --version
libprotoc 3.11.3
```

## 2. protobuf-maven-plugin
1. idea安装插件`Protobuf Support`（proto语法高亮，mvn编译命令）
- [idea使用Protobuf插件](https://www.cnblogs.com/TechSnail/p/7793813.html)

2. maven-plugin 配置，例如seata源码中的相应 [seata/pom.xml](../../pom.xml)
```XML
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>${protobuf-maven-plugin.version}</version>
    <configuration>
        <protoSourceRoot>${project.basedir}/src/main/resources/protobuf/io/seata/protocol/transcation/</protoSourceRoot>
        <protocArtifact>
            com.google.protobuf:protoc:3.3.0:exe:${os.detected.classifier}
        </protocArtifact>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

3. 手动编译，idea中`Maven - {seata-codec-protobuf 1.0.0} - plugins - protobuf - [protobuf:compile | protobuf:compile-javanano]`。
关于`protobuf:compile`或者`protobuf:compile-javanano`并不清楚其具体的含义。  
大致的表面现象是，最终生成的代码在`target/generated-sources`下目录不一样。

到此，IDEA中查看例如"io.seata.codec.protobuf.convertor.BranchCommitRequestConvertor"不在报错。

## 3 扩展，protobuf生成代码缺少"com.google.protobuf.nano.*"
为了解决这个问题，我在[codec/seata-codec-protobuf/pom.xml](../../codec/seata-codec-protobuf/pom.xml)中增加了其MAVEN依赖：
```xml
<!-- vergilyn-comment, 2020-02-13 >>>> 添加 -->
<dependency>
    <groupId>com.google.protobuf.nano</groupId>
    <artifactId>protobuf-javanano</artifactId>
    <version>3.1.0</version>
</dependency>
```

[github, protobuf]: https://github.com/protocolbuffers/protobuf
[github, protobuf-release]: https://github.com/protocolbuffers/protobuf/releases
# SEATA-conf
[seata参数配置 1.0.0版本](https://seata.io/zh-cn/docs/user/configurations.html)

以SEATA源码中的配置作为参考：
- [ParameterParser.java](../../server/src/main/java/io/seata/server/ParameterParser.java)
- [registry.conf.example](../../server/src/main/resources/registry.conf.example)
- [file.conf.example](../../server/src/main/resources/file.conf.example)

备注：
1. `register.conf`和`file.conf`中的配置项，有些是seata-server独有，有些是seata-client独有，
有些可能是2边都需要配置但用途不一样。

2. 查看配置项的方法，在代码`io.seata.core.constants.ConfigurationKeys`中找到对应配置项进行代码跟踪。
（绝大部分都可以这样，但部分配置可能还有别的常量定义，所以并没有统一常量）

3. seata中的server/client代码共用
比如`AbstractRpcRemoting`中有很多核心方法，都被server/client共用。

4. 1个配置项多个含义
例如`worker-thread-size`  
用途一：
```
// io.seata.core.rpc.netty.TmRpcClient#getInstance()
new java.util.concurrent.ThreadPoolExecutor(
        nettyClientConfig.getClientWorkerThreads(), nettyClientConfig.getClientWorkerThreads(),
        KEEP_ALIVE_TIME, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
        new NamedThreadFactory(nettyClientConfig.getTmDispatchThreadPrefix(),
            nettyClientConfig.getClientWorkerThreads()),
        RejectedPolicies.runsOldestTaskPolicy());
```

用途二：
```
// io.seata.core.rpc.netty.RpcClientBootstrap.start
new io.netty.util.concurrent.DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(),
                new NamedThreadFactory(getThreadPrefix(nettyClientConfig.getClientWorkerThreadPrefix()),
                    nettyClientConfig.getClientWorkerThreads()));
```

2020.02.27 >>>> 建议直接 [seata参数配置 1.0.0版本](https://seata.io/zh-cn/docs/user/configurations.html)  
不再整理了，不知道怎么整理。**有一说一，配置感觉真的蛮乱的，而且很多配置都没给出来，不然配置项可能更多。**

### 1. `register.conf`中的`register.*`在server/client都需要配置。
```
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "nacos"

  nacos {
    serverAddr = "127.0.0.1:8848"
    namespace = "be0813eb-8d8f-46a4-8afd-e5183af5333f"
    # seata-client用不到
    cluster = "default"
  }
}
```

seata-server:  
表明将seata-server注册到哪个注册中心。  
seata源码参考：`io.seata.discovery.registry.nacos.NacosRegistryServiceImpl#register()`

seata-client:  
表示seata-client需要从哪个注册中心，去获取需要的seata-server的信息。  
seata源码参考：`io.seata.discovery.registry.nacos.NacosRegistryServiceImpl#lookup()`  
（通过源码可知，seata-client中暂时用不到 registry.nacos.cluster）

### 2. `seata-spring-boot-stater`
因为这个jar中存在`SpringBootConfigurationProvider implements ExtConfigurationProvider`。
并且spring-boot的默认配置可能跟`*.conf.examples`的默认配置不一样。
并且解析配置顺序等，导致配置项的期望值贼乱！！！

+ [github issues#2265](https://github.com/seata/seata/issues/2265)

### 3. seata启动参数
源码：
- [ParameterParser.java](../../server/src/main/java/io/seata/server/ParameterParser.java)

```CMD
--host, -h              The ip to register to registry center.
--port, -p              The port to listen(default: 8091).
--storeMode, -m         log store mode : file, db
--serverNode, -n        server node id, such as 1, 2, 3. default is 1
--seataEnv, -e          The name used for multi-configuration isolation
```

比较常用到的`port`，用于修改seata-server的端口。

### 4. RegistryProvider
seata-server提供了多种注册方式，其对应的源码参考：[RegistryProvider.java](../../discovery/seata-discovery-core/src/main/java/io/seata/discovery/registry/RegistryProvider.java)

在seata-server启动时，通过`io.seata.core.rpc.netty.AbstractRpcRemotingServer#start() --内部调用--> io.seata.discovery.registry.RegistryService#register(...)`
将seata-server注册到相应的服务注册中心。
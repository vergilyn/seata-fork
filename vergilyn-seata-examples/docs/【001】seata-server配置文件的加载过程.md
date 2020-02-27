# 【001】seata-server配置文件的加载过程

+ [github, seata]
+ [vergilyn seata-fork]
+ [seata.io zh-cn docs]

## 1. 配置文件的读取（register.conf、file.conf）
![register_conf_load](plant-uml/register_conf_load.png)

## 2. 将seata-server注册到服务注册中心（例如eureka、nacos）
![seata_register_to_nacos](plant-uml/seata_register_to_nacos.png)

#### 2.1 备注
1. seata v1.0.0中，通过nacos获取conf并不支持指定GROUP，默认从`SEATA_GROUP`获取（在下一个版本开始支持配置GROUP）。
```JAVA
package io.seata.config.nacos;

public class NacosConfiguration extends AbstractConfiguration {

    private static final String SEATA_GROUP = "SEATA_GROUP";

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        String value;
        if ((value = getConfigFromSysPro(dataId)) != null) {
            return value;
        }
        try {
            value = configService.getConfig(dataId, SEATA_GROUP, timeoutMills);
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
        }
        return value == null ? defaultValue : value;
    }
}
```


2. question: 现在seata支持的nacos的配置是一项一项的（nacos的dataId过多）
+ [SEATA issues#2011](https://github.com/seata/seata/issues/2011)

```
store {
  ## store mode: file、db
  mode = "db"

  ## database store property
  db {
    datasource = "druid"
    db-type = "mysql"
    driver-class-name = "com.mysql.jdbc.Driver"
    url = "jdbc:mysql://127.0.0.1:3306/test_microservice"
    user = "root"
    password = "123456"
  }
}

对应的是7个data-id，而不是一个data-id中的key-value：
1. store.mode
2. store.db.datasource
3. store.db.db-type
4. ...
```

3. seata注册到nacos的服务名默认叫“serverAddr”
+ [SEATA issues#1277](https://github.com/seata/seata/issues/1277)

相关代码参考：[io.seata.discovery.registry.nacos.NacosRegistryServiceImpl#register(...)](/discovery/seata-discovery-nacos/src/main/java/io/seata/discovery/registry/nacos/NacosRegistryServiceImpl.java)
```java
package io.seata.discovery.registry.nacos;
public class NacosRegistryServiceImpl implements RegistryService<EventListener> {
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";

    @Override
    public void register(InetSocketAddress address) throws Exception {
        validAddress(address);
        // vergilyn-question, 2020-02-13 >>>> FIXME，注册到nacos的serviceName始终是“serverAddr”
        getNamingInstance().registerInstance(PRO_SERVER_ADDR_KEY, address.getAddress().getHostAddress(), address.getPort(), getClusterName());
    }
}
```

如果需要修改，不止是需改这一个地方，很多地方都需要修改！
例如`io.seata.discovery.registry.nacos.NacosRegistryServiceImpl#lookup(...)`中也需要修改。

## 3. 总结
1. seata配置的加载
seata配置加载类（factory模式）：`io.seata.config.ConfigurationFactory`  
不同`config.type`对应的加载扩展：`io.seata.config.ConfigurationProvide`

2. `register.conf`
其中只有2个配置：
a)`register.type`，将seata-server注册到什么地方。
b)`confi.type`，seata-server的一些核心配置。例如"store.mode"，seata-server如何记录transaction log。

3. seata-server注册到什么地方
`io.seata.discovery.registry.RegistryFactory` 注册类（factory模式）  
根据从`register.conf`中配置的不同`register.type`，调用相应`io.seata.discovery.registry.RegistryProvider`的实现类。


[seata.io zh-cn docs]: https://seata.io/zh-cn/docs/overview/what-is-seata.html
[github, seata]: https://github.com/seata/seata
[vergilyn seata-fork]: https://github.com/vergilyn/seata-fork


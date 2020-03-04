# vergilyn-seata-examples

+ [github, seata]
+ [vergilyn seata-fork]
+ [seata.io zh-cn docs]

以 [seata.io][seata.io zh-cn docs] 官方文档中提到的示例作为参考测试代码（不完全一样）。  
通过测试代码 debug 调试和阅读 seata源码的实现原理。


2020-02-26 >>>>
实在受不了seata-spring-boot-starter中的`SpringBootConfigurationProvider`。
不想去理解 spring-boot 和 conf配置的转换、解析顺序关系。
现在移除依赖`seata-spring-boot-stater`。

## 1. dependencies

|                      |                             version                              |
|:---------------------|:----------------------------------------------------------------:|
| nacos                |       [1.1.4](https://github.com/alibaba/nacos/releases/)        |
| mysql                |                       mysql-5.7.25-winx64                        |
| spring-boot          |                          2.1.1.RELEASE                           |
| spring-cloud         |                        Greenwich.RELEASE                         |
| spring-cloud-alibaba | [2.1.1.RELEASE](https://github.com/alibaba/spring-cloud-alibaba) |
| seata                |        [1.0.0](https://github.com/seata/seata/tree/1.0.0)        |

| project | server.port |
|:--------|:-----------:|
| NACOS   |    8848     |
| SEATA   |    8091     |
| ACCOUNT |    902X     |
| ORDER   |    903X     |
| STORAGE |    904X     |

特别：
1. 因为 seata-v1.0.0 中依赖的是 nacos-v1.1.4。所以统一用的nacos-v1.1.4。  
如果版本不一致，会出现一些乱七八糟的问题。例如`NacosNamingService.getAllInstances(...)`无法获取到nacos-v1.0.0中的实例。  
（用v1.1.4的API可以注册到nacos-v1.0.0.）

2. 运行SEATA源码需要用到[ProtoBuf](https://github.com/protocolbuffers/protobuf/releases)([protoc-3.11.3-windows-x86_64.exe](https://repo1.maven.org/maven2/com/google/protobuf/protoc/3.11.3/))  
虽然默认是"SEATA"，但可能IDEA中编译不通过，所以个人还是装了ProtoBug保证编译不报错。

## 2. 项目说明

1. nacos只用作服务注册发现中心，其配置中心禁用`spring.cloud.nacos.config.enabled=false`

2. 注意修改nacos中的`namespace`配置参数。

### 2.1 [vergilyn-account-examples](vergilyn-account-examples), [vergilyn-order-examples](vergilyn-order-examples), [vergilyn-storage-examples](vergilyn-storage-examples)
注意修改其中的配置。
- `bootstrap.yaml` 中的`spring.cloud.nacos.discovery.namespace`
- `application.yaml` 中的 datasource连接配置

### 2.2 [vergilyn-common-dependencies](vergilyn-common-dependencies)
1.`com.vergilyn.examples.feign.SeataFeignRequestInterceptor`  
OpenFeign请求时统一设置header: TX_XID={xid}

备注：
> [pring-cloud-feign-overriding-defaults](https://cloud.spring.io/spring-cloud-static/spring-cloud-openfeign/2.2.1.RELEASE/reference/html/#spring-cloud-feign-overriding-defaults)
> If you need to use ThreadLocal bound variables in your RequestInterceptor`s you will need to either
> set the thread isolation strategy for Hystrix to `SEMAPHORE or disable Hystrix in Feign.

所以`feign.hystrix.enabled=false`

2. `com.vergilyn.examples.seata.SeataAutoConfiguration`
获取request-header中的TX_XID，并`RootContext.bind(xid)`  
SEATA的AutoConfiguration。

备注：
注意`file.conf`中的"client.support.spring.datasource.autoproxy = true"，
以免`GlobalTransactionScanner`无法自动代理DataSource。

3. 测试代码 `src/test/java`

### 2.3 [server(seata-server)](../server)
1. `registry.conf`  
a) registry本示例采用`nacos`，注意其namespace与examples-application保持一致；  
b) config采用`file`。

2. `file.conf`  
本示例的seata config采用的是`file`模式。
其中，`store.mode=db`连接mysql数据库。


[seata.io zh-cn docs]: https://seata.io/zh-cn/docs/overview/what-is-seata.html
[github, seata]: https://github.com/seata/seata
[vergilyn seata-fork]: https://github.com/vergilyn/seata-fork

## 3. 备注

### 3.1 `io.seata.common.loader.EnhancedServiceLoader`
觉得这个工具类很有意思。

2020-03-04 >>>>
今天看了一下，原来这个Loader需要在`META-INF/services/`或`META-INF/seata/`中新增文件配合使用。
即告诉Loader，需要`Class.forName(xxx)`，让后

涉及到的新知识：
1. java.lang.Class#cast(...)
Casts an object to the class or interface represented by this Class object.
(将对象强制转换为此类对象表示的类或接口。)

**实际就是类型强转，不过做了一些安全措施。**

```JAVA
package java.lang;

public final class Class<T> implements java.io.Serializable,
                              GenericDeclaration,
                              Type,
                              AnnotatedElement {
    
    public T cast(Object obj) {
        if (obj != null && !isInstance(obj))
            throw new ClassCastException(cannotCastMsg(obj));
        return (T) obj;
    }
    
    public native boolean isInstance(Object obj);
}
```
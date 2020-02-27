package com.vergilyn.examples.seata;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;

/**
 * feign请求时在HEADER中添加参数`TX_XID` <br/>
 *
 * 备注：<a href="https://cloud.spring.io/spring-cloud-static/spring-cloud-openfeign/2.2.1.RELEASE/reference/html/#spring-cloud-feign-overriding-defaults">spring-cloud-feign-overriding-defaults</a>
 * <pre>
 *   If you need to use ThreadLocal bound variables in your RequestInterceptor`s you will need to either
 *   set the thread isolation strategy for Hystrix to `SEMAPHORE or disable Hystrix in Feign.
 * </pre>
 * @author vergilyn
 * @date 2020-02-26
 */
//@Configuration // 加上该注解后不需要`@FeignClient(configuration = SeataFeignRequestInterceptor.class)`
@Slf4j
public class SeataFeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        template.header(RootContext.KEY_XID, RootContext.getXID());
    }
}

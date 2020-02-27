package com.vergilyn.examples.seata;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactionScanner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author vergilyn
 * @date 2020-02-26
 */
@Configuration
@Slf4j
public class SeataAutoConfiguration {
    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean(GlobalTransactionScanner.class)
    public GlobalTransactionScanner globalTransactionScanner() {
        String applicationId = applicationName;
        String txServiceGroup = applicationId + "_tx_group";

        if (log.isInfoEnabled()) {
            log.info("Automatically configure Seata, applicationId = {}, txServiceGroup = {}", applicationId, txServiceGroup);
        }
        return new GlobalTransactionScanner(applicationId, txServiceGroup);
    }

    /**
     * > [SEATA 微服务框架支持](https://seata.io/zh-cn/docs/user/microservice.html) <br/>
     * > 跨服务调用的事务传播 <br/>
     * > 跨服务调用场景下的事务传播，本质上就是要把 XID 通过服务调用传递到服务提供方，并绑定到 RootContext 中去。 <br/>
     */
    @Bean
    public Filter seataFeignFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String xid = request.getHeader(RootContext.KEY_XID);
                boolean isBind = false;

                if (log.isInfoEnabled()){
                    log.info("request header: xid = {}", xid);
                }

                if (StringUtils.isNotBlank(xid)) {
                    RootContext.bind(xid);
                    isBind = true;

                    if (log.isInfoEnabled()){
                        log.info("bind request-header-xid success!");
                    }
                }

                try {
                    filterChain.doFilter(request, response);
                } finally {
                    /* 请求结束后`RootContext.unbind()`。
                     * 如果后续需要用到xid，会从ConnectionProxy中获取，所以可以安心的调用`RootContext.unbind()`
                     */
                    if (isBind) {
                        RootContext.unbind();
                        log.info("unbind request-header-xid success!");
                    }
                }
            }
        };
    }
}

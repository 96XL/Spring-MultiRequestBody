package com.xl.multirequestbody.config;

import com.xl.multirequestbody.resolver.MultiRequestBodyMethodArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 注入 @MultiRequestBody 解析器
 *
 * @author XL
 */
@Configuration
public class WebConfig extends WebMvcConfigurationSupport {

    /**
     * 添加 MultiRequestBody 参数解析器
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new MultiRequestBodyMethodArgumentResolver(super.getMessageConverters()));
    }

    /**
     * 解决中文乱码问题
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
        converters.add(responseBodyConverter());
        converters.add(new MappingJackson2HttpMessageConverter());
    }

    /**
     * 指定编码格式
     */
    @Bean
    public HttpMessageConverter<String> responseBodyConverter() {
        return new StringHttpMessageConverter(Charset.forName("UTF-8"));
    }
}
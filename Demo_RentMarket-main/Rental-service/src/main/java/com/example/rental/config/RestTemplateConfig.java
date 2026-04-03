package com.example.rental.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Cau hinh RestTemplate voi timeout de tranh block thread vo thoi han
 * khi goi Product-service, Identity-service, MoMo Gateway.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${app.http.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${app.http.read-timeout:10000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}

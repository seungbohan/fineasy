package com.fineasy.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DartApiProperties.class)
@ConditionalOnExpression("!'${dart.api.key:}'.isEmpty()")
public class DartApiConfig {
}

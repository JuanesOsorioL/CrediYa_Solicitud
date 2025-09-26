package co.com.crediya_solicitud.sqs.sender.config.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs")
public record SqsCommonProperties(String region, String endpoint) {}

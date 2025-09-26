package co.com.crediya_solicitud.sqs.sender.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs.notifier")
public record NotifierProperties(String queueUrl) {}

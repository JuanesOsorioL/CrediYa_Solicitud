package co.com.crediya_solicitud.sqs.sender.config;

import co.com.crediya_solicitud.sqs.sender.config.common.SqsCommonProperties;
import co.com.crediya_solicitud.sqs.sender.config.properties.CallbackProperties;
import co.com.crediya_solicitud.sqs.sender.config.properties.NotifierProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
@EnableConfigurationProperties({
        SqsCommonProperties.class, NotifierProperties.class, CallbackProperties.class
})
@ConditionalOnMissingBean(SqsAsyncClient.class)
public class SQSSenderConfig {


//    @Bean
//    public MetricPublisher metricPublisher() {
//        return NoOpMetricPublisher.create();
//    }

    @Bean
    public SqsAsyncClient configSqs(SqsCommonProperties common, MetricPublisher publisher) {
        return SqsAsyncClient.builder()
                .region(Region.of(common.region()))
                .credentialsProvider(getProviderChain())
                .overrideConfiguration(o -> o.addMetricPublisher(publisher))
                .endpointOverride(resolveEndpoint(common))
                .build();
    }

    private AwsCredentialsProviderChain getProviderChain() {
        return AwsCredentialsProviderChain.builder()
                .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .addCredentialsProvider(SystemPropertyCredentialsProvider.create())
                .addCredentialsProvider(WebIdentityTokenFileCredentialsProvider.create())
                .addCredentialsProvider(ProfileCredentialsProvider.create())
                .addCredentialsProvider(ContainerCredentialsProvider.builder().build())
                .addCredentialsProvider(InstanceProfileCredentialsProvider.create())
                .build();
    }

    private URI resolveEndpoint(SqsCommonProperties common) {
        if (common.endpoint() != null) {
            return URI.create(common.endpoint());
        }
        return null;
    }
}

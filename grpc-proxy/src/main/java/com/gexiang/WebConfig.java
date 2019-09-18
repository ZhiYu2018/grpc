package com.gexiang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {
    private static Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Override
    public void configurePathMatching(PathMatchConfigurer configurer) {
        configurer.setUseCaseSensitiveMatch(true)
                .setUseTrailingSlashMatch(false)
                .addPathPrefix("/grpc", HandlerTypePredicate.forAnnotation(RestController.class));
    }
}

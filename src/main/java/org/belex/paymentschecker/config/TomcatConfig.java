package org.belex.paymentschecker.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @org.springframework.context.annotation.Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> nio2Protocol() {
        return factory -> factory.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
    }
}

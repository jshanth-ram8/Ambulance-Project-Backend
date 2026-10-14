package com.ambulance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Lets the separately hosted frontend call this API from the browser.
 *
 * The allowed origin(s) come from the FRONTEND_URL environment variable
 * (app.frontend-url). Use a comma-separated list to allow more than one.
 *
 * Credentials are allowed because login uses a session cookie, so a wildcard
 * origin is not possible here (Spring refuses to start with "*").
 *
 * This is a servlet filter, not an MVC interceptor, on purpose: it answers the
 * browser's OPTIONS preflight before AuthInterceptor can reject it with a 401.
 */
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(@Value("${app.frontend-url}") String frontendUrl) {
        List<String> origins = Arrays.stream(frontendUrl.split(","))
                .map(String::trim)
                .map(origin -> origin.replaceAll("/+$", ""))   // browsers send the Origin without a trailing slash
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}

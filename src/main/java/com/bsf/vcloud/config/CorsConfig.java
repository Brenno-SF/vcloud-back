package com.bsf.vcloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
    //quem pode fazer chamadas para minha api
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOriginPattern("*"); // permite todas as origens
        corsConfig.addAllowedOrigin("*"); // TODO: ajustar para o dominio da aplicacao front-end
        corsConfig.addAllowedHeader("*"); // permite todos os headers
        corsConfig.addAllowedMethod("*"); // permite todos os metodos (GET, POST, PUT, DELETE, etc)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }
}

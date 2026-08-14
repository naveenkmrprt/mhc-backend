package com.mhc.dashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${SPRING_DATASOURCE_URL:${DATABASE_URL:}}")
    private String dbUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:${DATABASE_USERNAME:mhcuser}}")
    private String username;

    @Value("${SPRING_DATASOURCE_PASSWORD:${DATABASE_PASSWORD:password}}")
    private String password;

    @Bean
    public DataSource dataSource() {
        String finalUrl = dbUrl;
        if (finalUrl != null) {
            try {
                // Remove jdbc: prefix if present so java.net.URI can parse it
                String cleanUri = finalUrl.replace("jdbc:", "");
                java.net.URI uri = new java.net.URI(cleanUri);
                String host = uri.getHost();
                int port = uri.getPort();
                String path = uri.getPath();
                
                finalUrl = "jdbc:postgresql://" + host + (port != -1 ? ":" + port : "") + path;
            } catch (Exception e) {
                // Fallback if parsing fails
                if (!finalUrl.startsWith("jdbc:")) {
                    finalUrl = finalUrl.replace("postgres://", "jdbc:postgresql://");
                    if (!finalUrl.startsWith("jdbc:")) {
                        finalUrl = "jdbc:" + finalUrl;
                    }
                }
            }
        }
        return DataSourceBuilder.create()
                .url(finalUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}

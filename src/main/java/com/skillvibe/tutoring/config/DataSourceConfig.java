package com.skillvibe.tutoring.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Configuración del DataSource que convierte automáticamente el formato de URL
 * de Railway (postgres://user:pass@host:port/db) al formato JDBC necesario.
 */
@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:#{null}}")
    private String rawDatabaseUrl;

    @Value("${spring.datasource.username:sa}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Bean
    @Primary
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");

        if (rawDatabaseUrl != null && rawDatabaseUrl.contains("@")) {
            try {
                // Remove jdbc: prefix if it was added manually
                String cleanUrl = rawDatabaseUrl.replaceFirst("^jdbc:", "").trim();
                URI dbUri = new URI(cleanUrl);

                String userInfo = dbUri.getUserInfo();
                if (userInfo != null) {
                    String[] credentials = userInfo.split(":", 2);
                    if (credentials.length == 2) {
                        String urlUser = credentials[0].trim();
                        String urlPass = credentials[1].trim();
                        
                        // Use injected properties if they are explicitly set, otherwise use URL credentials
                        config.setUsername(!"sa".equals(username) && username != null && !username.trim().isEmpty() ? username.trim() : urlUser);
                        config.setPassword(password != null && !password.trim().isEmpty() ? password.trim() : urlPass);
                    }
                }

                String portPart = (dbUri.getPort() != -1) ? (":" + dbUri.getPort()) : "";
                String queryPart = (dbUri.getQuery() != null) ? ("?" + dbUri.getQuery()) : "";
                String jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + portPart + dbUri.getPath() + queryPart;
                
                config.setJdbcUrl(jdbcUrl);
            } catch (URISyntaxException e) {
                config.setJdbcUrl(rawDatabaseUrl.trim());
            }
        } else {
            String jdbcUrl = rawDatabaseUrl != null ? rawDatabaseUrl.trim() : null;
            if (jdbcUrl != null && !jdbcUrl.startsWith("jdbc:")) {
                jdbcUrl = jdbcUrl.replaceFirst("^postgres(?:ql)?://", "jdbc:postgresql://");
            }
            config.setJdbcUrl(jdbcUrl);
            if (username != null) config.setUsername(username.trim());
            if (password != null) config.setPassword(password.trim());
        }

        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}

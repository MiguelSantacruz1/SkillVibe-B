package com.skillvibe.tutoring.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configuración del DataSource que convierte automáticamente el formato de URL
 * de Railway (postgres://user:pass@host:port/db) al formato JDBC necesario
 * (jdbc:postgresql://host:port/db).
 *
 * Railway inyecta DATABASE_URL en el formato estándar de postgres://,
 * pero Spring Boot/Hibernate necesita el formato jdbc:postgresql://.
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
        String jdbcUrl = convertToJdbcUrl(rawDatabaseUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.postgresql.Driver");

        // Si la URL tiene credenciales embebidas (formato postgres://user:pass@host),
        // no se necesita username/password por separado
        if (!rawDatabaseUrl.contains("@")) {
            config.setUsername(username);
            config.setPassword(password);
        }

        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    /**
     * Convierte cualquier formato de URL de PostgreSQL al formato JDBC.
     *
     * Ejemplos:
     *   postgres://user:pass@host:5432/db   →  jdbc:postgresql://host:5432/db
     *   postgresql://user:pass@host:5432/db →  jdbc:postgresql://host:5432/db
     *   jdbc:postgresql://host:5432/db      →  jdbc:postgresql://host:5432/db (sin cambio)
     */
    private String convertToJdbcUrl(String url) {
        if (url == null) return null;

        // Ya está en formato JDBC — no hacer nada
        if (url.startsWith("jdbc:")) {
            return url;
        }

        // Quitar credenciales embebidas: postgres://user:pass@host → jdbc:postgresql://host
        String result = url;

        // Reemplazar el esquema
        result = result.replace("postgres://", "jdbc:postgresql://");
        result = result.replace("postgresql://", "jdbc:postgresql://");

        // Quitar las credenciales embebidas del URL (user:pass@)
        // jdbc:postgresql://user:pass@host:port/db → jdbc:postgresql://host:port/db
        if (result.contains("@")) {
            int atIndex = result.indexOf('@');
            String prefix = "jdbc:postgresql://";
            String afterAt = result.substring(atIndex + 1);
            result = prefix + afterAt;
        }

        return result;
    }
}

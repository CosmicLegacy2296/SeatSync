package com.office.booking.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts a platform-style DATABASE_URL (postgresql://user:pass@host/db?params)
 * into JDBC_DATABASE_URL / JDBC_DATABASE_DRIVER / JDBC_DATABASE_USERNAME /
 * JDBC_DATABASE_PASSWORD before Spring datasource auto-configuration reads them.
 *
 * This runs whether the app is launched via mvn-run.js or directly as a jar
 * (e.g. inside Docker on Render).
 */
public class DatabaseUrlNormalizer implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {

        String raw = environment.getProperty("DATABASE_URL");
        if (raw == null || raw.isBlank()) {
            return;
        }

        // Already a JDBC URL — nothing to do.
        if (raw.startsWith("jdbc:")) {
            return;
        }

        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) {
            return;
        }

        // Normalise scheme so java.net.URI can parse it.
        String uriString = raw.startsWith("postgresql://")
                ? raw.replaceFirst("postgresql://", "postgres://")
                : raw;

        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            return;
        }

        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath(); // e.g. /neondb
        String query = uri.getRawQuery(); // e.g. sslmode=require&channel_binding=require

        String jdbcBase = port > 0
                ? "jdbc:postgresql://" + host + ":" + port + path
                : "jdbc:postgresql://" + host + path;

        String jdbcUrl = (query != null && !query.isBlank())
                ? jdbcBase + "?" + query
                : jdbcBase;

        Map<String, Object> props = new HashMap<>();

        // Only set if not already overridden by an explicit env var.
        setIfAbsent(environment, props, "JDBC_DATABASE_URL", jdbcUrl);
        setIfAbsent(environment, props, "JDBC_DATABASE_DRIVER", "org.postgresql.Driver");

        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            int sep = userInfo.indexOf(':');
            if (sep >= 0) {
                setIfAbsent(environment, props, "JDBC_DATABASE_USERNAME", userInfo.substring(0, sep));
                setIfAbsent(environment, props, "JDBC_DATABASE_PASSWORD", userInfo.substring(sep + 1));
            } else {
                setIfAbsent(environment, props, "JDBC_DATABASE_USERNAME", userInfo);
            }
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource("normalizedDatabaseUrl", props));
        }
    }

    private void setIfAbsent(ConfigurableEnvironment env, Map<String, Object> props,
                              String key, String value) {
        if (env.getProperty(key) == null) {
            props.put(key, value);
        }
    }
}

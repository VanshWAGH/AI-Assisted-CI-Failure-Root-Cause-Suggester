package com.rootcause.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts a standard PostgreSQL connection URL (postgresql:// or postgres://)
 * — as provided by Neon, Heroku, Railway, etc. — into the properties
 * that Spring Boot's HikariCP datasource expects:
 *
 *   spring.datasource.url      → jdbc:postgresql://host:port/dbname?query
 *   spring.datasource.username → extracted from userInfo
 *   spring.datasource.password → extracted from userInfo
 *
 * This runs before Spring's datasource auto-configuration, so users can paste
 * their raw Neon connection string into DATABASE_URL without any manual conversion.
 *
 * Example input:
 *   postgresql://neondb_owner:password@ep-xxx.neon.tech/neondb?sslmode=require
 *
 * Example output properties set automatically:
 *   spring.datasource.url      = jdbc:postgresql://ep-xxx.neon.tech/neondb?sslmode=require
 *   spring.datasource.username = neondb_owner
 *   spring.datasource.password = password
 */
public class DatabaseUrlPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROP_SOURCE_NAME = "neonDatabaseUrlPostProcessor";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        String raw = environment.getProperty("DATABASE_URL");
        if (raw == null || raw.isBlank()) {
            return;  // not set — local dev or docker-compose handles datasource directly
        }

        // Only convert postgresql:// / postgres:// formats.
        // If already in jdbc: format, skip (user knows what they're doing).
        if (!raw.startsWith("postgresql://") && !raw.startsWith("postgres://")) {
            return;
        }

        try {
            // Parse: replace scheme with http:// so java.net.URI can extract host/userInfo
            String parseable = raw.replaceFirst("^postgres(?:ql)?://", "http://");
            URI uri = new URI(parseable);

            String host     = uri.getHost();
            int    port     = uri.getPort() > 0 ? uri.getPort() : 5432;
            String dbPath   = uri.getPath();           // e.g. "/neondb"
            String rawQuery = uri.getRawQuery();        // e.g. "sslmode=require&channel_binding=require"

            // Build JDBC URL (credentials go into separate properties, not in URL)
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d%s%s",
                    host,
                    port,
                    dbPath,
                    rawQuery != null && !rawQuery.isBlank() ? "?" + rawQuery : "");

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbcUrl);

            // Extract user:password from userInfo segment
            String userInfo = uri.getUserInfo();  // "neondb_owner:password"
            if (userInfo != null && !userInfo.isBlank()) {
                int colon = userInfo.indexOf(':');
                if (colon > 0) {
                    props.put("spring.datasource.username", userInfo.substring(0, colon));
                    props.put("spring.datasource.password", userInfo.substring(colon + 1));
                } else {
                    props.put("spring.datasource.username", userInfo);
                }
            }

            // Add with highest priority so it overrides yaml defaults
            environment.getPropertySources()
                    .addFirst(new MapPropertySource(PROP_SOURCE_NAME, props));

            System.out.println("[DatabaseUrlPostProcessor] Converted DATABASE_URL → JDBC datasource properties");

        } catch (URISyntaxException e) {
            // Print a clear error — Spring will fail with a useful message during startup
            System.err.println("[DatabaseUrlPostProcessor] ERROR: Could not parse DATABASE_URL: " + e.getMessage());
            System.err.println("  Expected format: postgresql://user:password@host/dbname?sslmode=require");
        }
    }

    @Override
    public int getOrder() {
        // Run early, before Spring's datasource auto-configuration reads the properties
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}

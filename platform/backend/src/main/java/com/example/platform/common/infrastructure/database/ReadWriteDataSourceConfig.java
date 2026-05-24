package com.example.platform.common.infrastructure.database;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class ReadWriteDataSourceConfig {

    private final Environment environment;
    private final TenantSchemaNameResolver tenantSchemaNameResolver;

    public ReadWriteDataSourceConfig(Environment environment, TenantSchemaNameResolver tenantSchemaNameResolver) {
        this.environment = environment;
        this.tenantSchemaNameResolver = tenantSchemaNameResolver;
    }

    @Bean
    @Primary
    public DataSource dataSource(MeterRegistry meterRegistry) {
        DataSourceProperties defaultProperties = bindOrDefault("spring.datasource");
        DataSourceProperties masterProperties = bindOrDefault("spring.datasource.master");
        DataSourceProperties replicaProperties = bindOrDefault("spring.datasource.replica");

        HikariDataSource writeDataSource = buildDataSource(masterProperties, defaultProperties, "spring.datasource.master.hikari");
        HikariDataSource readDataSource = buildReadDataSource(replicaProperties, writeDataSource);
        bindHikariMetrics(writeDataSource, "platform-write", meterRegistry);
        if (readDataSource != writeDataSource) {
            bindHikariMetrics(readDataSource, "platform-read", meterRegistry);
        }

        TransactionRoutingDataSource routingDataSource = new TransactionRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put(TransactionRoutingDataSource.WRITE, writeDataSource);
        targets.put(TransactionRoutingDataSource.READ, readDataSource);

        routingDataSource.setTargetDataSources(targets);
        routingDataSource.setDefaultTargetDataSource(writeDataSource);
        routingDataSource.afterPropertiesSet();

        if (isTenantSchemaMode()) {
            return new TenantSchemaRoutingDataSource(
                    routingDataSource,
                    tenantSchemaNameResolver
            );
        }
        return routingDataSource;
    }

    private boolean isTenantSchemaMode() {
        return "tenant-schema".equalsIgnoreCase(environment.getProperty("platform.tenancy.mode", "shared-schema"));
    }

    private DataSourceProperties bindOrDefault(String prefix) {
        return Binder.get(environment)
                .bind(prefix, Bindable.of(DataSourceProperties.class))
                .orElseGet(DataSourceProperties::new);
    }

    private HikariDataSource buildDataSource(
            DataSourceProperties preferred,
            DataSourceProperties fallback,
            String hikariPrefix
    ) {
        DataSourceProperties effective = StringUtils.hasText(preferred.getUrl()) ? preferred : fallback;
        HikariDataSource dataSource = effective.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        Binder.get(environment).bind(hikariPrefix, Bindable.ofInstance(dataSource));
        return dataSource;
    }

    private HikariDataSource buildReadDataSource(DataSourceProperties replicaProperties, HikariDataSource writeDataSource) {
        if (!StringUtils.hasText(replicaProperties.getUrl())) {
            return writeDataSource;
        }

        HikariDataSource readDataSource = replicaProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        Binder.get(environment).bind("spring.datasource.replica.hikari", Bindable.ofInstance(readDataSource));
        readDataSource.setReadOnly(true);
        return readDataSource;
    }

    private void bindHikariMetrics(HikariDataSource dataSource, String fallbackPoolName, MeterRegistry meterRegistry) {
        if (!StringUtils.hasText(dataSource.getPoolName())) {
            dataSource.setPoolName(fallbackPoolName);
        }
        dataSource.setMetricRegistry(meterRegistry);
    }
}

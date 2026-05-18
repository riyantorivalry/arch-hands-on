package com.example.platform.common.infrastructure.database;

import com.zaxxer.hikari.HikariDataSource;
import com.example.platform.common.infrastructure.observability.DatabaseQueryMetricsListener;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
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

    public ReadWriteDataSourceConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Primary
    public DataSource dataSource(MeterRegistry meterRegistry) {
        DataSourceProperties defaultProperties = bindOrDefault("spring.datasource");
        DataSourceProperties masterProperties = bindOrDefault("spring.datasource.master");
        DataSourceProperties replicaProperties = bindOrDefault("spring.datasource.replica");
        long slowQueryThresholdMs = environment.getProperty(
                "platform.observability.db.slow-query-threshold-ms",
                Long.class,
                250L
        );
        DatabaseQueryMetricsListener queryMetricsListener =
                new DatabaseQueryMetricsListener(meterRegistry, slowQueryThresholdMs);

        HikariDataSource writeDataSource = buildDataSource(masterProperties, defaultProperties, "spring.datasource.master.hikari");
        HikariDataSource readDataSource = buildReadDataSource(replicaProperties, writeDataSource);
        bindHikariMetrics(writeDataSource, "platform-write", meterRegistry);
        if (readDataSource != writeDataSource) {
            bindHikariMetrics(readDataSource, "platform-read", meterRegistry);
        }
        DataSource observedWriteDataSource = observeDataSource(writeDataSource, "platform-write", queryMetricsListener);
        DataSource observedReadDataSource = readDataSource == writeDataSource
                ? observedWriteDataSource
                : observeDataSource(readDataSource, "platform-read", queryMetricsListener);

        TransactionRoutingDataSource routingDataSource = new TransactionRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put(TransactionRoutingDataSource.WRITE, observedWriteDataSource);
        targets.put(TransactionRoutingDataSource.READ, observedReadDataSource);

        routingDataSource.setTargetDataSources(targets);
        routingDataSource.setDefaultTargetDataSource(observedWriteDataSource);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
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

    private DataSource observeDataSource(
            DataSource dataSource,
            String datasourceName,
            DatabaseQueryMetricsListener queryMetricsListener
    ) {
        return ProxyDataSourceBuilder.create(dataSource)
                .name(datasourceName)
                .listener(queryMetricsListener)
                .build();
    }
}

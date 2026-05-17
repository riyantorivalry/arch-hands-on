package com.example.platform.common.infrastructure.database;

import com.example.platform.common.web.RequestContext;
import com.example.platform.common.web.RequestContextHolder;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.AbstractDataSource;

public class TenantSchemaRoutingDataSource extends AbstractDataSource {

    private final DataSource delegate;
    private final TenantSchemaNameResolver schemaNameResolver;
    private final String defaultSchema;

    public TenantSchemaRoutingDataSource(
            DataSource delegate,
            TenantSchemaNameResolver schemaNameResolver,
            String defaultSchema
    ) {
        this.delegate = delegate;
        this.schemaNameResolver = schemaNameResolver;
        this.defaultSchema = defaultSchema;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = delegate.getConnection();
        connection.setSchema(currentSchema());
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = delegate.getConnection(username, password);
        connection.setSchema(currentSchema());
        return connection;
    }

    private String currentSchema() {
        return RequestContextHolder.get()
                .map(RequestContext::tenantId)
                .filter(tenantId -> !tenantId.isBlank())
                .map(schemaNameResolver::schemaForTenant)
                .orElse(defaultSchema);
    }
}

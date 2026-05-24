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

    public TenantSchemaRoutingDataSource(
            DataSource delegate,
            TenantSchemaNameResolver schemaNameResolver
    ) {
        this.delegate = delegate;
        this.schemaNameResolver = schemaNameResolver;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = delegate.getConnection();
        return applyCurrentTenantSchema(connection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = delegate.getConnection(username, password);
        return applyCurrentTenantSchema(connection);
    }

    private Connection applyCurrentTenantSchema(Connection connection) throws SQLException {
        String schema = currentTenantSchema();
        if (schema != null) {
            connection.setSchema(schema);
        }
        return connection;
    }

    private String currentTenantSchema() {
        return RequestContextHolder.get()
                .map(RequestContext::tenantId)
                .filter(tenantId -> !tenantId.isBlank())
                .map(schemaNameResolver::schemaForTenant)
                .orElse(null);
    }
}

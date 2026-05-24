create table tenant_schema_registry (
    tenant_id varchar(64) primary key,
    schema_name varchar(63) not null unique,
    model varchar(32) not null,
    provisioned_at timestamp with time zone not null,
    constraint fk_tenant_schema_registry_tenant foreign key (tenant_id) references tenants (tenant_id)
);

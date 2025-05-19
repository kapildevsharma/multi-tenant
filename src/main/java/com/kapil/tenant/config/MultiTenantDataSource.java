package com.kapil.tenant.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

// AbstractRoutingDataSource that switches schema using tenant context
public class MultiTenantDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenant();
    }
}

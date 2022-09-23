/*
 * Copyright 2022-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.aps.system.services.tenant;

import com.agiletec.aps.system.common.AbstractService;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.entando.entando.aps.system.services.tenant.cache.ITenantManagerCacheWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author E.Santoboni
 */
public class TenantManager extends AbstractService implements ITenantManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantManager.class);
    
    private Map<String, DataSource> dataSources = new HashMap<>();
    
    private ITenantManagerCacheWrapper cacheWrapper;

    @Override
    public void init() throws Exception {
        try {
            this.getCacheWrapper().initCache();
        } catch (Exception e) {
            logger.error("Error extracting tenant configs", e);
        }
    }
    
    @Override
    protected void release() {
        super.release();
        try {
            Iterator<DataSource> iter = this.getDataSources().values().iterator();
            while (iter.hasNext()) {
                DataSource datasource = iter.next();
                this.destroyDataSource(datasource);
            }
            this.getCacheWrapper().initCache();
        } catch (Exception e) {
            logger.error("Error closing connection", e);
        }
        this.getDataSources().clear();
    }
    
    public void destroyDataSource(DataSource dataSource) throws SQLException {
        if (dataSource instanceof BasicDataSource) {
            ((BasicDataSource) dataSource).close();
        }
    }

    @Override
    public boolean exists(String tenantCode) {
        return this.getCodes().contains(tenantCode);
    }

    @Override
    public List<String> getCodes() {
        return this.getCacheWrapper().getCodes();
    }

    @Override
    public DataSource getDatasource(String tenantCode) {
        DataSource dataSource = this.getDataSources().get(tenantCode);
        if (null == dataSource) {
            TenantConfig config = this.getConfig(tenantCode);
            if (null == config) {
                logger.warn("No tenant for code '{}'", tenantCode);
                return null;
            }
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setDriverClassName(config.getDbDriverClassName());
            basicDataSource.setUsername(config.getDbUsername());
            basicDataSource.setPassword(config.getDbPassword());
            basicDataSource.setUrl(config.getDbUrl());
            basicDataSource.setMaxTotal(100);
            basicDataSource.setMaxIdle(30);
            basicDataSource.setMaxWaitMillis(20000);
            // maxTotal="100" maxIdle="30" maxWaitMillis="20000"
            basicDataSource.setInitialSize(5);
            dataSource = basicDataSource;
            this.getDataSources().put(tenantCode, dataSource);
        }
        return dataSource;
    }

    @Override
    public TenantConfig getConfig(String tenantCode) {
        return this.getCacheWrapper().getConfig(tenantCode);
    }
    
    @Override
    public String getCodeByDomainPrefix(String domainPrefix) {
        return this.getCacheWrapper().getCodeByDomainPrefix(domainPrefix);
    }

    @Override
    public TenantConfig getConfigByDomainPrefix(String domainPrefix) {
        return this.getConfig(this.getCodeByDomainPrefix(domainPrefix));
    }
    
    protected Map<String, DataSource> getDataSources() {
        return dataSources;
    }

    protected ITenantManagerCacheWrapper getCacheWrapper() {
        return cacheWrapper;
    }
    public void setCacheWrapper(ITenantManagerCacheWrapper cacheWrapper) {
        this.cacheWrapper = cacheWrapper;
    }
    
}

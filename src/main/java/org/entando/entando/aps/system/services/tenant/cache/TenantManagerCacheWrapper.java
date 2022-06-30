/*
 * Copyright 2022-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.tenant.cache;

import com.agiletec.aps.system.common.AbstractGenericCacheWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.tenant.TenantConfig;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;

/**
 * @author E.Santoboni
 */
public class TenantManagerCacheWrapper extends AbstractGenericCacheWrapper<Map<String, String>> implements ITenantManagerCacheWrapper {
    
    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(TenantManagerCacheWrapper.class);
    
    @Value("${ENTANDO_TENANTS:}")
    private String tenantsConfig;
    
    @Override
	public void initCache() throws EntException {
        try {
            if (!StringUtils.isBlank(this.tenantsConfig)) {
                Cache cache = this.getCache();
                ObjectMapper objectMapper = new ObjectMapper();
                CollectionType mapCollectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class);
                List<Map<String, String>> result = objectMapper.readValue(this.tenantsConfig, mapCollectionType);
                Map<String, Map<String, String>> tenantsMap = result.stream().collect(Collectors.toMap(tc -> tc.get(TenantConfig.TENANT_CODE_PROPERTY), tc -> tc));
                this.insertAndCleanCache(cache, tenantsMap);
            }
        } catch (Exception e) {
            logger.error("Error extracting tenant configs", e);
            throw new EntException("Error loading tenants", e);
        }
	}
    
    @Override
    public TenantConfig getTenantConfig(String code) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            CollectionType mapCollectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class);
            List<Map<String, Object>> result = objectMapper.readValue(this.tenantsConfig, mapCollectionType);
            for (int i = 0; i < result.size(); i++) {
                Map<String, Object> map = result.get(i);
                if (code.equalsIgnoreCase(map.get(TenantConfig.TENANT_CODE_PROPERTY).toString())) {
                    TenantConfig config = new TenantConfig();
                    config.putAll(map);
                    return config;
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Error extracting tenant config " + code, e);
            throw new EntRuntimeException("Error loading tenant " + code, e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<String> getCodes() {
        Cache cache = this.getCache();
        List<String> codes = (List<String>) this.get(cache, this.getCodesCacheKey(), List.class);
        if (null != codes) {
            return new ArrayList<>(codes);
        }
        return new ArrayList<>();
    }
    
    @Override
    protected Cache getCache() {
        return this.getSpringCacheManager().getCache(TENANT_MANAGER_CACHE_NAME);
    }

    @Override
    protected String getCodesCacheKey() {
        return TENANT_CODES_CACHE_NAME;
    }

    @Override
    protected String getCacheKeyPrefix() {
        return TENANT_CACHE_NAME_PREFIX;
    }

    @Override
    protected String getCacheName() {
        return TENANT_MANAGER_CACHE_NAME;
    }
    
}

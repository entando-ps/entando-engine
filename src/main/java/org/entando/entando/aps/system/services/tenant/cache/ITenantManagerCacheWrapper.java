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

import com.agiletec.aps.system.common.ICacheWrapper;
import java.util.List;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.aps.system.services.tenant.TenantConfig;

/**
 * @author E.Santoboni
 */
public interface ITenantManagerCacheWrapper extends ICacheWrapper {
    
    public static final String TENANT_MANAGER_CACHE_NAME = "Entando_TenantManager";
	public static final String TENANT_PREFIX = "TenantManager_tenant_";
	public static final String TENANT_CODES = "TenantManager_tenantsCodes";
	public static final String TENANT_EXT_MAPPING = "TenantManager_externalMapping";
    
    public void initCache() throws EntException;
    
    public TenantConfig getConfig(String code);
    
    public String getCodeByDomainPrefix(String domainPrefix);
    
    public List<String> getCodes();
    
}

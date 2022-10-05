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

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author E.Santoboni
 */
public class TenantConfig extends HashMap<String, Object> implements Serializable {
    
    public static final String TENANT_CODE_PROPERTY = "tenantCode";
    
    public static final String DOMAIN_PREFIX_PROPERTY = "domainPrefix";
    
    public static final String KC_ENABLED_PROPERTY = "kcEnabled";
    public static final String KC_AUTH_URL_PROPERTY = "kcAuthUrl";
    public static final String KC_REALM_PROPERTY = "kcRealm";
    public static final String KC_CLIENT_ID_PROPERTY = "kcClientId";
    public static final String KC_CLIENT_SECRET_PROPERTY = "kcClientSecret";
    public static final String KC_PUBLIC_CLIENT_PROPERTY = "kcPublicClientId";
    public static final String KC_SECURE_URIS_PROPERTY = "kcSecureUris";
    public static final String KC_DEFAULT_AUTH_PROPERTY = "kcDefaultAuthorizations";
    
    public static final String DB_DRIVER_CLASS_NAME_PROPERTY = "dbDriverClassName";
    public static final String DB_URL_PROPERTY = "dbUrl";
    public static final String DB_USERNAME_PROPERTY = "dbUsername";
    public static final String DB_PASSWORD_PROPERTY = "dbPassword";
    
    public static final String DB_MAX_TOTAL_PROPERTY = "dbMaxTotal";
    public static final String DB_MAX_IDLE_PROPERTY = "dbMaxIdle";
    public static final String DB_MAX_WAIT_MS_PROPERTY = "dbMaxWaitMillis";
    public static final String DB_INITIAL_SIZE_PROPERTY = "dbInitialSize";
    
    @Override
    public TenantConfig clone() {
        TenantConfig clone = new TenantConfig();
        clone.putAll(this);
        return clone;
    }

	public String getTenantCode() {
		return this.getProperty(TENANT_CODE_PROPERTY);
	}

    public String getDomainPrefix() {
		return this.getProperty(DOMAIN_PREFIX_PROPERTY);
    }

    public boolean isKcEnabled() {
        String enabled = this.getProperty(KC_ENABLED_PROPERTY);
        if (null != enabled) {
            return Boolean.valueOf(enabled);
        }
        return false;
    }

    public String getKcAuthUrl() {
		return this.getProperty(KC_AUTH_URL_PROPERTY);
    }

    public String getKcRealm() {
        return this.getProperty(KC_REALM_PROPERTY);
    }

    public String getKcClientId() {
        return this.getProperty(KC_CLIENT_ID_PROPERTY);
    }

    public String getKcClientSecret() {
        return this.getProperty(KC_CLIENT_SECRET_PROPERTY);
    }

    public String getKcPublicClientId() {
        return this.getProperty(KC_PUBLIC_CLIENT_PROPERTY);
    }

    public String getKcSecureUris() {
        return this.getProperty(KC_SECURE_URIS_PROPERTY);
    }

    public String getKcDefaultAuthorizations() {
        return this.getProperty(KC_DEFAULT_AUTH_PROPERTY);
    }

    public String getDbDriverClassName() {
        return this.getProperty(DB_DRIVER_CLASS_NAME_PROPERTY);
    }

    public String getDbUrl() {
        return this.getProperty(DB_URL_PROPERTY);
    }

    public String getDbUsername() {
        return this.getProperty(DB_USERNAME_PROPERTY);
    }

    public String getDbPassword() {
        return this.getProperty(DB_PASSWORD_PROPERTY);
    }

    public String getProperty(String name) {
        Object property = this.get(name);
        if (null != property) {
            return property.toString();
        }
        return null;
    }
    
}

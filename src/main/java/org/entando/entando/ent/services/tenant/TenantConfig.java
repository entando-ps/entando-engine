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
package org.entando.entando.ent.services.tenant;

import java.io.Serializable;

/**
 * @author E.Santoboni
 */
public class TenantConfig implements Serializable {
    
    private String tenantCode;
    private boolean kcEnabled;
    private String kcAuthUrl;
    private String kcRealm;
    private String kcClientId;
    private String kcClientSecret;
    private String kcPublicClientId;
    private String kcSecureUris;
    private String kcDefaultAuthorizations;
    
	private String dbDriverClassName;
	private String dbUrl;
	private String dbUsername;
	private String dbPassword;
    
    @Override
    public TenantConfig clone() {
        TenantConfig clone = new TenantConfig();
        clone.setDbDriverClassName(this.getDbDriverClassName());
        clone.setDbPassword(this.getDbPassword());
        clone.setDbUrl(this.getDbUrl());
        clone.setDbUsername(this.getDbUsername());
        clone.setKcAuthUrl(this.getKcAuthUrl());
        clone.setKcClientId(this.getKcClientId());
        clone.setKcClientSecret(this.getKcClientSecret());
        clone.setKcDefaultAuthorizations(this.getKcDefaultAuthorizations());
        clone.setKcEnabled(this.isKcEnabled());
        clone.setKcPublicClientId(this.getKcPublicClientId());
        clone.setKcRealm(this.getKcRealm());
        clone.setKcSecureUris(this.getKcSecureUris());
        clone.setTenantCode(this.getTenantCode());
        return clone;
    }
    
	public String getTenantCode() {
		return tenantCode;
	}
	public void setTenantCode(String tenantCode) {
		this.tenantCode = tenantCode;
	}

    public boolean isKcEnabled() {
        return kcEnabled;
    }
    public void setKcEnabled(boolean kcEnabled) {
        this.kcEnabled = kcEnabled;
    }

    public String getKcAuthUrl() {
        return kcAuthUrl;
    }
    public void setKcAuthUrl(String kcAuthUrl) {
        this.kcAuthUrl = kcAuthUrl;
    }

    public String getKcRealm() {
        return kcRealm;
    }
    public void setKcRealm(String kcRealm) {
        this.kcRealm = kcRealm;
    }

    public String getKcClientId() {
        return kcClientId;
    }
    public void setKcClientId(String kcClientId) {
        this.kcClientId = kcClientId;
    }

    public String getKcClientSecret() {
        return kcClientSecret;
    }
    public void setKcClientSecret(String kcClientSecret) {
        this.kcClientSecret = kcClientSecret;
    }

    public String getKcPublicClientId() {
        return kcPublicClientId;
    }
    public void setKcPublicClientId(String kcPublicClientId) {
        this.kcPublicClientId = kcPublicClientId;
    }

    public String getKcSecureUris() {
        return kcSecureUris;
    }
    public void setKcSecureUris(String kcSecureUris) {
        this.kcSecureUris = kcSecureUris;
    }

    public String getKcDefaultAuthorizations() {
        return kcDefaultAuthorizations;
    }
    public void setKcDefaultAuthorizations(String kcDefaultAuthorizations) {
        this.kcDefaultAuthorizations = kcDefaultAuthorizations;
    }

    public String getDbDriverClassName() {
        return dbDriverClassName;
    }
    public void setDbDriverClassName(String dbDriverClassName) {
        this.dbDriverClassName = dbDriverClassName;
    }

    public String getDbUrl() {
        return dbUrl;
    }
    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }
    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }
    
}

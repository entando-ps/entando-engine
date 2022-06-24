/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import com.agiletec.aps.BaseTestCase;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
public class TenantManagerIntegrationTest extends BaseTestCase {
    
    private ITenantManager tenantManager;

    @BeforeEach
    private void init() {
        this.tenantManager = getApplicationContext().getBean(ITenantManager.class);
    }

    @Test
    void testGetTenantCodes() {
        List<String> codes = this.tenantManager.getCodes();
        Assertions.assertEquals(2, codes.size());
        Assertions.assertTrue(codes.contains("tenant1") && codes.contains("tenant2"));
    }

    @Test
    void testExist() throws Throwable {
        Assertions.assertTrue(this.tenantManager.exists("tenant1"));
        Assertions.assertFalse(this.tenantManager.exists("tenantX"));
    }

    @Test
    void testGetDataSource() {
        Assertions.assertNotNull(this.tenantManager.getDatasource("tenant1"));
        Assertions.assertNull(this.tenantManager.getDatasource("tenantX"));
    }
    
    @Test
    void testGetConfig() {
        TenantConfig config = this.tenantManager.getConfig("tenant1");
        Assertions.assertNotNull(config);
        Assertions.assertEquals("tenant1", config.getTenantCode());
        Assertions.assertTrue(config.isKcEnabled());
        Assertions.assertEquals("http://tenant1.test.nip.io/auth", config.getKcAuthUrl());
        Assertions.assertEquals("tenant1", config.getKcRealm());
        Assertions.assertEquals("quickstart", config.getKcClientId());
        Assertions.assertEquals("secret1", config.getKcClientSecret());
        Assertions.assertEquals("entando-web", config.getKcPublicClientId());
        Assertions.assertEquals("", config.getKcSecureUris());
        Assertions.assertEquals("", config.getKcDefaultAuthorizations());
        Assertions.assertEquals("org.postgresql.Driver", config.getDbDriverClassName());
        Assertions.assertEquals("jdbc:postgresql://testDbServer:5432/tenantDb1", config.getDbUrl());
        Assertions.assertEquals("db_user_2", config.getDbUsername());
        Assertions.assertEquals("db_password_2", config.getDbPassword());
        Assertions.assertEquals("db_password_2", config.getProperty(TenantConfig.DB_PASSWORD_PROPERTY));
        Assertions.assertEquals("custom value", config.getProperty("customParam"));
        Assertions.assertNull(this.tenantManager.getConfig("tenantX"));
    }
    
}

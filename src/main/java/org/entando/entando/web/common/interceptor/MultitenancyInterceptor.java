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
package org.entando.entando.web.common.interceptor;

import com.agiletec.aps.system.EntThreadLocal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.entando.entando.ent.services.tenant.ITenantManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @author E.Santoboni
 */
public class MultitenancyInterceptor extends HandlerInterceptorAdapter {
    
    private ITenantManager tenantManager;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantCode = request.getServerName().split("\\.")[0];
        if (this.getTenantManager().exists(tenantCode)) {
            EntThreadLocal.set(ITenantManager.THREAD_LOCAL_TENANT_CODE, tenantCode);
        } else {
            EntThreadLocal.remove(ITenantManager.THREAD_LOCAL_TENANT_CODE);
        }
        return true;
    }
    
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler, Exception ex) {
        EntThreadLocal.remove(ITenantManager.THREAD_LOCAL_TENANT_CODE);
    }
    
    protected ITenantManager getTenantManager() {
        return tenantManager;
    }
    @Autowired
    public void setTenantManager(ITenantManager tenantManager) {
        this.tenantManager = tenantManager;
    }
    
}

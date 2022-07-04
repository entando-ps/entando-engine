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
package com.agiletec.aps.system;

import java.util.Map;

/**
 * @author E.Santoboni
 */
public abstract class EntThread extends Thread {
    
    private Map<String, Object> threadLocalMap;
    
    protected EntThread() {
        this.initLocalMap();
    }
    
    /**
     * Init the local map with the parameters given by the TreadLocal.
     * Method to invoke into the thread Constructor
     */
    protected void initLocalMap() {
        this.threadLocalMap = EntThreadLocal.getMap();
    }
    
    /**
     * Apply the map into the ThreadLocal.
     * Method to call into run method, before every other function
     */
    protected void applyLocalMap() {
        if (null != this.threadLocalMap) {
            this.threadLocalMap.entrySet().stream().forEach(e -> EntThreadLocal.set(e.getKey(), e.getValue()));
        }
    }
    
}

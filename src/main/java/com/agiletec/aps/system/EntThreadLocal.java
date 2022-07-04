/*
 * Copyright 2020-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

public class EntThreadLocal {
    
    private static final ThreadLocal<Map<String, Object>> threadLocalMap = new ThreadLocal<Map<String, Object>>();
    
    private EntThreadLocal() {
        throw new IllegalStateException("EntThreadLocal is an Utility class");
    }
    
    public static void init() {
		Map<String, Object> map = threadLocalMap.get();
		if (null != map) {
			map.clear();
		} else {
			threadLocalMap.set(new HashMap<String, Object>());
			map = threadLocalMap.get();
		}
	}

	public static void destroy() {
		Map<String, Object> map = threadLocalMap.get();
		if (null != map) {
			map.clear();
		}
	}
    
	public static void set(String key, Object value) {
		Map<String, Object> map = threadLocalMap.get();
		if (null == map) {
			threadLocalMap.set(new HashMap<>());
			map = threadLocalMap.get();
		}
		map.put(key, value);
	}

	public static Object get(String key) {
		Map<String, Object> map = threadLocalMap.get();
		if (null != map) {
			return map.get(key);
		}
		return null;
	}
    
    public static void remove(String key) {
		Map<String, Object> map = threadLocalMap.get();
		if (null != map) {
            map.remove(key);
		}
	}

	public static Map<String, Object> getMap() {
		Map<String, Object> map = threadLocalMap.get();
		if (null != map) {
			return new HashMap<>(map);
		}
		return null;
	}
    
}

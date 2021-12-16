/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

/**
 * @author E.Santoboni
 */
public class EntThreadLocal {
    
    private static final ThreadLocal<Map<String, Object>> sessionThreadLocal = new ThreadLocal<Map<String, Object>>();
    
    public static void init() {
		Map<String, Object> map = sessionThreadLocal.get();
		if (null != map) {
			map.clear();
		} else {
			sessionThreadLocal.set(new HashMap<String, Object>());
			map = sessionThreadLocal.get();
		}
	}

	public static void destroy() {
		Map<String, Object> map = sessionThreadLocal.get();
		if (null != map) {
			map.clear();
		}
	}
    
	public static void set(String key, Object value) {
		Map<String, Object> map = sessionThreadLocal.get();
		if (null == map) {
			sessionThreadLocal.set(new HashMap<>());
			map = sessionThreadLocal.get();
		}
		map.put(key, value);
	}

	public static Object get(String key) {
		Map<String, Object> map = sessionThreadLocal.get();
		if (null != map) {
			return map.get(key);
		}
		return null;
	}
    
    public static void remove(String key) {
		Map<String, Object> map = sessionThreadLocal.get();
		if (null != map) {
            map.remove(key);
		}
	}
    
}

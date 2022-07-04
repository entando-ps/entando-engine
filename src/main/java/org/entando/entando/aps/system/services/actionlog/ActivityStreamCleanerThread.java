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
package org.entando.entando.aps.system.services.actionlog;

import com.agiletec.aps.system.EntThread;
import java.util.Iterator;
import java.util.Set;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

/**
 * @author E.Santoboni
 */
public class ActivityStreamCleanerThread extends EntThread {
	
	private static final EntLogger logger = EntLogFactory.getSanitizedLogger(ActivityStreamCleanerThread.class);
	
	private Integer maxActivitySizeByGroup;
	private IActionLogManager actionLogManager;
	
	public ActivityStreamCleanerThread(Integer maxActivitySizeByGroup, IActionLogManager actionLogManager) {
        super();
		this.maxActivitySizeByGroup = maxActivitySizeByGroup;
		this.actionLogManager = actionLogManager;
	}
	
	@Override
	public void run() {
        super.applyLocalMap();
		try {
			Set<Integer> ids = this.actionLogManager.extractOldRecords(this.maxActivitySizeByGroup);
			if (null != ids) {
				Iterator<Integer> iter = ids.iterator();
				while (iter.hasNext()) {
					Integer id = iter.next();
					this.actionLogManager.deleteActionRecord(id);
				}
			}
		} catch (Throwable t) {
			logger.error("Error in run ", t);
		}
	}
	
}

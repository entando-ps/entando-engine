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
import org.entando.entando.aps.system.services.actionlog.model.ActionLogRecord;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

/**
 * @author E.Santoboni
 */
public class ActionLogAppenderThread extends EntThread {

	private static final EntLogger logger = EntLogFactory.getSanitizedLogger(ActionLogAppenderThread.class);
	
	private ActionLogRecord actionRecordToAdd;
	private ActionLogManager actionLogManager;
	
	public ActionLogAppenderThread(ActionLogRecord actionRecordToAdd, ActionLogManager actionLogManager) {
        super();
		this.actionLogManager = actionLogManager;
		this.actionRecordToAdd = actionRecordToAdd;
	}
	
	@Override
	public void run() {
        super.applyLocalMap();
		try {
			this.actionLogManager.addActionRecordByThread(this.actionRecordToAdd);
		} catch (Throwable t) {
			logger.error("error in run", t);
		}
	}
	
}
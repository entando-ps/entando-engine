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
package org.entando.entando.aps.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import org.entando.entando.aps.system.services.storage.IStorageManager;

/**
 * Return the URl of the resources.
 * There two attribute that can be used (but not mandatory):<br/>
 * - "root" if not otherwise specified, the value of SystemConstants.PAR_RESOURCES_ROOT_URL is used.<br/>
 * - "folder" declares a specific directory for the desired resources. Unless specified, the value "" 
 * (empty string) is used in the generation of the URL.
 * @author E.Santoboni
 */
@SuppressWarnings("serial")
public class ResourceURLTag extends TagSupport {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(ResourceURLTag.class);
	
    private boolean ignoreTenant;
	private String root;
	private String folder;
	
    @Override
	public int doEndTag() throws JspException {
		try {
			if (null == this.root) {
                if (this.isIgnoreTenant()) {
                    ConfigInterface configService = ApsWebApplicationUtils.getBean(SystemConstants.BASE_CONFIG_MANAGER, ConfigInterface.class, this.pageContext);
                    this.root = configService.getParam(SystemConstants.PAR_RESOURCES_ROOT_URL);
                } else {
                    IStorageManager storageManager = ApsWebApplicationUtils.getBean(SystemConstants.STORAGE_MANAGER, IStorageManager.class, this.pageContext);
                    this.root = storageManager.getResourceUrl("", false);
                }
                if (!this.root.endsWith("/")) {
                    this.root += "/";
                }
			}
			if (null == this.folder) {
				this.folder = "";
			}
			pageContext.getOut().print(this.getRoot() + this.getFolder());
		} catch (Exception e) {
			_logger.error("Error closing the tag", e);
			throw new JspException("Error closing the tag", e);
		}
		return EVAL_PAGE;
	}

    public boolean isIgnoreTenant() {
        return ignoreTenant;
    }
    public void setIgnoreTenant(boolean ignoreTenant) {
        this.ignoreTenant = ignoreTenant;
    }
    
	public String getRoot() {
		return root;
	}
	public void setRoot(String root) {
		this.root = root;
	}
    
	public String getFolder() {
		return folder;
	}
	public void setFolder(String folder) {
		this.folder = folder;
	}

}

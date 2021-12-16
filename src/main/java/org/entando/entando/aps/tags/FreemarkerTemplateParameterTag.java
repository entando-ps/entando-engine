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
package org.entando.entando.aps.tags;

import static javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

/**
 * Add a parameter into the Freemarker's TemplateModel Map
 * @author E.Santoboni
 * @deprecated remove it from every freemarker template (page template and fragments) 
 * and substitute @wp.fragments with #include directive
 */
public class FreemarkerTemplateParameterTag extends TagSupport {
	
	private static final EntLogger _logger =  EntLogFactory.getSanitizedLogger(FreemarkerTemplateParameterTag.class);
	
	@Override
    public int doStartTag() throws JspException {
        _logger.warn("** TAG FreemarkerTemplateParameterTag DEPRECATED ** - "
                + "remove it from every freemarker template (page template and fragments) and substitute @wp.fragments with #include directive");
        return EVAL_BODY_INCLUDE;
    }
    
	public void setVar(String var) {}
	public void setValueName(String valueName) {}
	public void setRemoveOnEndTag(boolean removeOnEndTag) {}
	
}

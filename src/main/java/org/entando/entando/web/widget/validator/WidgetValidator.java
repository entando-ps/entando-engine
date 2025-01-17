/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.web.widget.validator;

import com.agiletec.aps.util.ApsProperties;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.exception.ResourceNotFoundException;
import org.entando.entando.aps.system.services.widgettype.WidgetType;
import org.entando.entando.aps.system.services.widgettype.WidgetTypeManager;
import org.entando.entando.web.common.validator.AbstractPaginationValidator;
import org.entando.entando.web.widget.model.WidgetRequest;
import org.entando.entando.web.widget.model.WidgetUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class WidgetValidator extends AbstractPaginationValidator {

    public static final String ERRCODE_WIDGET_NOT_FOUND = "1";

    public static final String ERRCODE_WIDGET_ALREADY_EXISTS = "1";

    public static final String ERRCODE_WIDGET_GROUP_INVALID = "2";

    public static final String ERRCODE_WIDGET_DOES_NOT_EXISTS = "1";
    public static final String ERRCODE_MISSING_TITLE = "4";
    
    public static final String ERRCODE_OPERATION_FORBIDDEN_LOCKED = "1";
    public static final String ERRCODE_CANNOT_DELETE_USED_PAGES = "2";

    public static final String ERRCODE_NOT_BLANK = "52";

    @Autowired
    private WidgetTypeManager widgetTypeManager;

    @Override
    public boolean supports(Class<?> paramClass) {
        return WidgetRequest.class.equals(paramClass);
    }

    @Override
    public void validate(Object target, Errors errors) {
        WidgetRequest widgetRequest = (WidgetRequest) target;
        if (StringUtils.isEmpty(widgetRequest.getCustomUi()) && StringUtils.isEmpty(widgetRequest.getParentType())) {
            errors.rejectValue("customUi", ERRCODE_NOT_BLANK, new String[]{}, "widgettype.customUi.notBlank");
        }
        this.validateTitles(widgetRequest.getTitles(), errors);
    }
    
    public void validateEditWidget(String widgetCode, WidgetUpdateRequest widgetRequest, Errors errors) {
        WidgetType type = widgetTypeManager.getWidgetType(widgetCode);

        if (type == null) {
            throw new ResourceNotFoundException(WidgetValidator.ERRCODE_WIDGET_DOES_NOT_EXISTS, "widget", widgetCode);
        }

        ApsProperties newWidgetConfig = ApsProperties.fromMap(widgetRequest.getConfig());
        ApsProperties widgetTypeConfig = type.getConfig();

        if (newWidgetConfig.size( ) == 0) {
            newWidgetConfig = null;
        }

        if ((widgetTypeConfig==null && newWidgetConfig!=null && type.isLocked()) ||
           (widgetTypeConfig!=null && newWidgetConfig==null && type.isLocked()) ||
           (widgetTypeConfig!=null && newWidgetConfig!=null  && !widgetTypeConfig.equals(newWidgetConfig) && type.isLocked()))
        {
            errors.rejectValue("code", ERRCODE_OPERATION_FORBIDDEN_LOCKED, new String[]{widgetCode}, "widgettype.edit.locked");
        }

        this.validateTitles(widgetRequest.getTitles(), errors);
    }

    protected void validateTitles(Map<String, String> titles, Errors errors) {
        if (null == titles) {
            errors.rejectValue("titles", ERRCODE_NOT_BLANK, "widgettype.titles.notBlank");
        } else {
            String[] langs = {"en", "it"};
            for (String lang : langs) {
                if (StringUtils.isBlank(titles.get(lang))) {
                    errors.rejectValue("titles", ERRCODE_MISSING_TITLE, new String[]{lang}, "widgettype.title.notBlank");
                }
            }
        }
    }
}

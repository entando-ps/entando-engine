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
package org.entando.entando.web.widget;

import com.agiletec.aps.system.services.role.Permission;
import org.entando.entando.aps.system.services.widgettype.IWidgetService;
import org.entando.entando.aps.system.services.widgettype.model.WidgetDto;
import org.entando.entando.aps.system.services.widgettype.model.WidgetInfoDto;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.exceptions.ValidationGenericException;
import org.entando.entando.web.common.model.*;
import org.entando.entando.web.component.ComponentUsage;
import org.entando.entando.web.component.ComponentUsageEntity;
import org.entando.entando.web.page.model.PageSearchRequest;
import org.entando.entando.web.widget.model.WidgetRequest;
import org.entando.entando.web.widget.model.WidgetUpdateRequest;
import org.entando.entando.web.widget.validator.WidgetValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WidgetController {
    public static final String COMPONENT_ID = "widget";

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

    @Autowired
    IWidgetService widgetService;

    @Autowired
    private WidgetValidator widgetValidator;

    @RestAccessControl(permission = Permission.MANAGE_PAGES)
    @GetMapping(value = "/widgets/{widgetCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<WidgetDto>> getWidget(@PathVariable String widgetCode) {
        logger.trace("getWidget by code {}", widgetCode);
        WidgetDto widgetDto = this.widgetService.getWidget(widgetCode);
        return new ResponseEntity<>(new SimpleRestResponse<>(widgetDto), HttpStatus.OK);
    }

    @RestAccessControl(permission = Permission.SUPERUSER)
    @GetMapping(value = "/widgets/{widgetCode}/usage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<ComponentUsage>> getComponentUsage(@PathVariable String widgetCode) {
        logger.trace("get {} usage by code {}", COMPONENT_ID, widgetCode);

        ComponentUsage usage = ComponentUsage.builder()
                .type(COMPONENT_ID)
                .code(widgetCode)
                .usage(widgetService.getComponentUsage(widgetCode))
                .build();

        return new ResponseEntity<>(new SimpleRestResponse<>(usage), HttpStatus.OK);
    }

    @RestAccessControl(permission = Permission.SUPERUSER)
    @GetMapping(value = "/widgets/{widgetCode}/usage/details", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedRestResponse<ComponentUsageEntity>> getComponentUsageDetails(@PathVariable String widgetCode, PageSearchRequest searchRequest) {

        logger.trace("get {} usage details by code {}", COMPONENT_ID, widgetCode);

        // clear filters
        searchRequest.setFilters(new Filter[0]);

        PagedMetadata<ComponentUsageEntity> result = widgetService.getComponentUsageDetails(widgetCode, searchRequest);

        return new ResponseEntity<>(new PagedRestResponse<>(result), HttpStatus.OK);
    }


    @RestAccessControl(permission = Permission.SUPERUSER)
    @DeleteMapping(value = "/widgets/{widgetCode}", produces = MediaType.APPLICATION_JSON_VALUE, name = "widget")
    public ResponseEntity<SimpleRestResponse<Map<String, String>>> deleteWidget(@PathVariable String widgetCode) {
        logger.info("deleting widget {}", widgetCode);
        this.widgetService.removeWidget(widgetCode);
        Map<String, String> result = new HashMap<>();
        result.put("code", widgetCode);
        return new ResponseEntity<>(new SimpleRestResponse<>(result), HttpStatus.OK);
    }

    @RestAccessControl(permission = Permission.SUPERUSER)
    @PutMapping(value = "/widgets/{widgetCode}", produces = MediaType.APPLICATION_JSON_VALUE, name = "widget")
    public ResponseEntity<SimpleRestResponse<WidgetDto>> updateWidget(@PathVariable String widgetCode,
                                                                      @Valid @RequestBody WidgetUpdateRequest widgetRequest,
                                                                      BindingResult bindingResult) {
        logger.trace("update widget. Code: {} and body {}: ", widgetCode, widgetRequest);
        //field validations
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        this.widgetValidator.validateEditWidget(widgetCode, widgetRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        WidgetDto widgetDto = this.widgetService.updateWidget(widgetCode, widgetRequest);
        return new ResponseEntity<>(new SimpleRestResponse<>(widgetDto), HttpStatus.OK);
    }

    @RestAccessControl(permission = Permission.MANAGE_PAGES)
    @PostMapping(value = "/widgets", produces = MediaType.APPLICATION_JSON_VALUE, name = "widget")
    public ResponseEntity<SimpleRestResponse<WidgetDto>> addWidget(
            @Valid @RequestBody WidgetRequest widgetRequest,
            BindingResult bindingResult) {
        logger.trace("add widget. body {}: ", widgetRequest);
        //field validations
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        //business validations
        this.widgetValidator.validate(widgetRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        WidgetDto widgetDto = this.widgetService.addWidget(widgetRequest);
        return new ResponseEntity<>(new SimpleRestResponse<>(widgetDto), HttpStatus.OK);
    }

    @RestAccessControl(permission = Permission.MANAGE_PAGES)
    @GetMapping(value = "/widgets", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedRestResponse<WidgetDto>> getWidgets(RestListRequest requestList) {
        logger.trace("get widget list {}", requestList);
        this.getWidgetValidator().validateRestListRequest(requestList, WidgetDto.class);
        PagedMetadata<WidgetDto> result = this.widgetService.getWidgets(requestList);
        this.getWidgetValidator().validateRestListResult(requestList, result);
        return new ResponseEntity<>(new PagedRestResponse<>(result), HttpStatus.OK);
    }

    @RestAccessControl(permission = Permission.SUPERUSER)
    @GetMapping(value = "/widgets/{widgetCode}/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<WidgetInfoDto>> getWidgetInfo(@PathVariable String widgetCode) {
        logger.trace("getWidgetInfo by code {}", widgetCode);
        WidgetInfoDto info = this.widgetService.getWidgetInfo(widgetCode);
        return new ResponseEntity<>(new SimpleRestResponse<>(info), HttpStatus.OK);
    }

    public IWidgetService getWidgetService() {
        return widgetService;
    }

    public void setWidgetService(IWidgetService widgetService) {
        this.widgetService = widgetService;
    }

    public WidgetValidator getWidgetValidator() {
        return widgetValidator;
    }

    public void setWidgetValidator(WidgetValidator widgetValidator) {
        this.widgetValidator = widgetValidator;
    }
}

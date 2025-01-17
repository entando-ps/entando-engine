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
package org.entando.entando.aps.system.services.page.model;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.util.ApsProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author paddeo
 */
public class PageDto {

    private static final String DEFAULT_CHARSET = "utf8";
    private static final String DEFAULT_CONTENT_TYPE= "text/html";
    private String code;
    private String status;
    private boolean onlineInstance;
    private boolean displayedInMenu;
    private String pageModel;
    private String charset;
    private String contentType;
    private String parentCode;
    private boolean seo;
    private Map<String, String> titles = new HashMap<>();
    private Map<String, String> fullTitles = new HashMap<>();
    private String ownerGroup;
    private List<String> joinGroups = new ArrayList<>();
    private List<String> children = new ArrayList<>();
    private int position;
    private int numWidget;
    private String lastModified;
    private String fullPath;
    private String token;

    /**
     * The references grouped by service name.
     * <p>
     * Lists all the managers that may contain references by indicating with
     * <code>true</code> the presence of references
     */
    @JsonInclude(Include.NON_NULL)
    private Map<String, Boolean> references;

    public PageDto() {
    }

    public PageDto(IPage page, IPageManager pageManager) {
        this.setCode(page.getCode());
        this.setStatus(getPageStatus(page));
        this.setOnlineInstance(page.isOnlineInstance());
        this.setDisplayedInMenu(page.isShowable());
        this.setPageModel(page.getModelCode());
        if (page.getCharset() != null) {
            this.setCharset(page.getCharset());
        } else {
            this.setCharset(DEFAULT_CHARSET);
        }
        if (page.getMimeType() != null) {
            this.setContentType(page.getMimeType());
        } else {
            this.setContentType(DEFAULT_CONTENT_TYPE);
        }
        this.setParentCode(page.getParentCode());
        this.setSeo(page.isUseExtraTitles());
        Optional<ApsProperties> apsTitles = Optional.ofNullable(page.getTitles());
        apsTitles.ifPresent(values -> values.keySet().forEach(lang
                -> {
            this.getTitles().put((String) lang, (String) values.get(lang));
            this.getFullTitles().put((String) lang, (String) page.getFullTitle((String) lang, pageManager));
        }
        ));
        this.setOwnerGroup(page.getGroup());
        Optional<Set<String>> groups = Optional.ofNullable(page.getExtraGroups());
        groups.ifPresent(values -> values.forEach((group) -> this.joinGroups.add(group)));
        Optional.ofNullable(page.getChildrenCodes()).
                ifPresent(values -> Arrays.asList(values).forEach((child) -> this.children.add(child)));
        this.setPosition(page.getPosition());
        Optional.ofNullable(page.getWidgets()).ifPresent(widgets -> this.setNumWidget(Arrays.asList(widgets).stream()
                .filter(widget -> widget != null)
                .collect(Collectors.toList())
                .size()));
        if (null != page.getMetadata().getUpdatedAt()) {
            SimpleDateFormat sdf = new SimpleDateFormat(SystemConstants.API_DATE_FORMAT);
            this.setLastModified(sdf.format(page.getMetadata().getUpdatedAt()));
        }
        this.setFullPath(page.getPath(pageManager));
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isOnlineInstance() {
        return onlineInstance;
    }

    public void setOnlineInstance(boolean onlineInstance) {
        this.onlineInstance = onlineInstance;
    }
    
    public boolean isDisplayedInMenu() {
        return displayedInMenu;
    }

    public void setDisplayedInMenu(boolean displayedInMenu) {
        this.displayedInMenu = displayedInMenu;
    }

    public String getPageModel() {
        return pageModel;
    }

    public void setPageModel(String pageModel) {
        this.pageModel = pageModel;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public boolean isSeo() {
        return seo;
    }

    public void setSeo(boolean seo) {
        this.seo = seo;
    }

    public Map<String, String> getTitles() {
        return titles;
    }

    public void setTitles(Map<String, String> titles) {
        this.titles = titles;
    }

    public Map<String, String> getFullTitles() {
        return fullTitles;
    }

    public void setFullTitles(Map<String, String> fullTitles) {
        this.fullTitles = fullTitles;
    }

    public String getOwnerGroup() {
        return ownerGroup;
    }

    public void setOwnerGroup(String ownerGroup) {
        this.ownerGroup = ownerGroup;
    }

    public List<String> getJoinGroups() {
        return joinGroups;
    }

    public void setJoinGroups(List<String> joinGroups) {
        this.joinGroups = joinGroups;
    }

    public void addJoinGroup(String joinGroup) {
        this.joinGroups.add(joinGroup);
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    public void addChild(String child) {
        this.children.add(child);
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getNumWidget() {
        return numWidget;
    }

    public void setNumWidget(int numWidget) {
        this.numWidget = numWidget;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public static String getEntityFieldName(String dtoFieldName) {
        switch (dtoFieldName) {
            case "code":
                return "code";
            case "name":
                return "descr";
            default:
                return dtoFieldName;
        }
    }

    public Map<String, Boolean> getReferences() {
        return references;
    }

    public void setReferences(Map<String, Boolean> references) {
        this.references = references;
    }

    private String getPageStatus(IPage page) {
        if (page.isOnline()) {
            if (page.isChanged()) {
                return "draft";
            } else {
                return "published";
            }
        } else {
            return "unpublished";
        }
    }
}

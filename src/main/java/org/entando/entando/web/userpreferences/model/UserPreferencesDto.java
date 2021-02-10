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
package org.entando.entando.web.userpreferences.model;

import java.util.ArrayList;
import java.util.List;
import org.entando.entando.aps.system.services.userpreferences.UserPreferences;
import static org.entando.entando.aps.system.services.userpreferences.UserPreferencesService.DEFAULT_JOIN_GROUP_DELIMITER;

public class UserPreferencesDto {

    private Boolean wizard;
    private Boolean loadOnPageSelect;
    private Boolean translationWarning;
    private String defaultPageOwnerGroup;
    private List<String> defaultPageJoinGroups;
    private String defaultContentOwnerGroup;
    private List<String> defaultContentJoinGroups;

    public UserPreferencesDto(UserPreferences userPreferences) {
        wizard = userPreferences.isWizard();
        loadOnPageSelect = userPreferences.isLoadOnPageSelect();
        translationWarning = userPreferences.isTranslationWarning();
        defaultPageOwnerGroup = userPreferences.getDefaultPageOwnerGroup();
        String defaultJoinPageGroupsString = userPreferences.getDefaultPageJoinGroups();
        if (defaultJoinPageGroupsString != null) {
            for (String group : userPreferences.getDefaultPageJoinGroups().split(DEFAULT_JOIN_GROUP_DELIMITER)) {
                if (defaultPageJoinGroups == null) {
                    defaultPageJoinGroups = new ArrayList<>();
                }
                defaultPageJoinGroups.add(group);
            }
        }
        defaultContentOwnerGroup = userPreferences.getDefaultContentOwnerGroup();
        String defaultJoinContentGroupsString = userPreferences.getDefaultContentJoinGroups();
        if (defaultJoinContentGroupsString != null) {
            for (String group : userPreferences.getDefaultContentJoinGroups().split(DEFAULT_JOIN_GROUP_DELIMITER)) {
                if (defaultContentJoinGroups == null) {
                    defaultContentJoinGroups = new ArrayList<>();
                }
                defaultContentJoinGroups.add(group);
            }
        }
    }

    public Boolean getWizard() {
        return wizard;
    }

    public void setWizard(Boolean wizard) {
        this.wizard = wizard;
    }

    public Boolean getLoadOnPageSelect() {
        return loadOnPageSelect;
    }

    public void setLoadOnPageSelect(Boolean loadOnPageSelect) {
        this.loadOnPageSelect = loadOnPageSelect;
    }

    public Boolean getTranslationWarning() {
        return translationWarning;
    }

    public void setTranslationWarning(Boolean translationWarning) {
        this.translationWarning = translationWarning;
    }

    public String getDefaultPageOwnerGroup() {
        return defaultPageOwnerGroup;
    }

    public void setDefaultPageOwnerGroup(String defaultPageOwnerGroup) {
        this.defaultPageOwnerGroup = defaultPageOwnerGroup;
    }

    public List<String> getDefaultPageJoinGroups() {
        return defaultPageJoinGroups;
    }

    public void setDefaultPageJoinGroups(List<String> defaultPageJoinGroups) {
        this.defaultPageJoinGroups = defaultPageJoinGroups;
    }

    public String getDefaultContentOwnerGroup() {
        return defaultContentOwnerGroup;
    }

    public void setDefaultContentOwnerGroup(String defaultContentOwnerGroup) {
        this.defaultContentOwnerGroup = defaultContentOwnerGroup;
    }

    public List<String> getDefaultContentJoinGroups() {
        return defaultContentJoinGroups;
    }

    public void setDefaultContentJoinGroups(List<String> defaultContentJoinGroups) {
        this.defaultContentJoinGroups = defaultContentJoinGroups;
    }

    @Override
    public String toString() {
        return "UserPreferencesDto{" +
                "wizard=" + wizard +
                ", loadOnPageSelect=" + loadOnPageSelect +
                ", translationWarning=" + translationWarning +
                ", defaultPageOwnerGroup='" + defaultPageOwnerGroup + '\'' +
                ", defaultPageJoinGroups=" + defaultPageJoinGroups +
                ", defaultContentOwnerGroup='" + defaultContentOwnerGroup + '\'' +
                ", defaultContentJoinGroups=" + defaultContentJoinGroups +
                '}';
    }
}

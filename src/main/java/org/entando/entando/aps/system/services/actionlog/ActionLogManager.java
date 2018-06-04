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

import com.agiletec.aps.system.common.AbstractService;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.exception.ApsSystemException;
import com.agiletec.aps.system.services.authorization.Authorization;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.keygenerator.IKeyGeneratorManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.DateConverter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.entando.entando.aps.system.services.actionlog.model.ActionLogRecord;
import org.entando.entando.aps.system.services.actionlog.model.ActivityStreamSeachBean;
import org.entando.entando.aps.system.services.actionlog.model.IActionLogRecordSearchBean;
import org.entando.entando.aps.system.services.actionlog.model.IActivityStreamSearchBean;
import org.entando.entando.aps.system.services.actionlog.model.ManagerConfiguration;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

/**
 * @author E.Santoboni - S.Puddu
 */
public class ActionLogManager extends AbstractService implements IActionLogManager/*, ProfileChangedObserver*/ {

    private static final Logger _logger = LoggerFactory.getLogger(ActionLogManager.class);

    @Override
    public void init() throws Exception {
        _logger.debug("{} ready", this.getClass().getName());
    }

    @Override
    public void addActionRecord(ActionLogRecord actionRecord) throws ApsSystemException {
        try {
            ActionLogAppenderThread thread = new ActionLogAppenderThread(actionRecord, this);
            String threadName = LOG_APPENDER_THREAD_NAME_PREFIX + DateConverter.getFormattedDate(new Date(), "yyyyMMddHHmmss");
            thread.setName(threadName);
            thread.start();
        } catch (Throwable t) {
            _logger.error("Error adding an actionlogger record", t);
            throw new ApsSystemException("Error adding an actionlogger record", t);
        }
    }

    @Override
    public void updateRecordDate(int id) throws ApsSystemException {
        try {
            this.getActionLogDAO().updateRecordDate(id);
        } catch (Throwable t) {
            _logger.error("Error updating data record", t);
            throw new ApsSystemException("Error updating data record", t);
        }
    }

    protected synchronized void addActionRecordByThread(ActionLogRecord actionRecord) throws ApsSystemException {
        try {
            Integer key = null;
            List<Integer> ids = null;
            do {
                key = this.getKeyGeneratorManager().getUniqueKeyCurrentValue();
                FieldSearchFilter filter = new FieldSearchFilter("id", key, true);
                FieldSearchFilter[] filters = {filter};
                ids = this.getActionLogDAO().getActionRecords(filters);
            } while (!ids.isEmpty());
            actionRecord.setId(key);
            actionRecord.setActionDate(new Date());
            this.getActionLogDAO().addActionRecord(actionRecord);
        } catch (Throwable t) {
            _logger.error("Error adding an actionlogger record", t);
            throw new ApsSystemException("Error adding an actionlogger record", t);
        }
    }

    @Override
    @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'ActionLogRecord_'.concat(#id)")
    public void deleteActionRecord(int id) throws ApsSystemException {
        try {
            this.getActionLogDAO().deleteActionRecord(id);
        } catch (Throwable t) {
            _logger.error("Error deleting the actionlogger record: {}", id, t);
            throw new ApsSystemException("Error deleting the actionlogger record: " + id, t);
        }
    }

    @Override
    public List<Integer> getActionRecords(IActionLogRecordSearchBean searchBean) throws ApsSystemException {
        List<Integer> records = new ArrayList<Integer>();
        try {
            records = this.getActionLogDAO().getActionRecords(searchBean);
        } catch (Throwable t) {
            _logger.error("Error loading actionlogger records", t);
            throw new ApsSystemException("Error loading actionlogger records", t);
        }
        return records;
    }

    @Override
    @Cacheable(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'ActionLogRecord_'.concat(#id)")
    public ActionLogRecord getActionRecord(int id) throws ApsSystemException {
        ActionLogRecord record = null;
        try {
            record = this.getActionLogDAO().getActionRecord(id);
        } catch (Throwable t) {
            _logger.error("Error loading actionlogger record with id: {}", id, t);
            throw new ApsSystemException("Error loading actionlogger record with id: " + id, t);
        }
        return record;
    }

    @Override
    public List<Integer> getActivityStream(List<String> userGroupCodes) throws ApsSystemException {
        List<Integer> recordIds = null;
        try {
            recordIds = this.getActionLogDAO().getActivityStream(userGroupCodes);
            ManagerConfiguration config = this.getManagerConfiguration();
            if (null != recordIds && null != config
                    && config.getCleanOldActivities() && config.getMaxActivitySizeByGroup() < recordIds.size()) {
                ActivityStreamCleanerThread thread = new ActivityStreamCleanerThread(config.getNumberOfStreamsOnHistory(), this);
                String threadName = LOG_CLEANER_THREAD_NAME_PREFIX + DateConverter.getFormattedDate(new Date(), "yyyyMMddHHmmss");
                thread.setName(threadName);
                thread.start();
            }
            if (null != config && null != recordIds && recordIds.size() > config.getMaxActivitySizeByGroup()) {
                recordIds = recordIds.subList(0, config.getMaxActivitySizeByGroup());
            }
        } catch (Throwable t) {
            _logger.error("Error loading activity stream records", t);
            throw new ApsSystemException("Error loading activity stream records", t);
        }
        return recordIds;
    }

    @Override
    public List<Integer> getActivityStream(IActivityStreamSearchBean activityStreamSearchBean) {
        List<Integer> recordIds = null;
        recordIds = this.getActionLogDAO().getActionRecords(activityStreamSearchBean);
        ManagerConfiguration config = this.getManagerConfiguration();
        if (null != config && null != recordIds && recordIds.size() > config.getMaxActivitySizeByGroup()) {
            recordIds = recordIds.subList(0, config.getMaxActivitySizeByGroup());
        }
        return recordIds;
    }

    @Override
    public List<Integer> getActivityStream(UserDetails loggedUser) throws ApsSystemException {
        List<String> userGroupCodes = this.extractUserGroupCodes(loggedUser);
        return this.getActivityStream(userGroupCodes);
    }

    @Override
    public Date lastUpdateDate(UserDetails loggedUser) throws ApsSystemException {
        List<Integer> actionRecordIds = null;
        Date lastUpdate = new Date();
        try {
            ActivityStreamSeachBean searchBean = new ActivityStreamSeachBean();
            searchBean.setUserGroupCodes(this.extractUserGroupCodes(loggedUser));
            searchBean.setEndUpdate(lastUpdate);
            actionRecordIds = this.getActionRecords(searchBean);
            if (null != actionRecordIds && actionRecordIds.size() > 0) {
                ActionLogRecord actionRecord = this.getActionRecord(actionRecordIds.get(0));
                lastUpdate = actionRecord.getUpdateDate();
            }
        } catch (Throwable t) {
            _logger.error("Error on loading updated activities", t);
            throw new ApsSystemException("Error on loading updated activities", t);
        }
        return lastUpdate;
    }

    private List<String> extractUserGroupCodes(UserDetails loggedUser) {
        List<String> codes = new ArrayList<String>();
        List<Authorization> auths = (null != loggedUser) ? loggedUser.getAuthorizations() : null;
        if (null != auths) {
            for (int i = 0; i < auths.size(); i++) {
                Authorization auth = auths.get(i);
                if (null != auth && null != auth.getGroup()) {
                    codes.add(auth.getGroup().getName());
                }
            }
        }
        if (!codes.contains(Group.FREE_GROUP_NAME)) {
            codes.add(Group.FREE_GROUP_NAME);
        }
        return codes;
    }

    @Override
    public Set<Integer> extractOldRecords(Integer maxActivitySizeByGroup) throws ApsSystemException {
        Set<Integer> actionRecordIds = null;
        try {
            actionRecordIds = this.getActionLogDAO().extractOldRecords(maxActivitySizeByGroup);
        } catch (Throwable t) {
            _logger.error("Error extracting old records", t);
            throw new ApsSystemException("Error extracting old records", t);
        }
        return actionRecordIds;
    }

    protected ManagerConfiguration getManagerConfiguration() {
        return _managerConfiguration;
    }

    public void setManagerConfiguration(ManagerConfiguration managerConfiguration) {
        this._managerConfiguration = managerConfiguration;
    }

    protected IActionLogDAO getActionLogDAO() {
        return _actionLogDAO;
    }

    public void setActionLogDAO(IActionLogDAO actionLogDAO) {
        this._actionLogDAO = actionLogDAO;
    }

    protected IKeyGeneratorManager getKeyGeneratorManager() {
        return _keyGeneratorManager;
    }

    public void setKeyGeneratorManager(IKeyGeneratorManager keyGeneratorManager) {
        this._keyGeneratorManager = keyGeneratorManager;
    }

    private ManagerConfiguration _managerConfiguration;

    private IActionLogDAO _actionLogDAO;
    private IKeyGeneratorManager _keyGeneratorManager;

}
